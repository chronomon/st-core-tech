package com.chronomon.storage.index.tree;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.kdtree.KdTree;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * KdTreeIndex
 *
 * @author yuzisheng
 * @date 2023-11-04
 */
public class KdTreeIndex {

    /**
     * @see KdTree
     */
    private final KdTree kdTree;

    KdTreeIndex(List<Coordinate> coordinates) {
        kdTree = new KdTree();
        for (Coordinate coordinate : coordinates) {
            kdTree.insert(coordinate, coordinate);
        }
    }

    public List query(double minX, double maxX, double minY, double maxY) {
        Envelope envelope = new Envelope(minX, maxX, minY, maxY);
        return kdTree.query(envelope);
    }

    public static void main(String[] args) throws Exception {
        String filePath = Objects.requireNonNull(KdTreeIndex.class.getResource("/points.txt")).getPath();
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<Coordinate> points = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] items = line.split(",");
            points.add(new Coordinate(Double.parseDouble(items[0]), Double.parseDouble(items[1])));
        }

        long startTime = System.currentTimeMillis();
        KdTreeIndex kdTreeIndex = new KdTreeIndex(points);
        List query = kdTreeIndex.query(116.392137, 116.401321, 39.913083, 39.922957);
        long endTime = System.currentTimeMillis();
        System.out.println("查询结果数量：" + query.size() + "，查询耗时毫秒：" + (endTime - startTime) + "ms");
    }
}
