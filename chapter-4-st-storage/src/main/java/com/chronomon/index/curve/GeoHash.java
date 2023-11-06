package com.chronomon.index.curve;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Point;

/**
 * GeoHash
 *
 * @author yuzisheng
 * @date 2023-11-04
 */
public class GeoHash {

    private static final SpatialContext ctx = new SpatialContext(new SpatialContextFactory());

    /**
     * GeoHash编码
     *
     * @see GeohashUtils#encodeLatLon(double, double, int)
     */
    public static String encode(double longitude, double latitude, int precision) {
        return GeohashUtils.encodeLatLon(latitude, longitude, precision);
    }

    /**
     * GeoHash解码
     *
     * @see GeohashUtils#decode(String, SpatialContext)
     */
    public static Coordinate decode(String geohash) {
        Point point = GeohashUtils.decode(geohash, ctx);
        return new Coordinate(point.getX(), point.getY());
    }

    public static void main(String[] args) {
        System.out.println(encode(116.562895, 39.786652, 7));  // wx4fk6y
        System.out.println(decode("wx4fk6y"));  // 116.56288146972656,39.78630065917969
    }
}
