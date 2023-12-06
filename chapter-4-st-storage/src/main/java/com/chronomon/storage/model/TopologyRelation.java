package com.chronomon.storage.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.util.Arrays;

/**
 * 空间拓扑关系
 *
 * @author yuzisheng
 * @date 2023-12-06
 */
public class TopologyRelation {
    /**
     * 判断两个几何是否相离
     *
     * @see Geometry#disjoint(Geometry)
     */
    public static boolean disjoint(Geometry geometry1, Geometry geometry2) {
        return geometry1.disjoint(geometry2);
    }

    /**
     * 判断两个几何是否相切
     *
     * @see Geometry#touches(Geometry)
     */
    public static boolean touches(Geometry geometry1, Geometry geometry2) {
        return geometry1.touches(geometry2);
    }

    /**
     * 判断两个几何是否相交
     *
     * @see Geometry#intersects(Geometry)
     */
    public static boolean intersects(Geometry geometry1, Geometry geometry2) {
        return geometry1.intersects(geometry2);
    }

    /**
     * 判断第一个几何是否包含第二个几何
     *
     * @see Geometry#contains(Geometry)
     */
    public static boolean contains(Geometry geometry1, Geometry geometry2) {
        return geometry1.contains(geometry2);
    }

    /**
     * 计算两个几何之间距离
     *
     * @see Geometry#distance(Geometry)
     */
    public static double distance(Geometry geometry1, Geometry geometry2) {
        return geometry1.distance(geometry2);
    }

    /**
     * 计算两个几何之间最近点
     *
     * @see DistanceOp#nearestPoints()
     */
    public static Coordinate[] nearest(Geometry geometry1, Geometry geometry2) {
        DistanceOp distanceOp = new DistanceOp(geometry1, geometry2);
        return distanceOp.nearestPoints();
    }

    /**
     * 九交模型计算
     *
     * @see Geometry#relate(Geometry)
     */
    public static String de9im(Geometry geometry1, Geometry geometry2) {
        return geometry1.relate(geometry2).toString();
    }

    public static void main(String[] args) throws Exception {
        // 示例一：相离、相切、相交、包含
        WKTReader wktReader = new WKTReader();
        Polygon polygon = (Polygon) wktReader.read("POLYGON((0 0,0 2,2 2,2 0,0 0))");
        Polygon polygon1 = (Polygon) wktReader.read("POLYGON((-1 0,-1 1,-2 1,-2 0,-1 0))");
        System.out.println("is disjoint: " + disjoint(polygon1, polygon));  // true
        Polygon polygon2 = (Polygon) wktReader.read("POLYGON((0 0,0 1,-1 1,-1 0,0 0))");
        System.out.println("is touches: " + touches(polygon2, polygon));  // true
        Polygon polygon3 = (Polygon) wktReader.read("POLYGON((0.5 0,0.5 1,-0.5 1,-0.5 0,0.5 0))");
        System.out.println("is intersects: " + intersects(polygon3, polygon));  // true
        Polygon polygon4 = (Polygon) wktReader.read("POLYGON((1.5 0,1.5 1,0.5 1,0.5 0,1.5 0))");
        System.out.println("is polygon contains polygon4: " + contains(polygon, polygon4));  // true

        // 示例三：距离、最近点对
        LineString line1 = (LineString) wktReader.read("LINESTRING (0 0,1 1)");
        LineString line2 = (LineString) wktReader.read("LINESTRING (0 1,1 0)");
        System.out.println("the distance between line1 and line2: " + distance(line1, line2));  // 0.0
        System.out.println("the closest point pair on line1 and line2" + Arrays.toString(nearest(line1, line2)));  // [(0.5,0.5),(0.5,0.5)]

        // 示例三：九交模型，以书中湖泊和第一个码头举例
        Polygon lake = (Polygon) wktReader.read("POLYGON((0 0,0 2,2 2,2 0,0 0))");
        LineString dock1 = (LineString) wktReader.read("LINESTRING (0 1,1 1)");
        System.out.println("the de9im between lake and dock1 in the book:" + de9im(lake, dock1));  // 102F01FF2
    }
}
