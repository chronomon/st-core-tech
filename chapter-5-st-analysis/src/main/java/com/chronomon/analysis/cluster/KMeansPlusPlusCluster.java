package com.chronomon.analysis.cluster;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * KMeans++聚类
 *
 * @author yuzisheng
 * @date 2023-11-05
 * @see <a href="http://en.wikipedia.org/wiki/K-means%2B%2B">K-means++ (wikipedia)</a>
 */
public class KMeansPlusPlusCluster {
    /**
     * 聚类算法模型
     *
     * @see KMeansPlusPlusClusterer
     */
    private final KMeansPlusPlusClusterer<DoublePoint> kMeansPlusPlus;

    /**
     * @param clusterNum 聚类个数
     */
    KMeansPlusPlusCluster(int clusterNum) {
        this.kMeansPlusPlus = new KMeansPlusPlusClusterer(clusterNum);
    }

    /**
     * 执行聚类
     *
     * @param points 空间点集合
     * @return 聚类簇集合
     * @see KMeansPlusPlusClusterer#cluster(Collection)
     */
    public List<CentroidCluster<DoublePoint>> doCluster(List<DoublePoint> points) {
        return kMeansPlusPlus.cluster(points);
    }

    public static void main(String[] args) throws Exception {
        String filePath = Objects.requireNonNull(KMeansPlusPlusCluster.class.getResource("/kmeans.txt")).getPath();
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<DoublePoint> points = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] items = line.split(",");
            points.add(new DoublePoint(new double[]{Double.parseDouble(items[0]), Double.parseDouble(items[1])}));
        }

        KMeansPlusPlusCluster kmeans = new KMeansPlusPlusCluster(2);  // two clusters
        List<CentroidCluster<DoublePoint>> clusters = kmeans.doCluster(points);
        int clusterId = 1;
        for (CentroidCluster<DoublePoint> cluster : clusters) {
            for (DoublePoint point : cluster.getPoints()) {
                System.out.println(point.getPoint()[0] + "," + point.getPoint()[1] + "," + clusterId);
            }
            clusterId++;
        }
    }
}
