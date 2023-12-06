package com.chronomon.analysis.trajectory.compress;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

/**
 * TrajectoryCompress
 *
 * @author yuzisheng
 * @date 2023-11-07
 * @see DouglasPeuckerSimplifier
 */
public class TrajectoryCompress {
    /**
     * 基于道格拉斯扑克的轨迹几何压缩
     *
     * @param lineString        轨迹几何
     * @param distanceTolerance 距离阈值
     * @return 简化轨迹几何
     * @see DouglasPeuckerSimplifier#simplify(Geometry, double)
     */
    public static LineString compress(LineString lineString, double distanceTolerance) {
        return (LineString) DouglasPeuckerSimplifier.simplify(lineString, distanceTolerance);
    }

    public static void main(String[] args) throws Exception {
        WKTReader wktReader = new WKTReader();
        LineString lineString = (LineString) wktReader.read("LINESTRING(10 60,12 46,15 36,23 29,28 34,30 46,30 56,27 66,26 76,30 85,34 87,40 78,42 72,43 60,44 51,46 38,46 26,49 17,58 11,63 24,61 38,60 55,59 67,58 82,64 90,68 91,79 89,84 82,85 70,83 60,80 50,79 36,82 26,86 20,94 17,113 15,127 19,133 28,134 40,134 53,131 62,124 72,116 76,105 77,99 73,94 63,95 50,102 41,111 34,123 42,122 53,116 61,109 64,106 58)");
        System.out.println(compress(lineString, 15.0)); // 54 points to 12 points
    }
}
