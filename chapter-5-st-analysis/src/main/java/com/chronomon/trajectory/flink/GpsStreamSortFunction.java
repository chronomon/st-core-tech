package com.chronomon.trajectory.flink;

import com.chronomon.trajectory.model.GpsPoint;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GpsStreamSortFunction extends ProcessWindowFunction<GpsPoint, GpsPoint, String, TimeWindow> {

    @Override
    public void process(String s,
                        ProcessWindowFunction<GpsPoint, GpsPoint, String, TimeWindow>.Context context,
                        Iterable<GpsPoint> elements,
                        Collector<GpsPoint> collector) {

        List<GpsPoint> gpsHolder = new ArrayList<>();
        for (GpsPoint element : elements) {
            gpsHolder.add(element);
        }

        gpsHolder.sort(Comparator.comparing(GpsPoint::getTime));
        gpsHolder.forEach(collector::collect);
    }


    /**
     * 对GPS实时数据流按照时间排序
     *
     * @param rawGpsStream     原始数据流
     * @param windowSpanInSec  滑动时间窗口大小(秒)
     * @param maxWaitTimeInSec 最大等待时间(秒)
     * @return 排序后的数据流
     */
    public static DataStream<GpsPoint> sort(DataStream<GpsPoint> rawGpsStream,
                                            long windowSpanInSec,
                                            long maxWaitTimeInSec) {
        // 保证窗口大于等于等待时间
        if (windowSpanInSec < maxWaitTimeInSec) {
            windowSpanInSec = maxWaitTimeInSec;
        }

        WatermarkStrategy<GpsPoint> strategy = WatermarkStrategy
                .<GpsPoint>forBoundedOutOfOrderness(Duration.ofSeconds(maxWaitTimeInSec))
                .withTimestampAssigner(
                        (SerializableTimestampAssigner<GpsPoint>) (gpsPoint, l) -> gpsPoint.getTime().getTime()
                );

        return rawGpsStream.assignTimestampsAndWatermarks(strategy)
                .keyBy((KeySelector<GpsPoint, String>) GpsPoint::getOid)
                .window(TumblingEventTimeWindows.of(Time.seconds(windowSpanInSec)))
                .process(new GpsStreamSortFunction());
    }
}
