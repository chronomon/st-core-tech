package com.chronomon.analysis.trajectory.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.Collections;
import java.util.List;

/**
 * 轨迹对象：包括对象ID和按照时间戳排序的GPS点序列
 */
public class Trajectory {

    /**
     * 对象ID
     */
    private final String oid;

    /**
     * 按照时间戳排序的GPS点序列
     */
    private final List<GpsPoint> sortedGpsList;

    public Trajectory(String oid, List<GpsPoint> gpsList) {
        this(oid, gpsList, false);
    }

    public Trajectory(String oid, List<GpsPoint> gpsList, boolean isSorted) {
        this.oid = oid;
        this.sortedGpsList = gpsList;
        if (gpsList.size() < 2) {
            throw new IllegalArgumentException("轨迹至少应该包含2个GPS点");
        }
        if (!isSorted) {
            Collections.sort(gpsList);
        }
    }

    public String getOid() {
        return oid;
    }

    public GpsPoint getFirstGpsPoint() {
        return sortedGpsList.get(0);
    }

    public GpsPoint getLastGpsPoint() {
        return sortedGpsList.get(getNumPoints() - 1);
    }

    public GpsPoint getGpsPoint(int index) {
        return sortedGpsList.get(index);
    }

    public int getNumPoints() {
        return sortedGpsList.size();
    }

    public List<GpsPoint> getSortedGpsList() {
        return sortedGpsList;
    }

    public LineString getLineString() {
        Coordinate[] coordinates = sortedGpsList.stream()
                .map(s -> s.getGeom().getCoordinate())
                .toArray(Coordinate[]::new);
        return DefaultUtil.GEOMETRY_FACTORY.createLineString(coordinates);
    }
}
