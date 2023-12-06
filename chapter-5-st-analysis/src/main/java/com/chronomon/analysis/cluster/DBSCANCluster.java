package com.chronomon.analysis.cluster;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * DBSCAN聚类
 *
 * @author yuzisheng
 * @date 2023-11-05
 * @see <a href="http://en.wikipedia.org/wiki/DBSCAN">DBSCAN (wikipedia)</a>
 */
public class DBSCANCluster {
    /**
     * 聚类算法模型
     *
     * @see DBSCANClusterer
     */
    private final DBSCANClusterer<DoublePoint> dbscan;

    /**
     * @param eps    邻域半径
     * @param minPts 聚类簇所需最小点数
     */
    DBSCANCluster(double eps, int minPts) {
        dbscan = new DBSCANClusterer(eps, minPts);
    }

    /**
     * 执行聚类
     *
     * @param points 空间点集合
     * @return 聚类簇集合
     * @see DBSCANClusterer#cluster(Collection)
     */
    public List<Cluster<DoublePoint>> doCluster(List<DoublePoint> points) {
        return dbscan.cluster(points);
    }

    public static void main(String[] args) throws Exception {
        String filePath = Objects.requireNonNull(KMeansPlusPlusCluster.class.getResource("/dbscan.txt")).getPath();
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<DoublePoint> points = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] items = line.split(",");
            points.add(new DoublePoint(new double[]{Double.parseDouble(items[0]), Double.parseDouble(items[1])}));
        }

        DBSCANCluster dbscan = new DBSCANCluster(0.15, 7);  // two clusters in shape of circle
        List<Cluster<DoublePoint>> clusters = dbscan.doCluster(points);
        int clusterId = 1;
        for (Cluster<DoublePoint> cluster : clusters) {
            for (DoublePoint point : cluster.getPoints()) {
                System.out.println(point.getPoint()[0] + "," + point.getPoint()[1] + "," + clusterId);
            }
            clusterId++;
        }
    }
}
