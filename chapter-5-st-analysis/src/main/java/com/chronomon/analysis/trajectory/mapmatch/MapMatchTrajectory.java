package com.chronomon.analysis.trajectory.mapmatch;

import com.chronomon.analysis.trajectory.model.GpsPoint;
import com.chronomon.analysis.trajectory.model.Trajectory;

import java.sql.Timestamp;
import java.util.List;

/**
 * 地图匹配后的轨迹
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class MapMatchTrajectory {

    /**
     * 对象ID
     */
    private final String oid;

    /**
     * 轨迹起始时间
     */
    private final Timestamp startTime;

    /**
     * 轨迹终止时间
     */
    private final Timestamp endTime;

    /**
     * 匹配后的轨迹坐标，这些坐标由轨迹GPS在路段上的投影点，
     * 以及路段的坐标点按照行驶顺序组成，不再有原始的GPS点
     * 因此这些坐标点上不再有对应的时间戳
     */
    private final List<GpsPoint> matchedPath;

    public MapMatchTrajectory(String oid, Timestamp startTime, Timestamp endTime,
                              List<GpsPoint> matchedPath) {
        this.oid = oid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.matchedPath = matchedPath;
    }

    public Trajectory toTrajectory() {
        int startIndex = -1;
        for (int i = 0; i < matchedPath.size(); i++) {
            if (matchedPath.get(i).getTime() != null) {
                if (startIndex == -1) {
                    startIndex = i;
                    continue;
                }

                GpsPoint startGps = matchedPath.get(startIndex);
                GpsPoint endGps = matchedPath.get(i);
                long startTime = startGps.getTime().getTime();
                long endTime = endGps.getTime().getTime();
                long avgTimeInterval = (endTime - startTime) / (i - startIndex);

                for (int j = startIndex + 1; j < i; j++) {
                    startTime = startTime + avgTimeInterval;
                    matchedPath.get(j).setTime(new Timestamp(startTime));
                }
                startIndex = i;
            }
        }

        return new Trajectory(oid, matchedPath, true);
    }
}
