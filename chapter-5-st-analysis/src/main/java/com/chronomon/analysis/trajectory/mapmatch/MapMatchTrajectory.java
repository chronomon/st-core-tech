package com.chronomon.analysis.trajectory.mapmatch;

import org.locationtech.jts.geom.LineString;

import java.sql.Timestamp;

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
    public final String oid;

    /**
     * 轨迹起始时间
     */
    public final Timestamp startTime;

    /**
     * 轨迹终止时间
     */
    public final Timestamp endTime;

    /**
     * 匹配后的轨迹坐标，这些坐标由轨迹GPS在路段上的投影点，
     * 以及路段的坐标点按照行驶顺序组成，不再有原始的GPS点
     * 因此这些坐标点上不再有对应的时间戳
     */
    public final LineString matchedPath;

    public MapMatchTrajectory(String oid, Timestamp startTime, Timestamp endTime,
                              LineString matchedPath) {
        this.oid = oid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.matchedPath = matchedPath;
    }
}
