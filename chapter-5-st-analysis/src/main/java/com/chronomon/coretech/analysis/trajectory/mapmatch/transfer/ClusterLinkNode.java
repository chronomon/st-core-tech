package com.chronomon.coretech.analysis.trajectory.mapmatch.transfer;

import com.chronomon.coretech.analysis.trajectory.mapmatch.project.ProjectCluster;
import com.chronomon.coretech.analysis.trajectory.mapmatch.project.ProjectPoint;
import com.chronomon.coretech.analysis.trajectory.model.DistanceUtil;
import com.chronomon.coretech.analysis.trajectory.road.IRoadSegment;
import com.chronomon.coretech.analysis.trajectory.road.RoadNetwork;
import com.chronomon.coretech.analysis.trajectory.road.RoadNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 投影点簇的链表节点
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class ClusterLinkNode {

    /**
     * 两个GPS点之间的最大时间间隔，如果超过值，则将轨迹进行分段后再匹配
     * 因为如果两个GPS点之间的时间跨度很大时，预测出来的路径也就没有意义
     */
    private static final int MAX_TIME_INTERVAL_IN_SEC = 300;

    /**
     * 当前节点中存储的投影点簇
     */
    public final ProjectCluster projectCluster;

    /**
     * 前驱节点
     */
    private ClusterLinkNode prevNode = null;

    /**
     * 后继结点
     */
    private ClusterLinkNode nextNode = null;

    public ClusterLinkNode(ProjectCluster projectCluster) {
        this.projectCluster = projectCluster;
    }

    public void connect(ClusterLinkNode prevClusterLinkNode) {
        this.prevNode = prevClusterLinkNode;
        if (prevClusterLinkNode != null) {
            prevClusterLinkNode.nextNode = this;
        }
    }

    public boolean hasPrev() {
        return prevNode != null;
    }

    public boolean hasNext() {
        return nextNode != null;
    }

    public ClusterLinkNode prev() {
        return prevNode;
    }

    public ClusterLinkNode next() {
        return nextNode;
    }

    public void setPrevNode(ClusterLinkNode prevNode) {
        this.prevNode = prevNode;
    }

    public void setNextNode(ClusterLinkNode nextNode) {
        this.nextNode = nextNode;
    }

    public void mapMatch(RoadNetwork rn, HmmProbability hmmProbability) {
        List<ProjectPoint> currProjectPoints = projectCluster.projectPointList;

        if (prevNode != null &&
                prevNode.projectCluster.gpsPoint.timeIntervalInSec(projectCluster.gpsPoint) < MAX_TIME_INTERVAL_IN_SEC) {

            //准备前后两个GPS点对应的投影点的集合
            ProjectCluster prevProjectCluster = prevNode.projectCluster;
            List<ProjectPoint> prevProjectPointList = prevProjectCluster.projectPointList;

            Set<RoadNode> prevRoadNodes = prevProjectPointList.stream()
                    .map(projectPoint -> projectPoint.roadSegment.getToNode())
                    .collect(Collectors.toSet());
            Set<RoadNode> currRoadNodes = currProjectPoints.stream()
                    .map(projectPoint -> projectPoint.roadSegment.getFromNode())
                    .collect(Collectors.toSet());

            //计算两个投影点之间的最短路径
            ShortestPathCalculator calculator = new ShortestPathCalculator(rn);
            ShortestPathCalculator.ShortestPathSet shortestPathSet = calculator.calculate(prevRoadNodes, currRoadNodes);

            //计算当前GPS点对应投影点的概率值
            int unConnectiveCount = 0;
            List<ProjectPoint> validCurrProjectPoints = new ArrayList<>(currProjectPoints.size());
            for (ProjectPoint currProjectPoint : currProjectPoints) {
                double emissionProbability = hmmProbability.emissionProbability(currProjectPoint.projectDistInM);

                double bestMetric = Double.NEGATIVE_INFINITY;
                int bestPrevIndex = -1;
                List<IRoadSegment> bestPathSegments = null;
                double bestGraphDistance = 0.0;
                double bestLinearDistance = 0.0;
                for (int prevIndex = 0; prevIndex < prevProjectPointList.size(); prevIndex++) {
                    ProjectPoint prevProjectPoint = prevProjectPointList.get(prevIndex);
                    IRoadSegment prevRoadSegment = prevProjectPoint.roadSegment;
                    IRoadSegment currRoadSegment = currProjectPoint.roadSegment;

                    // 求两个投影点之间的路网距离和途径路段
                    double graphDistance;
                    List<IRoadSegment> pathSegments = Collections.emptyList();
                    if (prevProjectPoint.onSameSegmentAndBefore(currProjectPoint)) {
                        // 两个投影点在同一个路段上，且时间早的投影点在前，时间晚的投影点在后
                        graphDistance = currProjectPoint.offsetDistInM - prevProjectPoint.offsetDistInM;
                    } else if (prevRoadSegment.getToNode().equals(currRoadSegment.getFromNode())) {
                        // 两个投影点在相邻路段上，上一个路段的终点是下一个路段的起点
                        graphDistance = currProjectPoint.offsetDistInM + (prevRoadSegment.getLengthInM() - prevProjectPoint.offsetDistInM);
                    } else {
                        // 两个投影点所在的路段没有连接在一起，上一个路段的终点和下一个路段的起点之间存在最短路径
                        Optional<ShortestPathCalculator.ShortestPath> shortestPathOpt = shortestPathSet.getShortestPath(prevRoadSegment.getToNode(), currRoadSegment.getFromNode());
                        if (shortestPathOpt.isPresent()) {
                            graphDistance = shortestPathOpt.get().pathLength + (prevRoadSegment.getLengthInM() - prevProjectPoint.offsetDistInM) + currProjectPoint.offsetDistInM;
                            pathSegments = shortestPathOpt.get().segmentList;
                        } else {
                            graphDistance = Double.POSITIVE_INFINITY;
                        }
                    }

                    // 计算当前投影点上的概率值
                    if (graphDistance < Double.POSITIVE_INFINITY) {
                        // 投影点之间在路网上是可达的
                        double linearDistance = DistanceUtil.distInMeter(prevProjectPoint.point, currProjectPoint.point);
                        double transitionProbability = hmmProbability.transitionProbability(graphDistance, linearDistance);
                        double metric = prevProjectPoint.getMetric() + emissionProbability + transitionProbability;
                        if (metric > bestMetric) {
                            // 保留概率最大的一个前置投影点
                            bestMetric = metric;
                            bestPrevIndex = prevIndex;
                            bestPathSegments = pathSegments;
                            bestGraphDistance = graphDistance;
                            bestLinearDistance = linearDistance;
                        }
                    }
                }
                //save hmm metric of the current project point
                if (bestPrevIndex != -1) {
                    currProjectPoint.setPrevIndex(bestPrevIndex);
                    currProjectPoint.setMetric(bestMetric);
                    currProjectPoint.setPathSegments(bestPathSegments);
                    if (bestGraphDistance / bestLinearDistance > 3) {
                        // 如果路网距离是直线距离的3倍以上，标记为异常
                        currProjectPoint.setNormal(false);
                    }
                    validCurrProjectPoints.add(currProjectPoint);
                } else {
                    currProjectPoint.setMetric(emissionProbability);
                    currProjectPoint.setPathSegments(Collections.emptyList());
                    unConnectiveCount++;
                }
            }

            // isStuck=true表示当前GPS点与前置GPS点的任意两个投影点之间都不连通
            projectCluster.isStuck = unConnectiveCount == currProjectPoints.size();
            if (!projectCluster.isStuck) {
                projectCluster.projectPointList = validCurrProjectPoints;
                // confirmedIndex表示必经投影点在投影点列表中的位置
                List<Integer> validPrevIndexList = validCurrProjectPoints.stream()
                        .map(ProjectPoint::getPrevIndex)
                        .distinct().collect(Collectors.toList());
                if (validPrevIndexList.size() == 1) {
                    //notice: 道路的方向对必经点的跨度影响很大，不能粗暴地将路段都设成双向
                    prevNode.projectCluster.mustPassIndex = validPrevIndexList.get(0);
                }
            }
        } else {
            // 前置点为空，或者前后两GPS的时间差过大时，认为前后两GPS不连通，初始概率值为发射概率
            projectCluster.isStuck = true;
            for (ProjectPoint projectPoint : currProjectPoints) {
                double metric = hmmProbability.emissionProbability(projectPoint.projectDistInM);
                projectPoint.setMetric(metric);
            }
        }
    }
}
