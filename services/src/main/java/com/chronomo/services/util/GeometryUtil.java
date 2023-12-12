package com.chronomo.services.util;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * 几何工具类
 *
 * @author yuzisheng
 * @date 2023-03-24
 */
public class GeometryUtil {
    /**
     * 默认坐标系
     */
    private static CoordinateReferenceSystem defaultCRS;

    static {
        try {
            defaultCRS = CRS.decode("EPSG:4326", true);
        } catch (Exception ignored) {
        }
    }

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public static Point createPoint(Double lng, Double lat) {
        return FACTORY.createPoint(new Coordinate(lng, lat));
    }

    /**
     * 将基于3857的单位米转为基于4326的单位度
     */
    public static double meterToDegree(Double distanceInMeter) {
        return DistanceUtils.dist2Degrees(distanceInMeter, DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM * 1000.0);
    }

    /**
     * 将基于4326的单位度转为基于3857的单位米
     */
    public static double degreeToMeter(Double degree) {
        return DistanceUtils.degrees2Dist(degree, DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM * 1000.0);
    }

    /**
     * 计算两个空间对象之间的距离，单位为米
     */
    public static Double calculateDistanceInMeter(Geometry g1, Geometry g2) {
        if (g1 == null || g2 == null || g1.isEmpty() || g2.isEmpty()) {
            return null;
        }

        Double distanceInMeter = null;
        try {
            DistanceOp distanceOp = new DistanceOp(g1, g2);
            Coordinate[] coordinates = distanceOp.nearestPoints();
            if (coordinates.length < 2) {
                return null;
            }
            distanceInMeter = JTS.orthodromicDistance(coordinates[0], coordinates[1], defaultCRS);
        } catch (Exception ignored) {
        }
        return distanceInMeter;
    }
}
