package com.chronomon.storage.index.curve;

import org.locationtech.geomesa.curve.NormalizedDimension;
import org.locationtech.geomesa.curve.Z2SFC;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.sfcurve.IndexRange;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;

/**
 * ZOrderIndex
 *
 * @author yuzisheng
 * @date 2023-11-05
 */
public class ZOrderIndex {
    /**
     * @see Z2SFC
     */
    private final Z2SFC z2SFC;

    /**
     * key-value pair storage in format of < z-order value, point >
     * <p>
     * TreeMap is a key-sorted map to simulate the sequential arrangement of index values in memory
     */
    private TreeMap<Long, List<Coordinate>> indexMap;

    ZOrderIndex(int precision) {
        z2SFC = new Z2SFC(precision);
    }

    ZOrderIndex(double minX, double maxX, double minY, double maxY, int precision) throws Exception {
        z2SFC = new Z2SFC(precision);
        try {
            Field lon = z2SFC.getClass().getDeclaredField("lon");
            lon.setAccessible(true);
            lon.set(z2SFC, new NormalizedDimension.BitNormalizedDimension(minX, maxX, precision));

            Field lat = z2SFC.getClass().getDeclaredField("lat");
            lat.setAccessible(true);
            lat.set(z2SFC, new NormalizedDimension.BitNormalizedDimension(minY, maxY, precision));
        } catch (Exception ignored) {
            throw new Exception("Fail to change the ranges of x and y by reflection");
        }
    }

    /**
     * @see Z2SFC#index(double, double, boolean)
     */
    public long index(double x, double y) {
        return z2SFC.index(x, y, false);
    }

    /**
     * build index based on z-order curve for points
     */
    public void build(List<Coordinate> coordinates) {
        indexMap = new TreeMap<>();
        for (Coordinate coordinate : coordinates) {
            long index = index(coordinate.getX(), coordinate.getY());
            if (!indexMap.containsKey(index)) {
                indexMap.put(index, new ArrayList<>());
            }
            indexMap.get(index).add(coordinate);
        }
    }

    /**
     * query points by a given range
     */
    public List<Coordinate> query(double minX, double maxX, double minY, double maxY) {
        Envelope envelope = new Envelope(minX, maxX, minY, maxY);
        List<IndexRange> ranges = JavaConversions.seqAsJavaList(z2SFC.ranges(new Tuple2<>(minX, maxX), new Tuple2<>(minY, maxY)));

        List<Coordinate> results = new ArrayList<>();
        for (IndexRange range : ranges) {
            NavigableMap<Long, List<Coordinate>> subMap = indexMap.subMap(range.lower(), true, range.upper(), true);
            for (List<Coordinate> candidates : subMap.values()) {
                for (Coordinate candidate : candidates) {
                    if (envelope.contains(candidate)) {
                        results.add(candidate);
                    }
                }
            }
        }
        return results;
    }

    public static void main(String[] args) throws Exception {
        // 示例一：计算单个Point的Z-Order索引值，可指定不同阶数
        ZOrderIndex z1 = new ZOrderIndex(1);
        System.out.println(z1.index(-180, -90));  // 0
        System.out.println(z1.index(180, 90));  // 3

        ZOrderIndex z2 = new ZOrderIndex(2);
        System.out.println(z2.index(-180, -90));  // 0
        System.out.println(z2.index(180, 90));  // 15

        // 示例二：指定阶数，创建多个Point的Z-Order索引，输入查询范围，输出查询结果
        ZOrderIndex z16 = new ZOrderIndex(-180, 180, -90, 90, 16);
        String filePath = Objects.requireNonNull(ZOrderIndex.class.getResource("/points.txt")).getPath();
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<Coordinate> points = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] items = line.split(",");
            points.add(new Coordinate(Double.parseDouble(items[0]), Double.parseDouble(items[1])));
        }
        z16.build(points);

        long startTime = System.currentTimeMillis();
        List<Coordinate> query = z16.query(116.36236773134938005, 116.37030397581409602, 39.92034877315298047, 39.92787803072206287);
        long endTime = System.currentTimeMillis();
        System.out.println("查询结果数量：" + query.size() + "，查询耗时毫秒：" + (endTime - startTime) + "ms");
    }
}
