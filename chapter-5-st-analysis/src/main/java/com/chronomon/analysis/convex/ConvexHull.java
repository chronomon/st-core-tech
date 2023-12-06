package com.chronomon.analysis.convex;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 凸包计算
 *
 * @author yuzisheng
 * @date 2023-11-07
 */
public class ConvexHull {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * 最小凸包计算
     *
     * @param coordinates 点集
     * @return 最小凸包
     * @see Geometry#convexHull()
     */
    public static Polygon getConvexHull(List<Coordinate> coordinates) {
        if (coordinates.size() < 3) {
            return null;
        }

        MultiPoint multiPoint = GEOMETRY_FACTORY.createMultiPoint(new CoordinateArraySequence(coordinates.toArray(new Coordinate[0])));
        return (Polygon) multiPoint.convexHull();
    }

    public static void main(String[] args) {
        List<Coordinate> coordinates = new ArrayList<>();
        coordinates.add(new Coordinate(0, 0));
        coordinates.add(new Coordinate(1, 2));
        coordinates.add(new Coordinate(2, 2));
        coordinates.add(new Coordinate(2, 1));
        coordinates.add(new Coordinate(1.5, 1.5));

        System.out.println(getConvexHull(coordinates));  // POLYGON ((0 0, 1 2, 2 2, 2 1, 0 0))
    }
}
