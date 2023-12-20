package com.chronomo.services.util;

import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


/**
 * GIS计算通用工具类
 * 坐标系常数
 * 坐标系转换
 *
 * @author wangxu649
 * @date 20190818
 */
@Slf4j
public class CrsUtil {
    /**
     * web墨卡托坐标系下的地球半径，单位：米
     */
    public static final int EARTH_RADIUS3857 = 6378137;

    /**
     * 卡拉索夫斯基椭球半径，单位：米
     */
    public static final int EARTH_KRASOVSKY_RADIUS = 6378245;

    /**
     * 卡拉索夫斯基椭球偏心率 f = 1/298.3; e^2 = 2*f - f**2
     */
    public static final double EARTH_KRASOVSKY_EE = 0.00669342162296594323;

    /**
     * 百度坐标系经度偏移量
     */
    public static final double BD_DLON = 0.0065;

    /**
     * 百度坐标系纬度偏移量
     */
    public static final double BD_DLAT = 0.0060;

    /**
     * 国测局地球平均半径
     */
    public static final double EARTH_MEAN_RADIUS = 6371000;


    /***
     * 中国最北部纬度
     */
    public static final double CHINA_NORTH_LAT = 55.8271;
    /**
     * 中国最南部纬度
     */
    public static final double CHINA_SOUTH_LAT = 0.8293;
    /**
     * 中国最西部经度
     */
    public static final double CHINA_WEST_LON = 72.004;
    /**
     * 中国最东部经度
     */
    public static final double CHINA_EAST_LON = 137.8347;

    /**
     * web墨卡托坐标系椭球(正球)参数
     */
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;

    /**
     * 几何工厂类
     */
    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

    /**
     * web mector坐标系
     */
    public static final String WEB_MERCATOR = "EPSG:3857";

    /**
     * 国家2000坐标系
     */
    public static final String CGCS2000 = "EPSG:4490";
    /**
     * wgs84坐标系
     */
    public static final String WGS84 = "EPSG:4326";

    /**
     * 在epsg中未知的坐标系
     */
    public static final String CRS_UNKNOWN = "Unknown";

    /**
     * EPSG:null
     */
    public static final String EPSG_NULL = "EPSG:null";

    /**
     * geotools的wgs84经纬度坐标系
     */
    public static CoordinateReferenceSystem CRS_WGS84;

    /**
     * geotools的web mercator坐标系
     */
    public static CoordinateReferenceSystem CRS_WEBMERCATOR;

    static {
        //坐标规范默认经度在前,纬度在后
        System.setProperty("org.geotools.referencing.forceXY", "true");
        Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        try {
            CRS_WGS84 = CRS.decode("EPSG:4326");
            CRS_WEBMERCATOR = CRS.decode("EPSG:3857");
        } catch (FactoryException e) {
            log.error("GeoTools找不到EPSG:4326和EPSG:3857，请检查是否引用gt-epsg包");
        }
    }

    /**
     * 地理范围坐标转换
     *
     * @param env           经纬度地理范围
     * @param sourceCrcCode 源坐标系EPSG
     * @param targetCrsCode 要转换到的目标坐标系EPSG
     * @return 转换后的边界
     */
    public static ReferencedEnvelope envTransform(ReferencedEnvelope env, String sourceCrcCode, String targetCrsCode) {
        ReferencedEnvelope tileEnv = null;
        if (sourceCrcCode.equals(targetCrsCode)) {
            return env;
        }
        try {
            CoordinateReferenceSystem sourceCrs = CRS.decode(sourceCrcCode);
            CoordinateReferenceSystem targetCrs = CRS.decode(targetCrsCode);
            MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
            Envelope newEnv = JTS.transform(env, transform);
            tileEnv = new ReferencedEnvelope(newEnv, targetCrs);
        } catch (Exception e) {
            log.error("bbox坐标系转换出错;" + e);
        }
        return tileEnv;
    }

}
