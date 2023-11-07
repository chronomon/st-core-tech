package com.chronomon.trajectory.road;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.util.List;

/**
 * 路段接口
 */
public interface IRoadSegment {

    int getRoadId();

    RoadNode getFromNode();

    RoadNode getToNode();

    Coordinate getCoordinateN(int n);

    Point getPointN(int n);

    int getNumPoints();

    Envelope getEnvelop();

    boolean intersects(Geometry geom);

    List<Coordinate> getCoordinateList();

    double getLengthInM();

    double distanceFromStartInM(int index);

}
