package com.chronomon.trajectory.staypoint;

import com.chronomon.trajectory.model.DefaultUtil;
import com.chronomon.trajectory.model.GpsPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.Assert;

import java.util.List;

public class StayPoint {

    public final String oid;
    public final List<GpsPoint> gpsPointList;

    public StayPoint(String oid, List<GpsPoint> gpsPointList) {
        this.oid = oid;
        this.gpsPointList = gpsPointList;
        Assert.isTrue(gpsPointList.size() > 1, "驻留点至少包含2两个GPS点");
    }

    public Geometry getConvexHull() {
        Coordinate[] coordinates = gpsPointList.stream()
                .map(s -> s.getGeom().getCoordinate())
                .toArray(Coordinate[]::new);

        return DefaultUtil.GEOMETRY_FACTORY
                .createLineString(coordinates)
                .convexHull();
    }
}
