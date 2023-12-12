package com.chronomo.services.util;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

/**
 * WKT工具类
 *
 * @author wangrubin1
 * @date 2022-08-23
 */
@Slf4j
public class WKTUtil {
    private static final ThreadLocal<WKTWriter> WRITER_POOL = ThreadLocal.withInitial(WKTWriter::new);
    private static final ThreadLocal<WKTReader> READER_POOL = ThreadLocal.withInitial(WKTReader::new);

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Geometry转WKT
     *
     * @param geom 几何对象
     * @return WKT
     */
    public static String toWKT(Geometry geom) {
        return WRITER_POOL.get().write(geom);
    }

    /**
     * WKT转Geometry
     *
     * @param geomWKT WKT格式的几何对象
     * @return Geometry对象
     */
    public static <T extends Geometry> T toGeom(String geomWKT) {
        try {
            return (T) READER_POOL.get().read(geomWKT);
        } catch (Exception e) {
            log.error("解析WKT失败：" + geomWKT, e);
        }
        return null;
    }

    /**
     * 创建空间点
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 点
     */
    public static Point createPoint(Double lng, Double lat) {
        return FACTORY.createPoint(new Coordinate(lng, lat));
    }
}
