package com.chronomon.storage.model;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

/**
 * 几何验证
 *
 * @author yuzisheng
 * @date 2023-12-07
 */
public class GeometryValidation {
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
        // 示例：简单性、合法性
        WKTReader wktReader = new WKTReader();
        LineString notSimpleLine = (LineString) wktReader.read("LINESTRING (0 0,1 1,1 0,0 1)");
        System.out.println("is simple: " + isSimple(notSimpleLine));  // false
        Polygon notValidPolygon = (Polygon) wktReader.read("POLYGON((0 0,0 2,2 2,2 0,0 0), (0 0,0 1,1 1,1 0,0 0))");
        System.out.println("is valid: " + isValid(notValidPolygon));  // false
    }
}
