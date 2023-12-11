package com.chronomon.analysis.trajectory.mapmatch.project;

import com.chronomon.analysis.trajectory.model.DistanceUtil;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import com.chronomon.analysis.trajectory.road.ReversedRoadSegment;
import com.chronomon.analysis.trajectory.road.RoadNetwork;
import com.chronomon.analysis.trajectory.road.RoadSegment;
import com.chronomon.analysis.trajectory.road.RoadSegmentVisitor;
import com.chronomon.analysis.trajectory.model.GpsPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 投影点簇：维护了一个GPS点在搜索半径内的路段上的投影点集合
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class ProjectCluster {

    /**
     * 原始的轨迹GPS点
     */
    public final GpsPoint gpsPoint;

    /**
     * GPS点在搜索半径内的路段上的投影点集合
     */
    public List<ProjectPoint> projectPointList;

    /**
     * 轨迹地图匹配时，用于标识该投影点簇是否为一个死结
     * 死结表示当前投影点簇与下一个投影点簇之间没有一对投影点之间存在路网最短路径
     * 如果当前投影点簇为一个死结，则将轨迹从此处截断
     */
    public boolean isStuck = false;

    /**
     * 当前投影点簇处用隐马尔科夫模型预测出来的最有可能经过的投影点
     * 该索引值指向projectPointList中的一个投影点
     */
    public int markedIndex = -1;

    /**
     * 当前投影点簇中的必经投影点，一个投影点簇中不一定存在必经投影点
     * 当下一个投影点簇中的所有投影点的前置投影点都指向当前投影点簇中的同一个投影点时，
     * 这个投影点就叫必经投影点
     * 该索引值指向projectPointList中的一个投影点
     */
    public int mustPassIndex = -1;

    public ProjectCluster(GpsPoint gpsPoint, List<ProjectPoint> projectPointList) {
        this.gpsPoint = gpsPoint;
        this.projectPointList = projectPointList;
    }

    /**
     * 计算当前投影点簇中，最大概率的投影点
     *
     * @return 最大概率投影点在projectPointList中的位置
     */
    public Integer bestIndex() {
        if (mustPassIndex != -1) {
            return mustPassIndex;
        }
        Integer bestIndex = null;
        double bestMetric = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < projectPointList.size(); i++) {
            if (projectPointList.get(i).getMetric() > bestMetric) {
                bestMetric = projectPointList.get(i).getMetric();
                bestIndex = i;
            }
        }
        assert bestIndex != null;
        return bestIndex;
    }

    /**
     * 获取某个投影点
     *
     * @param index 投影点索引值
     * @return 拖影点对象
     */
    public ProjectPoint getProjectPoint(int index) {
        return projectPointList.get(index);
    }

    /**
     * 在路网集合上搜索GPS点的投影点簇的静态方法
     *
     * @param gpsPoint      GPS点
     * @param rn            路网对象
     * @param searchDistInM 投影点的搜索半径（米）
     * @return 投影点簇
     */
    public static Optional<ProjectCluster> searchCandidatePoint(GpsPoint gpsPoint, RoadNetwork rn, double searchDistInM) {
        // 获取路段构成的RTree空间索引
        STRtree roadRtree = rn.getRoadRtree();

        // 在空间索引中查询搜索框内的路段，注意此处的搜索范围是一个以GPS为中心的正方形而非圆形
        Envelope queryEnv = DistanceUtil.expandEnv(gpsPoint.getGeom(), searchDistInM);
        RoadSegmentVisitor visitor = new RoadSegmentVisitor(queryEnv);
        roadRtree.query(queryEnv, visitor);
        List<RoadSegment> candidateSegmentList = visitor.segmentList;

        // 计算GPS点在每个路段上的投影点，并保留GPS点与投影点距离小于搜索半径的那些投影点
        List<ProjectPoint> projectPointList = new ArrayList<>(candidateSegmentList.size());
        for (RoadSegment segment : candidateSegmentList) {
            ProjectPoint projectPoint = ProjectPoint.project(gpsPoint.getGeom(), segment);
            if (projectPoint.projectDistInM <= searchDistInM) {
                projectPointList.add(projectPoint);
                if (segment.getReversedOne().isPresent()) {
                    // 如果是双向路，复制投影点
                    ReversedRoadSegment reversedSegment = segment.getReversedOne().get();
                    int reversedSegmentIndex = segment.getNumPoints() - projectPoint.segmentIndex - 2;
                    double reversedOffsetDistInM = segment.getLengthInM() - projectPoint.offsetDistInM;
                    ProjectPoint reversed = new ProjectPoint(reversedSegment, reversedSegmentIndex,
                            projectPoint.point, projectPoint.projectDistInM,
                            reversedOffsetDistInM);
                    projectPointList.add(reversed);
                }
            }
        }

        if (projectPointList.size() > 0) {
            return Optional.of(new ProjectCluster(gpsPoint, projectPointList));
        } else {
            return Optional.empty();
        }
    }
}
