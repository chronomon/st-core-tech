package com.chronomon.analysis.trajectory.cluster;

import org.locationtech.jts.geom.Coordinate;

/**
 * trajectory segment
 *
 * @author yuzisheng
 * @date 2023-11-07
 */
public class TrajectorySegment {
    /**
     * start point
     */
    private final Coordinate startPoint;
    /**
     * end point
     */
    private final Coordinate endPoint;
    /**
     * id of the trajectory containing this segment
     */
    private final String tid;

    public TrajectorySegment(Coordinate startPoint, Coordinate endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.tid = "default";
    }

    public TrajectorySegment(Coordinate startPoint, Coordinate endPoint, String tid) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.tid = tid;
    }

    /**
     * length of this segment
     */
    public double length() {
        return Math.sqrt(Math.pow(startPoint.getX() - endPoint.getX(), 2) + Math.pow(startPoint.getY() - endPoint.getY(), 2));
    }

    public double getCoord(int i) {
        if (i == 0) {
            return startPoint.getX();
        } else if (i == 1) {
            return startPoint.getY();
        } else if (i == 2) {
            return endPoint.getX();
        } else {
            return endPoint.getY();
        }
    }

    public Coordinate getStartPoint() {
        return startPoint;
    }

    public Coordinate getEndPoint() {
        return endPoint;
    }

    public String getTid() {
        return tid;
    }

    @Override
    public String toString() {
        return "LINESTRING (" + startPoint.getX() + " " + startPoint.getY() + ", " + endPoint.getX() + " " + endPoint.getY() + ")";
    }
}

