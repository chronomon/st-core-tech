package com.chronomon.analysis.trajectory.mapmatch;

import com.chronomon.analysis.trajectory.filter.TrajNoiseFilter;
import com.chronomon.analysis.trajectory.mapmatch.project.ProjectCluster;
import com.chronomon.analysis.trajectory.mapmatch.project.ProjectPoint;
import com.chronomon.analysis.trajectory.mapmatch.transfer.ClusterLinkNode;
import com.chronomon.analysis.trajectory.mapmatch.transfer.HmmProbability;
import com.chronomon.analysis.trajectory.model.CoordinateUtil;
import com.chronomon.analysis.trajectory.road.DirectionEnum;
import com.chronomon.analysis.trajectory.road.IRoadSegment;
import com.chronomon.analysis.trajectory.road.RoadNetwork;
import com.chronomon.analysis.trajectory.model.GpsPoint;
import com.chronomon.analysis.trajectory.model.Trajectory;
import com.chronomon.analysis.trajectory.road.RoadSegment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于隐马尔科夫模型的轨迹地图匹配器
 * 参考文献：“Map-matching for low-sampling-rate GPS trajectories”
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class HmmMapMatcher {

    /**
     * 路网对象
     */
    public final RoadNetwork rn;

    /**
     * GPS投影点的搜索半径（单位是米）
     */
    public final double searchDistInM;

    /**
     * 隐马尔科夫的概率计算模型
     */
    public final HmmProbability hmmProbability;

    public HmmMapMatcher(RoadNetwork rn, double searchDistInM) {
        this.rn = rn;
        this.searchDistInM = searchDistInM;
        this.hmmProbability = new HmmProbability(searchDistInM);
    }

    /**
     * 执行轨迹地图匹配算法，得到物体实现路线
     * 可能会由于GPS噪点和路段数据的缺失导致匹配中断
     * 所以返回的结果由多段路径组成
     *
     * @param trajectory 原始的轨迹
     * @return 匹配到路网上的路径
     */
    public List<MapMatchTrajectory> mapMatch(Trajectory trajectory) {
        // 执行隐马尔科夫模型的轨迹地图匹配算法
        ClusterLinkNode lastNode = projectTrajectory(trajectory);
        if (lastNode == null) {
            // 说明所有GPS点都没找到投影点
            return Collections.emptyList();
        }

        // 根据计算出来的概率值，明确最终预测到的行驶路径
        return buildMatchedTrajectory(trajectory.getOid(), lastNode);
    }

    /**
     * 根据隐马尔科夫模型计算出来的概率值，回溯最终预测到的行驶路径
     *
     * @param oid      对象ID
     * @param lastNode GPS链表的最后一个节点
     * @return 预测到的车辆行驶路径
     */
    public List<MapMatchTrajectory> buildMatchedTrajectory(String oid, ClusterLinkNode lastNode) {
        // 最优路径回溯，将路径上的投影点标记为必经投影点
        ClusterLinkNode currNode = markConfirmProjectPoint(lastNode);

        // 从前到后收集最优路径上的坐标点，形成地图匹配之后的轨迹
        List<MapMatchTrajectory> matchPathList = new ArrayList<>();
        List<GpsPoint> gpsPointList = new ArrayList<>();
        Timestamp startTime = currNode.projectCluster.gpsPoint.getTime();
        while (currNode.hasNext()) {
            ClusterLinkNode nextNode = currNode.next();
            if (nextNode.projectCluster.isStuck) {
                if (gpsPointList.size() > 1) {
                    Timestamp endTime = currNode.projectCluster.gpsPoint.getTime();
                    matchPathList.add(new MapMatchTrajectory(oid, startTime, endTime, gpsPointList));
                    startTime = nextNode.projectCluster.gpsPoint.getTime();
                }
                gpsPointList = new ArrayList<>();
            } else {
                int currConfirmedIndex = currNode.projectCluster.markedIndex;
                ProjectPoint currentProject = currNode.projectCluster.getProjectPoint(currConfirmedIndex);
                int nextConfirmedIndex = nextNode.projectCluster.markedIndex;
                ProjectPoint nextProjectPoint = nextNode.projectCluster.getProjectPoint(nextConfirmedIndex);
                assert currConfirmedIndex == nextProjectPoint.getPrevIndex();

                if (currentProject.onSameSegmentAndBefore(nextProjectPoint)) {
                    // 将当前GPS点的时间存储成坐标点的Z值
                    GpsPoint gpsPoint = new GpsPoint(oid, currentProject.point.getCoordinate());
                    gpsPoint.setTime(currNode.projectCluster.gpsPoint.getTime());
                    gpsPointList.add(gpsPoint);

                    // 最短路径上的坐标点没有时间值
                    IRoadSegment roadSegment = currentProject.roadSegment;
                    for (int i = currentProject.segmentIndex + 1; i <= nextProjectPoint.segmentIndex; i++) {
                        gpsPointList.add(new GpsPoint(oid, roadSegment.getCoordinateN(i)));
                    }

                    // 将下一个GPS点的时间存储成坐标点的Z值
                    gpsPoint = new GpsPoint(oid, nextProjectPoint.point.getCoordinate());
                    gpsPoint.setTime(nextNode.projectCluster.gpsPoint.getTime());
                    gpsPointList.add(gpsPoint);
                } else {
                    List<IRoadSegment> pathSegments = nextProjectPoint.getPathSegments();
                    if (pathSegments == null || pathSegments.isEmpty()) {
                        assert currentProject.roadSegment.getToNode().equals(nextProjectPoint.roadSegment.getFromNode());
                    } else {
                        assert currentProject.roadSegment.getToNode().geom.getCoordinate().equals(pathSegments.get(0).getFromNode().geom.getCoordinate());
                        for (int i = 0; i < pathSegments.size() - 1; i++) {
                            assert pathSegments.get(i).getToNode().geom.getCoordinate().equals(pathSegments.get(i + 1).getFromNode().geom.getCoordinate());
                        }
                    }
                    // 当前投影点到投影路段终点之间的坐标点，第一个坐标点是GPS的投影点，时间存储在Z中
                    List<GpsPoint> suffixCoordinateList = currentProject.getSuffixCoordinates()
                            .stream().map(coordinate -> new GpsPoint(oid, coordinate))
                            .collect(Collectors.toList());
                    suffixCoordinateList.get(0).setTime(currNode.projectCluster.gpsPoint.getTime());
                    gpsPointList.addAll(suffixCoordinateList);

                    // 当前投影路段终点到下一个投影点之间的坐标点，最后一个坐标点是GPS的投影点，时间存储在Z中
                    List<GpsPoint> prefixCoordinateList = nextProjectPoint.getPrefixCoordinates()
                            .stream().map(coordinate -> new GpsPoint(oid, coordinate))
                            .collect(Collectors.toList());
                    prefixCoordinateList.get(prefixCoordinateList.size() - 1).setTime(nextNode.projectCluster.gpsPoint.getTime());
                    gpsPointList.addAll(prefixCoordinateList);
                }
            }
            currNode = nextNode;
        }

        if (gpsPointList.size() > 1) {
            Timestamp endTime = currNode.projectCluster.gpsPoint.getTime();
            matchPathList.add(new MapMatchTrajectory(oid, startTime, endTime, gpsPointList));
        }
        return matchPathList;
    }

    private ClusterLinkNode projectTrajectory(Trajectory trajectory) {
        ClusterLinkNode lastNode = null;

        // 投影每个GPS点
        for (GpsPoint gpsPoint : trajectory.getSortedGpsList()) {
            // 计算GPS点的投影点
            Optional<ProjectCluster> candidatePointOpt = ProjectCluster.searchCandidatePoint(gpsPoint, rn, searchDistInM);
            if (candidatePointOpt.isPresent()) {
                // 建立与最后一个GPS点的链表关系
                ClusterLinkNode currNode = new ClusterLinkNode(candidatePointOpt.get());
                currNode.connect(lastNode);
                // 计算与最后一个GPS之间的最短路径，并计算经过不同投影点的概率
                currNode.mapMatch(rn, hmmProbability);
                // 当前GPS点变为最后一个GPS点
                lastNode = currNode;
            }
            // 如果当前GPS点没有找到任何投影点，说明附近没有道路，则该GPS点作为噪点被丢弃
        }

        return lastNode;
    }

    private ClusterLinkNode markConfirmProjectPoint(ClusterLinkNode lastNode) {
        int prevIndex = -1;
        ClusterLinkNode head = lastNode;
        while (lastNode != null) {
            int confirmedIndex;
            if (prevIndex == -1) {
                confirmedIndex = lastNode.projectCluster.bestIndex();
            } else {
                confirmedIndex = prevIndex;
            }
            lastNode.projectCluster.markedIndex = confirmedIndex;
            prevIndex = lastNode.projectCluster.getProjectPoint(confirmedIndex).getPrevIndex();
            head = lastNode;
            lastNode = lastNode.prev();
        }

        return head;
    }

    public static void main(String[] args) throws Exception {
        RoadNetwork rn = readRoadNetwork();
        Trajectory trajectory = readTrajectory();
        HmmMapMatcher mapMatcher = new HmmMapMatcher(rn, 50.0);
        List<MapMatchTrajectory> mapMatchTrajectories = mapMatcher.mapMatch(trajectory);
        for (MapMatchTrajectory mapMatchTrajectory : mapMatchTrajectories) {
            System.out.println(mapMatchTrajectory.toTrajectory().getLineString());
        }
    }

    public static RoadNetwork readRoadNetwork() throws Exception {
        List<RoadSegment> roadSegmentList = new ArrayList<>();
        URI uri = Objects.requireNonNull(TrajNoiseFilter.class.getClassLoader().getResource("road_mapmatch.csv")).toURI();
        List<String> lines = Files.readAllLines(Paths.get(uri));
        for (String line : lines) {
            roadSegmentList.add(parseRoadSegment(line));
        }
        return new RoadNetwork(roadSegmentList, false);
    }

    private static RoadSegment parseRoadSegment(String line) throws ParseException {
        String[] record = line.split("\t");
        LineString lineString = (LineString) new WKTReader().read(record[0]);
        int direction;
        if (Objects.equals(record[4], "F")) {
            direction = 2;
        } else if (Objects.equals(record[4], "T")) {
            direction = 1;
        } else {
            direction = 1;
        }
        DirectionEnum directionEnum = DirectionEnum.getByCode(direction);
        return new RoadSegment(lineString, directionEnum);
    }

    public static Trajectory readTrajectory() throws Exception {
        List<GpsPoint> gpsPointList = new ArrayList<>();
        URI uri = Objects.requireNonNull(TrajNoiseFilter.class.getClassLoader().getResource("trajectory_mapmatch.txt")).toURI();
        List<String> lines = Files.readAllLines(Paths.get(uri));
        for (String line : lines) {
            String[] split = line.split("\t");
            Coordinate coordinate = CoordinateUtil.gcj02ToWgs84(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
            gpsPointList.add(new GpsPoint("oid", coordinate, Timestamp.valueOf(split[2])));
        }
        return new Trajectory("oid", gpsPointList, true);
    }
}
