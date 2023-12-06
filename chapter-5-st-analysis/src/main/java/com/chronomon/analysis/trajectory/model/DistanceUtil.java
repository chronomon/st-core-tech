package com.chronomon.analysis.trajectory.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.distance.GeodesicSphereDistCalc;
import org.locationtech.spatial4j.shape.impl.PointImpl;
import org.locationtech.spatial4j.shape.jts.JtsPoint;

/**
 * 距离计算工具
 */
public class DistanceUtil {

    /**
     * 计算两点间的球面距离
     *
     * @param fromX 起点经度
     * @param fromY 起点纬度
     * @param toX   终点经度
     * @param toY   终点纬度
     * @return 球面距离（米）
     */
    public static double distInMeter(double fromX, double fromY, double toX, double toY) {
        double degrees = new GeodesicSphereDistCalc.LawOfCosines().distance(new PointImpl(fromX, fromY, JtsSpatialContext.GEO), new PointImpl(toX, toY, JtsSpatialContext.GEO));
        return DistanceUtils.degrees2Dist(degrees, DistanceUtils.EARTH_MEAN_RADIUS_KM) * 1000;
    }

    /**
     * 计算两点间的球面距离
     *
     * @param from 起点
     * @param to   终点
     * @return 球面距离（米）
     */
    public static double distInMeter(Point from, Point to) {
        double degrees = new GeodesicSphereDistCalc.LawOfCosines().distance(new JtsPoint(from, JtsSpatialContext.GEO), new JtsPoint(to, JtsSpatialContext.GEO));
        return DistanceUtils.degrees2Dist(degrees, DistanceUtils.EARTH_MEAN_RADIUS_KM) * 1000;
    }

    /**
     * 获取度数表示的扩展MBR
     *
     * @param centre        中心点
     * @param expandDistInM 扩展距离(米)
     * @return 扩展MBR
     */
    public static Envelope expandEnv(Point centre, double expandDistInM) {
        double latDegrees = DistanceUtils.dist2Degrees(expandDistInM / 1000, DistanceUtils.EARTH_MEAN_RADIUS_KM);
        double lonDegrees = DistanceUtils.calcLonDegreesAtLat(centre.getY(), latDegrees);
        Envelope env = new Envelope(centre.getCoordinate());
        env.expandBy(lonDegrees, latDegrees);
        return env;
    }

    /**
     * 测试距离计算函数
     */
    public static void main(String[] args) {
        // 赤道上的距离
        Point from = DefaultUtil.GEOMETRY_FACTORY.createPoint(new Coordinate(110.0, 0));
        Point to = DefaultUtil.GEOMETRY_FACTORY.createPoint(new Coordinate(120.0, 0));
        System.out.println(distInMeter(from, to));

        // 北纬60度上的距离
        from = DefaultUtil.GEOMETRY_FACTORY.createPoint(new Coordinate(110.0, 60.0));
        to = DefaultUtil.GEOMETRY_FACTORY.createPoint(new Coordinate(120.0, 60.0));
        System.out.println(distInMeter(from, to));

        // 北纬60度上扩展的MBR
        Point centre = DefaultUtil.GEOMETRY_FACTORY.createPoint(new Coordinate(0.0, 60.0));
        System.out.println(DistanceUtil.expandEnv(centre, 100.0));
    }
}
