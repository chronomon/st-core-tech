package com.chronomon.analysis.trajectory.filter;

import com.chronomon.analysis.trajectory.model.GpsPoint;
import com.chronomon.analysis.trajectory.model.Trajectory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 轨迹去噪
 */
public class TrajNoiseFilter {

    /**
     * 连续的噪点数量，如果分段后的子轨迹GPS点数量小于这个值，则认为该子轨迹是连续的噪点
     */
    private final static int MAX_NOISE_POINT_NUM = 5;

    /**
     * 两个GPS点的最大平均速度
     */
    private final double maxSpeedInMeterPerSec;

    /**
     * @param maxSpeedInMeterPerSec 两点间的最大速度，要根据车辆的行驶速度来定
     */
    public TrajNoiseFilter(double maxSpeedInMeterPerSec) {
        this.maxSpeedInMeterPerSec = maxSpeedInMeterPerSec;
    }

    public List<Trajectory> filter(Trajectory trajectory) {
        // 检测并标记可疑的Segment
        int currIndex = 0;
        List<Integer> tags = new ArrayList<>();
        while (currIndex < trajectory.getNumPoints() - 1) {
            double speed = trajectory.getGpsPoint(currIndex).speedInMeterPerSec(trajectory.getGpsPoint(currIndex + 1));
            if (speed > maxSpeedInMeterPerSec) {
                // 当前GPS点和下一个GPS速度超过阈值，打上标签
                tags.add(currIndex);
            }
            currIndex++;
        }

        if (tags.isEmpty()) {
            // 没有检测到噪点，返回原始轨迹
            return Collections.singletonList(trajectory);
        }

        // 剔除噪点
        tags.add(trajectory.getNumPoints() - 1); // 为了包含最后一段轨迹
        List<Trajectory> cleanTrajectoryList = new ArrayList<>();
        List<GpsPoint> reservedGpsList = new ArrayList<>();
        int from = 0;
        for (Integer tag : tags) {
            int to = tag + 1;
            if (to - from > MAX_NOISE_POINT_NUM) {
                // 是一段有效子轨迹，否则认为是噪点
                List<GpsPoint> validGpsList = trajectory.getSortedGpsList().subList(from, to);
                if (reservedGpsList.isEmpty()) {
                    reservedGpsList.addAll(validGpsList);
                } else {
                    GpsPoint tail = reservedGpsList.get(reservedGpsList.size() - 1);
                    if (tail.getTime().equals(trajectory.getGpsPoint(from - 1).getTime())) {
                        // 如果tag前后的两段轨迹都不属于噪点，则认为该tag是一个误检测，不进行轨迹切分
                        reservedGpsList.addAll(validGpsList);
                    } else if (tail.speedInMeterPerSec(validGpsList.get(0)) <= maxSpeedInMeterPerSec) {
                        // 前后两段有效轨迹衔接处的速度小于阈值，则可将两段轨迹连接起来，不进行轨迹切分
                        reservedGpsList.addAll(validGpsList);
                    } else {
                        // 前后两段有效轨迹衔接处的速度大于阈值，要进行轨迹切分
                        cleanTrajectoryList.add(new Trajectory(trajectory.getOid(), reservedGpsList));
                        reservedGpsList = new ArrayList<>(validGpsList);
                    }
                }
            }
            from = to;
        }
        if (reservedGpsList.size() > 0) {
            cleanTrajectoryList.add(new Trajectory(trajectory.getOid(), reservedGpsList));
        }

        return cleanTrajectoryList;
    }

    public static void main(String[] args) throws Exception {
        List<GpsPoint> gpsList = readGpsPoint();
        TrajNoiseFilter noiseFilter = new TrajNoiseFilter(10.0);
        List<Trajectory> subTrajectoryList = noiseFilter.filter(new Trajectory("oid", gpsList, true));
        System.out.println(subTrajectoryList.size());

        int count = 0;
        for (Trajectory sub : subTrajectoryList) {
            count += sub.getNumPoints();
        }

        System.out.println("raw:" + gpsList.size());
        System.out.println("clean:" + count);

        if (subTrajectoryList.size() == 1) {
            System.out.println("没有检测到噪点");
        } else {
            if (count == gpsList.size()) {
                System.out.println("去噪轨迹速度设置偏小，GPS点被误认为是噪点");
            } else {
                System.out.println("检测到噪点，数量为：" + (gpsList.size() - count));
            }
        }
    }

    public static List<GpsPoint> readGpsPoint() throws Exception {
        URI uri = Objects.requireNonNull(TrajNoiseFilter.class.getClassLoader().getResource("trajectory_noisefilter.txt")).toURI();
        List<String> lines = Files.readAllLines(Paths.get(uri));
        String wkt = lines.get(0);
        LineString lineString = (LineString) new WKTReader().read(wkt);
        List<GpsPoint> gpsPointList = new ArrayList<>(lineString.getNumPoints());
        for (int i = 0; i < lineString.getNumPoints(); i++) {
            Point geom = lineString.getPointN(i);
            Timestamp time = new Timestamp((long) geom.getCoordinate().getM());
            gpsPointList.add(new GpsPoint("oid", lineString.getCoordinateN(i), time));
        }
        return gpsPointList;
    }
}
