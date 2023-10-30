package com.chronomon.coretech.analysis.trajectory.flink;

import com.chronomon.coretech.analysis.trajectory.model.GpsPoint;
import com.chronomon.coretech.analysis.trajectory.staypoint.StayPoint;
import com.chronomon.coretech.analysis.trajectory.staypoint.TrajStayPointDetector;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.ArrayList;
import java.util.List;

public class TrajStayPointDetectFunction extends KeyedProcessFunction<String, GpsPoint, StayPoint> {

    private final double maxStayDistInMeter;
    private final long minStayTimeInSecond;

    private final OutputTag<GpsPoint> movingGpsPointTag;

    private final boolean needMovingGpsPoint;

    private TrajStayPointDetectFunction(double maxStayDistInMeter, long minStayTimeInSecond) {
        this(maxStayDistInMeter, minStayTimeInSecond, null);
    }

    private TrajStayPointDetectFunction(double maxStayDistInMeter, long minStayTimeInSecond,
                                        OutputTag<GpsPoint> movingGpsPointTag) {

        this.maxStayDistInMeter = maxStayDistInMeter;
        this.minStayTimeInSecond = minStayTimeInSecond;
        this.movingGpsPointTag = movingGpsPointTag;
        this.needMovingGpsPoint = movingGpsPointTag != null;
    }

    private transient ValueState<List<GpsPoint>> candidatePointState = null;

    transient private TrajStayPointDetector trajStayPointDetector = null;

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<List<GpsPoint>> candidatePointDesc =
                new ValueStateDescriptor<>("candidateGpsPoints", Types.LIST(Types.GENERIC(GpsPoint.class)));
        candidatePointState = this.getRuntimeContext().getState(candidatePointDesc);
        trajStayPointDetector = new TrajStayPointDetector(maxStayDistInMeter, minStayTimeInSecond);
    }

    @Override
    public void processElement(GpsPoint recentPoint,
                               KeyedProcessFunction<String, GpsPoint, StayPoint>.Context context,
                               Collector<StayPoint> collector) throws Exception {

        // 将第一个GPS点作为锚点加入缓存列表中，并将锚点写入行进GPS点的侧输出流
        if (candidatePointState.value() == null) {
            List<GpsPoint> candidatePointList = new ArrayList<>();
            candidatePointList.add(recentPoint);
            candidatePointState.update(candidatePointList);
            // 将第一个GPS点写入行进GPS点侧输出流
            if (needMovingGpsPoint) {
                context.output(movingGpsPointTag, recentPoint);
            }
            // 结束第一个GPS点的处理流程
            return;
        }

        List<GpsPoint> candidatePointList = candidatePointState.value();
        GpsPoint anchorPoint = candidatePointList.get(0);
        if (trajStayPointDetector.isExceedMaxDistThreshold(anchorPoint, recentPoint)) {
            // 空间距离检测：最新GPS点与锚点的距离超过了最大驻留距离，
            // 则缓存列表中的GPS点有可能会形成一个驻留点
            if (candidatePointList.size() > 1) {
                // 缓存列表中有2个以上的GPS点，这是形成驻留点的必要条件
                GpsPoint lastPoint = candidatePointList.get(candidatePointList.size() - 1);
                boolean timeFlag = trajStayPointDetector.isExceedMinTimeThreshold(anchorPoint, lastPoint);
                if (timeFlag) {
                    // 时间跨度检测：缓存列表中首尾GPS点的时间跨度大于最小驻留时长，
                    // 说明缓存列表中的GPS点形成了一个驻留点
                    String oid = context.getCurrentKey();
                    StayPoint stayPoint = new StayPoint(oid, candidatePointList);
                    collector.collect(stayPoint);
                    // 将驻留点的最后一个GPS点写入行进GPS点输出流
                    if (needMovingGpsPoint) {
                        context.output(movingGpsPointTag, lastPoint);
                    }
                    // 创建空的缓存列表，并将最新GPS点作为新的锚点
                    candidatePointState.update(new ArrayList<>());
                    candidatePointState.value().add(recentPoint);
                    // 将最新的锚点写入行进GPS点输出流
                    if (needMovingGpsPoint) {
                        context.output(movingGpsPointTag, recentPoint);
                    }
                } else {
                    // 时间跨度检测：缓存列表首尾GPS点的时间跨度太短，不符合驻留点要求
                    // 缓存列表中的第一个GPS点不满足锚点条件，直接删除，并将最新GPS点加入缓存列表
                    candidatePointList.remove(0);
                    candidatePointList.add(recentPoint);
                    //从缓存列表头部开始，寻找下一个锚点
                    boolean isHeadAnchor = false;
                    while (!isHeadAnchor) {
                        // 缓存列表首部的GPS点要么是锚点，要么是行进GPS点
                        // 不管哪种情况，都是需要写入到行进GPS点输出流
                        GpsPoint movingGpsPoint = candidatePointList.remove(0);
                        if (needMovingGpsPoint) {
                            context.output(movingGpsPointTag, movingGpsPoint);
                        }

                        boolean isExceedMaxDist = false;
                        for (int i = 1; i < candidatePointList.size() && !isExceedMaxDist; i++) {
                            if (trajStayPointDetector.isExceedMaxDistThreshold(
                                    candidatePointList.get(0),
                                    candidatePointList.get(i))) {
                                isExceedMaxDist = true;
                            }
                        }
                        isHeadAnchor = !isExceedMaxDist;
                    }
                }
            } else {
                // 缓存列表里面只有一个GPS点，且与最新GPS点不可能形成驻留点，
                // 删除原有GPS点，将最新的GPS点加入缓存列表， 最新GPS点将作为新的锚点
                // 最后将最新GPS点输出到行进GPS点出流中
                candidatePointList.remove(0);
                candidatePointList.add(recentPoint);
                if (needMovingGpsPoint) {
                    context.output(movingGpsPointTag, recentPoint);
                }
            }
        } else {
            // 最新GPS点与锚点的距离没有超过最大驻留距离
            // 说明最新GPS点有可能处于驻留状态，将其点加入缓存列表
            candidatePointList.add(recentPoint);
        }
    }

    public static DataStream<StayPoint> detect(DataStream<GpsPoint> gpsStream,
                                               double maxStayDistInMeter,
                                               long minStayTimeInSecond,
                                               boolean needMoving) {

        OutputTag<GpsPoint> movingGpsPointTag = null;
        if (needMoving) {
            movingGpsPointTag = new OutputTag<>("movingGpsPoint");
        }

        TrajStayPointDetectFunction stayPointDetectFunction =
                new TrajStayPointDetectFunction(maxStayDistInMeter, minStayTimeInSecond, movingGpsPointTag);
        SingleOutputStreamOperator<StayPoint> stayPointStream = gpsStream
                .keyBy((KeySelector<GpsPoint, String>) GpsPoint::getOid)
                .process(stayPointDetectFunction);

        if (needMoving) {
            DataStream<GpsPoint> movingGpsPointStream = stayPointStream.getSideOutput(movingGpsPointTag);
        }
        //todo: 封装输出结果

        return stayPointStream;
    }
}
