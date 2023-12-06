package com.chronomon.analysis.trajectory.flink;

import com.chronomon.analysis.trajectory.mapmatch.HmmMapMatcher;
import com.chronomon.analysis.trajectory.mapmatch.MapMatchTrajectory;
import com.chronomon.analysis.trajectory.mapmatch.project.ProjectCluster;
import com.chronomon.analysis.trajectory.mapmatch.transfer.ClusterLinkNode;
import com.chronomon.analysis.trajectory.road.RoadNetworkHolder;
import com.chronomon.analysis.trajectory.road.RoadSegment;
import com.chronomon.analysis.trajectory.model.GpsPoint;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.List;
import java.util.Optional;

public class TrajMapMatchFunction extends KeyedProcessFunction<String, GpsPoint, MapMatchTrajectory> {

    private transient int count = 0;

    private final String rnCacheKey;

    private final List<RoadSegment> roadSegmentList;

    private final double searchDistInM;

    private transient ValueState<ClusterLinkNode> recentState;

    private transient HmmMapMatcher hmmMapMatcher;

    public TrajMapMatchFunction(String rnCacheKey, List<RoadSegment> roadSegmentList, double searchDistInM) {
        this.rnCacheKey = rnCacheKey;
        this.roadSegmentList = roadSegmentList;
        this.searchDistInM = searchDistInM;
    }

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<ClusterLinkNode> recentDesc = new ValueStateDescriptor<>("recent", ClusterLinkNode.class);
        recentState = getRuntimeContext().getState(recentDesc);
        hmmMapMatcher = new HmmMapMatcher(RoadNetworkHolder.getRoadNetwork(roadSegmentList, rnCacheKey), searchDistInM);
    }

    @Override
    public void processElement(GpsPoint gpsPoint,
                               KeyedProcessFunction<String, GpsPoint, MapMatchTrajectory>.Context context,
                               Collector<MapMatchTrajectory> collector) throws Exception {


        Optional<ProjectCluster> candidatePointOpt =
                ProjectCluster.searchCandidatePoint(gpsPoint, hmmMapMatcher.rn, searchDistInM);
        if (!candidatePointOpt.isPresent()) {
            return;
        }

        ClusterLinkNode prevNode = recentState.value();
        ClusterLinkNode currNode = new ClusterLinkNode(candidatePointOpt.get());
        currNode.connect(prevNode);
        currNode.mapMatch(hmmMapMatcher.rn, hmmMapMatcher.hmmProbability);

        if (!currNode.hasPrev()) {
            // 说明currNode是第一个节点
            return;
        }

        String oid = gpsPoint.getOid();
        if (currNode.projectCluster.isStuck) {
            // 当前节点与前置节点不连通
            // 输出匹配结果
            List<MapMatchTrajectory> matchedPaths = hmmMapMatcher.buildMatchedTrajectory(oid, currNode);
            matchedPaths.forEach(collector::collect);
            // 清空缓存(断开链表方便垃圾回收)
            prevNode.setNextNode(null);
            currNode.setPrevNode(null);
        } else if (prevNode.projectCluster.mustPassIndex != -1) {
            // 前置节点中存在必经投影点，将前置节点之前的所有缓存内容输出
            prevNode.projectCluster.getProjectPoint(prevNode.projectCluster.mustPassIndex).setMetric(Double.POSITIVE_INFINITY);
            // 输出匹配结果
            List<MapMatchTrajectory> matchedPaths = hmmMapMatcher.buildMatchedTrajectory(oid, prevNode);
            matchedPaths.forEach(collector::collect);
            // 清空缓存(断开链表方便垃圾回收)
            if (prevNode.hasPrev()) {
                prevNode.prev().setNextNode(null);
            }
            prevNode.setPrevNode(null);
        } else {
            // 地图匹配的连续性没有断掉，且没有出现必经投影点，则每2个点输出一次
            count++;
            if (count < 2) {
                return;
            }
            count = 0;
            List<MapMatchTrajectory> matchedPaths = hmmMapMatcher.buildMatchedTrajectory(oid, currNode);
            assert matchedPaths.size() == 1;
            collector.collect(matchedPaths.get(0));
        }
    }

    public static DataStream<MapMatchTrajectory> mapMatch(DataStream<GpsPoint> gpsPointStream,
                                                          List<RoadSegment> roadSegmentList,
                                                          double searchDistInM) {

        String rnCacheKey = RoadNetworkHolder.generateKey();
        return gpsPointStream
                .keyBy((KeySelector<GpsPoint, String>) GpsPoint::getOid)
                .process(new TrajMapMatchFunction(rnCacheKey, roadSegmentList, searchDistInM));
    }
}
