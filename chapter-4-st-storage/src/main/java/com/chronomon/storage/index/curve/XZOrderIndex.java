package com.chronomon.storage.index.curve;

import org.geotools.geometry.jts.JTS;
import org.locationtech.geomesa.curve.XZ2SFC;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.sfcurve.IndexRange;
import scala.Tuple2;
import scala.Tuple4;
import scala.collection.JavaConversions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * XZOrderIndex
 *
 * @author yuzisheng
 * @date 2023-11-05
 */
public class XZOrderIndex {

    /**
     * @see XZ2SFC
     */
    private final XZ2SFC xz2SFC;

    /**
     * key-value pair storage in format of < xz-order value, point >
     * <p>
     * TreeMap is a key-sorted map to simulate the sequential arrangement of index values in memory
     */
    private TreeMap<Long, List<Geometry>> indexMap;

    XZOrderIndex(int precision) {
        xz2SFC = new XZ2SFC((short) precision, new Tuple2<>(-180.0, 180.0), new Tuple2<>(-90.0, 90.0));
    }

    XZOrderIndex(double minX, double maxX, double minY, double maxY, int precision) {
        xz2SFC = new XZ2SFC((short) precision, new Tuple2<>(minX, maxX), new Tuple2<>(minY, maxY));
    }

    /**
     * @see XZ2SFC#index(Tuple4)
     */
    public long index(Geometry geometry) {
        Envelope envelope = geometry.getEnvelopeInternal();
        return xz2SFC.index(new Tuple4<>(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()));
    }

    /**
     * build index based on xz-order curve for geometries
     */
    public void build(List<Geometry> geometries) {
        indexMap = new TreeMap<>();
        for (Geometry geometry : geometries) {
            long index = index(geometry);
            if (!indexMap.containsKey(index)) {
                indexMap.put(index, new ArrayList<>());
            }
            indexMap.get(index).add(geometry);
        }
    }

    /**
     * query points by a given range
     *
     * @param contained true to select contained geometry and false to select intersected geometry
     */
    public List<Geometry> query(double minX, double maxX, double minY, double maxY, boolean contained) {
        Polygon envelope = JTS.toGeometry(new Envelope(minX, maxX, minY, maxY));
        List<IndexRange> ranges = JavaConversions.seqAsJavaList(xz2SFC.ranges(minX, minY, maxX, maxY));

        List<Geometry> results = new ArrayList<>();
        for (IndexRange range : ranges) {
            NavigableMap<Long, List<Geometry>> subMap = indexMap.subMap(range.lower(), true, range.upper(), true);
            for (List<Geometry> candidates : subMap.values()) {
                for (Geometry candidate : candidates) {
                    if (contained) {
                        if (envelope.contains(candidate)) {
                            results.add(candidate);
                        }
                    } else {
                        if (envelope.intersects(candidate)) {
                            results.add(candidate);
                        }
                    }
                }
            }
        }
        return results;
    }

    public static void main(String[] args) throws Exception {
        // 示例一：计算单个Geometry的XZ-Order索引值，可指定不同阶数
        WKTReader wktReader = new WKTReader();
        XZOrderIndex xz1 = new XZOrderIndex(1);
        System.out.println(xz1.index(wktReader.read("POINT (-180.0 -90.0)")));  // 1
        System.out.println(xz1.index(wktReader.read("POINT (180.0 90.0)")));  // 4

        XZOrderIndex xz2 = new XZOrderIndex(2);
        System.out.println(xz2.index(wktReader.read("POINT (-180.0 -90.0)")));  // 2
        System.out.println(xz2.index(wktReader.read("POINT (180.0 90.0)")));  // 20

        // 示例二：指定阶数，创建多个Geometry的XZ-Order索引，输入查询范围，输出查询结果
        XZOrderIndex xz16 = new XZOrderIndex(-180, 180, -90, 90, 31);
        String filePath = Objects.requireNonNull(XZOrderIndex.class.getResource("/polygons.txt")).getPath();
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<Geometry> polygons = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            polygons.add(wktReader.read(line));
        }
        xz16.build(polygons);

        long startTime = System.currentTimeMillis();
        List<Geometry> query = xz16.query(116.36236773134938005, 116.37030397581409602, 39.92034877315298047, 39.92787803072206287, false);
        long endTime = System.currentTimeMillis();
        System.out.println("查询结果数量：" + query.size() + "，查询耗时毫秒：" + (endTime - startTime) + "ms");  // 12
    }
}
