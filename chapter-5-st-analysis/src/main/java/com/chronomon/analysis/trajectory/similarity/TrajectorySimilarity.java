package com.chronomon.analysis.trajectory.similarity;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.WKTReader;

/**
 * 轨迹相似性
 *
 * @author yuzisheng
 * @date 2023-11-11
 */
public class TrajectorySimilarity {
    /**
     * 基于点的距离：欧式距离
     */
    public static double distanceInEuclidean(Coordinate[] traj1, Coordinate[] traj2) {
        if (traj1.length != traj2.length) {
            throw new RuntimeException("欧式距离的两条轨迹长度应该相同");
        }

        double distance = 0.0;
        int n = traj1.length;
        for (int i = 0; i < n; i++) {
            Coordinate p1 = traj1[i];
            Coordinate p2 = traj2[i];
            distance += distance(p1, p2);
        }
        return distance / n;
    }

    /**
     * 基于点的距离：动态时间规整距离
     */
    public static double distanceInDtw(Coordinate[] traj1, Coordinate[] traj2) {
        int n = traj1.length;
        int m = traj2.length;

        double[][] dp = new double[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) {
                dp[i][j] = Double.POSITIVE_INFINITY;
            }
        }

        dp[0][0] = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                double cost = distance(traj1[i - 1], traj2[j - 1]);
                dp[i][j] = cost + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
            }
        }
        return dp[n][m];
    }

    /**
     * 基于点的距离：最长公共子序列
     */
    public static int distanceInLcss(Coordinate[] traj1, Coordinate[] traj2, double distanceThreshold) {
        int n = traj1.length;
        int m = traj2.length;
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) {
                if (i == 0 || j == 0) {
                    dp[i][j] = 0;
                } else if (distance(traj1[i - 1], traj2[j - 1]) <= distanceThreshold) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[n][m];
    }

    /**
     * 基于点的距离：编辑距离
     */
    public static int distanceInEdit(Coordinate[] traj1, Coordinate[] traj2, double distanceThreshold) {
        int n = traj1.length;
        int m = traj2.length;

        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= n; i++) {
            Coordinate p1 = traj1[i - 1];
            for (int j = 1; j <= m; j++) {
                Coordinate p2 = traj2[j - 1];
                if (distance(p1, p2) <= distanceThreshold) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1] + 1, dp[i][j - 1] + 1), dp[i - 1][j] + 1);
                }
            }
        }
        return dp[n][m];
    }

    /**
     * 基于形状的距离：豪斯多夫距离
     */
    public static double distanceInHausdorff(Coordinate[] traj1, Coordinate[] traj2) {
        double distance12 = Double.MIN_VALUE;
        for (Coordinate p1 : traj1) {
            double temp = Double.POSITIVE_INFINITY;
            for (Coordinate p2 : traj2) {
                temp = Math.min(temp, distance(p1, p2));
            }
            distance12 = Math.max(distance12, temp);
        }

        double distance21 = Double.MIN_VALUE;
        for (Coordinate p2 : traj2) {
            double temp = Double.POSITIVE_INFINITY;
            for (Coordinate p1 : traj1) {
                temp = Math.min(temp, distance(p2, p1));
            }
            distance21 = Math.max(distance21, temp);
        }
        return Math.max(distance12, distance21);
    }

    /**
     * 基于形状的距离：弗雷歇距离
     */
    public static double distanceInFrechet(Coordinate[] traj1, Coordinate[] traj2) {
        double distance = Double.MIN_VALUE;
        for (Coordinate p1 : traj1) {
            for (Coordinate p2 : traj2) {
                distance = Math.max(distance, distance(p1, p2));
            }
        }
        return distance;
    }

    private static double distance(Coordinate p1, Coordinate p2) {
        return Math.sqrt((Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2)));
    }

    public static void main(String[] args) throws Exception {
        WKTReader wktReader = new WKTReader();

        LineString line1 = (LineString) wktReader.read("LINESTRING (0 0,1 0,2 0)");
        LineString line2 = (LineString) wktReader.read("LINESTRING (0 1,1 1,2 1)");
        System.out.println("欧式距离：" + distanceInEuclidean(line1.getCoordinates(), line2.getCoordinates()));  // 1.0

        LineString line3 = (LineString) wktReader.read("LINESTRING (0 0,1 0,2 0)");
        LineString line4 = (LineString) wktReader.read("LINESTRING (0 1,1 1,2 1)");
        System.out.println("动态时间规整距离：" + distanceInDtw(line3.getCoordinates(), line4.getCoordinates()));  // 3.0

        LineString line5 = (LineString) wktReader.read("LINESTRING (0 0,1 0.5,2 0)");
        LineString line6 = (LineString) wktReader.read("LINESTRING (0 1,2 1)");
        System.out.println("编辑距离一：" + distanceInEdit(line5.getCoordinates(), line6.getCoordinates(), 0.5));  // 3
        System.out.println("编辑距离二：" + distanceInEdit(line5.getCoordinates(), line6.getCoordinates(), 1.5));  // 1

        LineString line7 = (LineString) wktReader.read("LINESTRING (0 0,1 0.5,2 0)");
        LineString line8 = (LineString) wktReader.read("LINESTRING (0 1,1 1,2 1)");
        System.out.println("最长公共子序列距离一：" + distanceInLcss(line7.getCoordinates(), line8.getCoordinates(), 0.5));  // 1
        System.out.println("最长公共子序列距离二：" + distanceInLcss(line7.getCoordinates(), line8.getCoordinates(), 1.5));  // 3

        LineString line9 = (LineString) wktReader.read("LINESTRING (0 0,1 0.5,2 3)");
        LineString line10 = (LineString) wktReader.read("LINESTRING (0 1,1 1,2 1)");
        System.out.println("豪斯多夫距离：" + distanceInHausdorff(line9.getCoordinates(), line10.getCoordinates()));  // 2.0

        LineString line11 = (LineString) wktReader.read("LINESTRING (0 0,1 0,2 0,3 0)");
        LineString line12 = (LineString) wktReader.read("LINESTRING (0 4,1 1,2 1,3 1)");
        System.out.println("弗雷歇距离：" + distanceInFrechet(line11.getCoordinates(), line12.getCoordinates()));  // 5.0
    }
}
