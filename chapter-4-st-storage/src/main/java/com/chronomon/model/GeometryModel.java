package com.chronomon.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.util.Arrays;

/**
 * 几何模型
 *
 * @author yuzisheng
 * @date 2023-11-04
 */
public class GeometryModel {
    /**
     * 几何简单性验证
     *
     * @see Geometry#isSimple()
     */
    public static boolean isSimple(Geometry geometry) {
        return geometry.isSimple();
    }

    /**
     * 几何合法性验证
     *
     * @see Geometry#isValid()
     */
    public static boolean isValid(Geometry geometry) {
        return geometry.isValid();
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
    public static String relate(Geometry geometry1, Geometry geometry2) {
        return geometry1.relate(geometry2).toString();
    }

    public static void main(String[] args) throws Exception {
        WKTReader wktReader = new WKTReader();

        LineString notSimpleLine = (LineString) wktReader.read("LINESTRING (0 0,1 1,1 0,0 1)");
        System.out.println(isSimple(notSimpleLine));  // false

        Polygon notValidPolygon = (Polygon) wktReader.read("POLYGON((0 0,0 2,2 2,2 0,0 0), (0 0,0 1,1 1,1 0,0 0))");
        System.out.println(isValid(notValidPolygon));  // false

        LineString line1 = (LineString) wktReader.read("LINESTRING (0 0,1 1)");
        LineString line2 = (LineString) wktReader.read("LINESTRING (0 1,1 0)");
        System.out.println(intersects(line1, line2));  // true
        System.out.println(distance(line1, line2));  // 0.0
        System.out.println(Arrays.toString(nearest(line1, line2)));  // [(0.5,0.5),(0.5,0.5)]

        // 湖泊与第一个码头的九交模型计算
        Polygon lake = (Polygon) wktReader.read("POLYGON((0 0,0 2,2 2,2 0,0 0))");
        LineString dock = (LineString) wktReader.read("LINESTRING (0 1,1 1)");
        System.out.println(relate(lake, dock));  // 102F01FF2
    }
}
