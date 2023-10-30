package com.chronomon.coretech.analysis.trajectory.mapmatch;

import com.chronomon.coretech.analysis.trajectory.mapmatch.project.ProjectCluster;
import com.chronomon.coretech.analysis.trajectory.mapmatch.project.ProjectPoint;
import com.chronomon.coretech.analysis.trajectory.mapmatch.transfer.ClusterLinkNode;
import com.chronomon.coretech.analysis.trajectory.mapmatch.transfer.HmmProbability;
import com.chronomon.coretech.analysis.trajectory.model.DefaultUtil;
import com.chronomon.coretech.analysis.trajectory.model.GpsPoint;
import com.chronomon.coretech.analysis.trajectory.model.Trajectory;
import com.chronomon.coretech.analysis.trajectory.road.IRoadSegment;
import com.chronomon.coretech.analysis.trajectory.road.RoadNetwork;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        List<Coordinate> coordinateList = new ArrayList<>();
        Timestamp startTime = currNode.projectCluster.gpsPoint.getTime();
        while (currNode.hasNext()) {
            ClusterLinkNode nextNode = currNode.next();
            if (nextNode.projectCluster.isStuck) {
                if (coordinateList.size() > 1) {
                    LineString matchedPath = DefaultUtil.GEOMETRY_FACTORY.createLineString(coordinateList.toArray(new Coordinate[0]));
                    Timestamp endTime = currNode.projectCluster.gpsPoint.getTime();
                    matchPathList.add(new MapMatchTrajectory(oid, startTime, endTime, matchedPath));
                    startTime = nextNode.projectCluster.gpsPoint.getTime();
                }
                coordinateList = new ArrayList<>();
            } else {
                int currConfirmedIndex = currNode.projectCluster.markedIndex;
                ProjectPoint currentProject = currNode.projectCluster.getProjectPoint(currConfirmedIndex);
                int nextConfirmedIndex = nextNode.projectCluster.markedIndex;
                ProjectPoint nextProjectPoint = nextNode.projectCluster.getProjectPoint(nextConfirmedIndex);
                assert currConfirmedIndex == nextProjectPoint.getPrevIndex();

                if (currentProject.onSameSegmentAndBefore(nextProjectPoint)) {
                    coordinateList.add(currentProject.point.getCoordinate());
                    IRoadSegment roadSegment = currentProject.roadSegment;
                    for (int i = currentProject.segmentIndex + 1; i <= nextProjectPoint.segmentIndex; i++) {
                        coordinateList.add(roadSegment.getCoordinateN(i));
                    }
                    coordinateList.add(nextProjectPoint.point.getCoordinate());
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
                    if (nextProjectPoint.isNormal()) {
                        coordinateList.addAll(currentProject.getSuffixCoordinates());
                        coordinateList.addAll(nextProjectPoint.getPrefixCoordinates());
                    }
                }
            }
            currNode = nextNode;
        }

        if (coordinateList.size() > 1) {
            LineString matchedPath = DefaultUtil.GEOMETRY_FACTORY.createLineString(coordinateList.toArray(new Coordinate[0]));
            Timestamp endTime = currNode.projectCluster.gpsPoint.getTime();
            matchPathList.add(new MapMatchTrajectory(oid, startTime, endTime, matchedPath));
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
}
