package com.chronomon.coretech.analysis.trajectory.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import java.sql.Timestamp;

/**
 * GPS点：由空间位置geom和时间戳time组成
 */
public class GpsPoint implements Comparable<GpsPoint> {

    /**
     * 实体ID
     */
    private final String oid;

    /**
     * 空间位置
     */
    private final Point geom;

    /**
     * 时间戳
     */
    private final Timestamp time;

    /**
     * @param oid  实体ID
     * @param geom 空间位置
     * @param time 时间戳
     */
    public GpsPoint(String oid, Point geom, Timestamp time) {
        this.oid = oid;
        this.geom = geom;
        this.time = time;
    }

    /**
     * @param oid  实体ID
     * @param lng  经度
     * @param lat  纬度
     * @param time 时间戳
     */
    public GpsPoint(String oid, Double lng, Double lat, Timestamp time) {
        this.oid = oid;
        this.geom = DefaultUtil.GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        this.time = time;
    }

    public String getOid() {
        return oid;
    }

    public Point getGeom() {
        return geom;
    }

    public Timestamp getTime() {
        return time;
    }

    /**
     * 计算两个GPS点的球面距离
     *
     * @param other 另外一个GPS点
     * @return 球面距离（米）
     */
    public double distInMeter(GpsPoint other) {
        return DistanceUtil.distInMeter(this.geom, other.geom);
    }

    /**
     * 计算两个GPS点的时间差
     *
     * @param other 另外一个GPS点
     * @return 时间差（秒）
     */
    public long timeIntervalInSec(GpsPoint other) {
        return Math.abs(this.time.getTime() - other.time.getTime()) / 1000;
    }

    /**
     * 计算两个GPS点的平均速度
     *
     * @param other 另外一个GPS点
     * @return 平均速度（米/秒）
     */
    public double speedInMeterPerSec(GpsPoint other) {
        return distInMeter(other) / timeIntervalInSec(other);
    }

    @Override
    public int compareTo(GpsPoint o) {
        return this.time.compareTo(o.time);
    }
}
