package com.chronomon.analysis.trajectory.model;

import org.locationtech.jts.geom.Coordinate;

/**
 * 坐标系转换工具类
 *
 * @author yuzisheng
 * @date 2023-11-01
 */
public class CoordinateUtil {
    /**
     * 坐标转换参数：火星坐标系与百度坐标系转换的中间量
     */
    public static final double X_PI = 3.1415926535897932384626433832795 * 3000.0 / 180.0;
    /**
     * 坐标转换参数：π
     */
    public static final double PI = 3.1415926535897932384626433832795D;
    /**
     * 地球半径（Krasovsky 1940）
     */
    public static final double RADIUS = 6378245.0D;
    /**
     * 修正参数（偏率ee）
     */
    public static final double CORRECTION_PARAM = 0.00669342162296594323D;

    /**
     * GCJ02转WGS84
     *
     * @param lng 经度
     * @param lat 纬度
     * @return WGS84坐标
     */
    public static Coordinate gcj02ToWgs84(double lng, double lat) {
        Coordinate coordInGcj02 = new Coordinate(lng, lat);
        Coordinate temp = addOffset(coordInGcj02, calOffset(lng, lat, false));
        return addOffset(coordInGcj02, calOffset(temp.getX(), temp.getY(), false));
    }


    /**
     * WGS84与GCJ02转换的偏移算法
     *
     * @param lng    经度
     * @param lat    纬度
     * @param isPlus 是否正向偏移：WGS84转GCJ02使用正向，否则使用反向
     * @return 偏移坐标
     */
    private static double[] calOffset(double lng, double lat, boolean isPlus) {
        double dlng = transLng(lng - 105.0, lat - 35.0);
        double dlat = transLat(lng - 105.0, lat - 35.0);

        double magic = Math.sin(lat / 180.0 * PI);
        magic = 1 - CORRECTION_PARAM * magic * magic;
        final double sqrtMagic = Math.sqrt(magic);

        dlng = (dlng * 180.0) / (RADIUS / sqrtMagic * Math.cos(lat / 180.0 * PI) * PI);
        dlat = (dlat * 180.0) / ((RADIUS * (1 - CORRECTION_PARAM)) / (magic * sqrtMagic) * PI);

        if (!isPlus) {
            dlng = -dlng;
            dlat = -dlat;
        }
        return new double[]{dlng, dlat};
    }

    /**
     * 坐标偏移
     *
     * @param rawCoord 原始坐标
     * @param offset   坐标偏移量
     * @return 偏移后的坐标
     */
    private static Coordinate addOffset(Coordinate rawCoord, double[] offset) {
        return new Coordinate(rawCoord.getX() + offset[0], rawCoord.getY() + offset[1]);
    }

    /**
     * 计算经度坐标
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 计算完成后的经度
     */
    private static double transLng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 计算纬度坐标
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 计算完成后的纬度
     */
    private static double transLat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat
                + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }
}
