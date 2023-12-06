package com.chronomon.analysis.trajectory.cluster;

import org.locationtech.jts.geom.Coordinate;

/**
 * distance util for trajectory cluster
 *
 * @author yuzisheng
 * @date 2023-11-07
 */
public class TrajectoryClusterUtil {
    /**
     * dimension of point
     */
    static final int POINT_DIM = 2;
    /**
     * some temp variables
     */
    static double coefficient;
    static double[] vector1 = new double[2];
    static double[] vector2 = new double[2];
    static double[] projectionVector = new double[2];

    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    /**
     * compute vector length
     */
    public static double computeVectorLength(double[] vector) {
        double squareSum = 0.0;
        for (double value : vector) {
            squareSum += Math.pow(value, 2);
        }
        return Math.sqrt(squareSum);
    }

    /**
     * compute inner product of two vectors
     */
    public static double computeInnerProduct(double[] v1, double[] v2) {
        int vectorDim = v1.length;
        double innerProduct = 0.0;
        for (int i = 0; i < vectorDim; i++) {
            innerProduct += (v1[i] * v2[i]);
        }
        return innerProduct;
    }

    /**
     * compute euclidean distance between two points
     */
    public static double computePointToPointDistance(Coordinate p1, Coordinate p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    /**
     * compute perpendicular distance between two segments
     */
    public static double computePerpendicularDistance(TrajectorySegment s1, TrajectorySegment s2) {
        if (s1.length() < s2.length()) {
            TrajectorySegment temp = s1;
            s1 = s2;
            s2 = temp;
        }
        double distance1 = computePointToSegmentDistance(s2.getStartPoint(), s1);
        double distance2 = computePointToSegmentDistance(s2.getEndPoint(), s1);
        if (distance1 == 0.0 && distance2 == 0.0) {
            return 0.0;
        }
        return (Math.pow(distance1, 2) + Math.pow(distance2, 2)) / (distance1 + distance2);
    }

    /**
     * compute angle distance between two segments
     */
    public static double computeAngleDistance(TrajectorySegment s1, TrajectorySegment s2) {
        for (int i = 0; i < POINT_DIM; i++) {
            vector1[i] = s1.getCoord(i + POINT_DIM) - s1.getCoord(i);
            vector2[i] = s2.getCoord(i + POINT_DIM) - s2.getCoord(i);
        }
        double vectorLength1 = computeVectorLength(vector1);
        double vectorLength2 = computeVectorLength(vector2);
        if (vectorLength1 == 0.0 || vectorLength2 == 0.0) return 0.0;

        double innerProduct = computeInnerProduct(vector1, vector2);
        double cosTheta = innerProduct / (vectorLength1 * vectorLength2);
        if (cosTheta > 1.0) cosTheta = 1.0;
        if (cosTheta < -1.0) cosTheta = -1.0;
        double sinTheta = Math.sqrt(1 - Math.pow(cosTheta, 2));
        return (vectorLength2 * sinTheta);
    }

    /**
     * compute distance between two segments
     */
    public static double computeSegmentToSegmentDistance(TrajectorySegment s1, TrajectorySegment s2) {
        double perDistance;
        double parDistance;
        double angleDistance;

        double segmentLength1 = s1.length();
        double segmentLength2 = s2.length();
        if (segmentLength1 < segmentLength2) {
            TrajectorySegment temp = s1;
            s1 = s2;
            s2 = temp;
        }

        double perDistance1, perDistance2;
        double parDistance1, parDistance2;
        perDistance1 = computePointToSegmentDistance(s2.getStartPoint(), s1);
        if (coefficient < 0.5) {
            parDistance1 = computePointToPointDistance(s1.getStartPoint(), vectorToSpatialCoord(projectionVector));
        } else {
            parDistance1 = computePointToPointDistance(s1.getEndPoint(), vectorToSpatialCoord(projectionVector));
        }
        perDistance2 = computePointToSegmentDistance(s2.getEndPoint(), s1);
        if (coefficient < 0.5) {
            parDistance2 = computePointToPointDistance(s1.getStartPoint(), vectorToSpatialCoord(projectionVector));
        } else {
            parDistance2 = computePointToPointDistance(s1.getEndPoint(), vectorToSpatialCoord(projectionVector));
        }

        // perpendicular distance: (d1^2 + d2^2) / (d1 + d2)
        if (!(perDistance1 == 0.0 && perDistance2 == 0.0)) {
            perDistance = ((Math.pow(perDistance1, 2) + Math.pow(perDistance2, 2)) / (perDistance1 + perDistance2));
        } else {
            perDistance = 0.0;
        }

        // parallel distance: min(d1, d2)
        parDistance = Math.min(parDistance1, parDistance2);

        // Angle Distance
        angleDistance = computeAngleDistance(s1, s2);
        return (perDistance + parDistance + angleDistance);
    }

    /**
     * convert distance in meter to degree
     */
    public static double getDegreeFromM(double distanceInM) {
        double EARTH_RADIUS_IN_METER = 6378137.0;
        double perimeter = 2 * Math.PI * EARTH_RADIUS_IN_METER;
        double degreePerM = 360 / perimeter;
        return distanceInM * degreePerM;
    }

    /**
     * compute distance from point to segment
     */
    private static double computePointToSegmentDistance(Coordinate p, TrajectorySegment s) {
        Coordinate p1 = s.getStartPoint();
        Coordinate p2 = s.getEndPoint();
        for (int i = 0; i < POINT_DIM; i++) {
            vector1[i] = getValue(i, p) - getValue(i, p1);
            vector2[i] = getValue(i, p2) - getValue(i, p1);
        }
        coefficient = computeInnerProduct(vector1, vector2) / computeInnerProduct(vector2, vector2);
        for (int i = 0; i < POINT_DIM; i++) {
            projectionVector[i] = getValue(i, p1) + coefficient * vector2[i];
        }
        return computePointToPointDistance(p, vectorToSpatialCoord(projectionVector));
    }

    private static double getValue(int index, Coordinate coordinate) {
        if (index == 0) {
            return coordinate.getX();
        } else {
            return coordinate.getY();
        }
    }

    private static Coordinate vectorToSpatialCoord(double[] vector) {
        return new Coordinate(vector[0], vector[1]);
    }
}
