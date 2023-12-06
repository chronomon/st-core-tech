package com.chronomon.storage.model;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;

/**
 * 几何模型
 *
 * @author yuzisheng
 * @date 2023-11-04
 */
public class GeometryModel {
    /**
     * 获取几何WKT
     */
    public static String toWkt(Geometry geometry) {
        return geometry.toText();
    }

    /**
     * 获取几何WKB
     */
    public static String toWkb(Geometry geometry) {
        WKBWriter wkbWriter = new WKBWriter();
        // 将字节数组转为十六进制字符串
        return WKBWriter.toHex(wkbWriter.write(geometry));
    }

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

    public static void main(String[] args) throws Exception {
        // 示例一：WKT、WKB
        WKTReader wktReader = new WKTReader();
        Point point = (Point) wktReader.read("POINT (1 1)");
        System.out.println("WKT: " + toWkt(point));  // POINT (1 1)
        System.out.println("WKB: " + toWkb(point));  // 00000000013FF00000000000003FF0000000000000

        // 示例二：简单性、合法性
        LineString notSimpleLine = (LineString) wktReader.read("LINESTRING (0 0,1 1,1 0,0 1)");
        System.out.println("simplicity: " + isSimple(notSimpleLine));  // false
        Polygon notValidPolygon = (Polygon) wktReader.read("POLYGON((0 0,0 2,2 2,2 0,0 0), (0 0,0 1,1 1,1 0,0 0))");
        System.out.println("validity: " + isValid(notValidPolygon));  // false
    }
}
