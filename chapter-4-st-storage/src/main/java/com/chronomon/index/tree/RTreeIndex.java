package com.chronomon.index.tree;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RTreeIndex
 *
 * @author yuzisheng
 * @date 2023-11-04
 */
public class RTreeIndex {

    /**
     * @see STRtree
     */
    private final STRtree rTree;

    private final double delta = 1e-7;

    RTreeIndex(List<Coordinate> coordinates) {
        rTree = new STRtree();
        for (Coordinate coordinate : coordinates) {
            Envelope envelope = new Envelope(coordinate);
            envelope.expandBy(delta, delta);
            rTree.insert(envelope, coordinate);
        }
        rTree.build();
    }

    public List query(double minX, double maxX, double minY, double maxY) {
        Envelope envelope = new Envelope(minX, maxX, minY, maxY);
        List<Coordinate> results = new ArrayList<>();
        for (Object o : rTree.query(envelope)) {
            if (envelope.contains((Coordinate) o)) {
                results.add((Coordinate) o);
            }
        }
        return results;
    }

    public static void main(String[] args) throws Exception {
        String filePath = Objects.requireNonNull(RTreeIndex.class.getResource("/points.txt")).getPath();
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
        RTreeIndex rTreeIndex = new RTreeIndex(points);
        List query = rTreeIndex.query(116.392137, 116.401321, 39.913083, 39.922957);
        long endTime = System.currentTimeMillis();
        System.out.println("查询结果数量：" + query.size() + "，查询耗时毫秒：" + (endTime - startTime) + "ms");
    }
}
