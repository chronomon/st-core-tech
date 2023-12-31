package com.chronomon.analysis.trajectory.model;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * 默认工具类
 *
 * @author wangrubin
 * @date 2023-11-05
 */
public class DefaultUtil {
    /**
     * 默认的几何工厂：默认精度，WGS84坐标系
     */
    public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

}
