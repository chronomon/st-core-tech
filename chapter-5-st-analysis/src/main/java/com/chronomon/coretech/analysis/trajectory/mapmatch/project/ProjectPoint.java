package com.chronomon.coretech.analysis.trajectory.mapmatch.project;

import com.chronomon.coretech.analysis.trajectory.model.DistanceUtil;
import com.chronomon.coretech.analysis.trajectory.road.IRoadSegment;
import com.chronomon.coretech.analysis.trajectory.road.RoadSegment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.distance.GeometryLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 投影点: 由路段、GPS点在路段上的最近点等属性组成
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class ProjectPoint {

    /**
     * 投影到的路段
     */
    public final IRoadSegment roadSegment;

    /**
     * 投影到了路段的第几截
     */
    public final int segmentIndex;

    /**
     * 路段上与GPS最近的点
     */
    public final Point point;

    /**
     * 最近点与GPS点之间的直线距离
     */
    public final double projectDistInM;

    /**
     * 最近点与路段起始坐标之间的路线距离，由路段每一截的长度叠加组成（不是直线距离）
     */
    public final double offsetDistInM;

    /**
     * 当前投影点上预测到的前置投影点
     */
    private int prevIndex = -1;

    /**
     * 当前投影点上计算出的概率值
     */
    private double metric = Double.NEGATIVE_INFINITY;

    /**
     * 当前投影点与前置投影点直接的路网最短路径
     */
    private List<IRoadSegment> pathSegments;

    /**
     * 如果当前投影点与前置投影点之间的路网距离是其直线距离的3倍，
     * 则认为物体不可能是从上一个投影点移动到当前投影点，标记当前
     * 投影点的正常状态为false
     */
    private boolean isNormal = true;

    public ProjectPoint(IRoadSegment roadSegment, int segmentIndex,
                        Point point, double projectDistInM,
                        double offsetDistInM) {
        this.roadSegment = roadSegment;
        this.segmentIndex = segmentIndex;
        this.point = point;
        this.projectDistInM = projectDistInM;
        this.offsetDistInM = offsetDistInM;
    }

    public void setPrevIndex(int prevIndex) {
        this.prevIndex = prevIndex;
    }

    public void setMetric(double metric) {
        this.metric = metric;
    }

    public void setPathSegments(List<IRoadSegment> pathSegments) {
        this.pathSegments = pathSegments;
    }

    public void setNormal(boolean normal) {
        isNormal = normal;
    }

    public boolean isNormal() {
        return isNormal;
    }

    public double getMetric() {
        return metric;
    }

    public int getPrevIndex() {
        return prevIndex;
    }

    public List<IRoadSegment> getPathSegments() {
        return pathSegments;
    }

    /**
     * 判断当前投影点是否与other投影点在同一个路段上，并且比other投影点更靠近路段起点
     *
     * @param other 另外一个要对比的投影点
     * @return 是否符合判断条件
     */
    public boolean onSameSegmentAndBefore(ProjectPoint other) {
        return roadSegment.getRoadId() == other.roadSegment.getRoadId() &&
                offsetDistInM <= other.offsetDistInM;
    }


    /**
     * 获取投影点之间最短路径的前半部分坐标点
     *
     * @return 坐标序列
     */
    public List<Coordinate> getSuffixCoordinates() {
        int size = roadSegment.getNumPoints() - segmentIndex;
        List<Coordinate> suffixCoordinates = new ArrayList<>(size);
        suffixCoordinates.add(point.getCoordinate());
        for (int i = segmentIndex + 1; i < roadSegment.getNumPoints(); i++) {
            suffixCoordinates.add(roadSegment.getCoordinateN(i));
        }
        return suffixCoordinates;
    }

    /**
     * 获取投影点之间最短路径的后半部分坐标点
     *
     * @return 坐标序列
     */
    public List<Coordinate> getPrefixCoordinates() {
        List<Coordinate> prefixCoordinates = new ArrayList<>();
        for (IRoadSegment segment : pathSegments) {
            prefixCoordinates.addAll(segment.getCoordinateList());
            prefixCoordinates.remove(prefixCoordinates.size() - 1);  // 路段的首尾坐标相同，只保留首部的即可
        }
        for (int i = 0; i <= segmentIndex; i++) {
            prefixCoordinates.add(roadSegment.getCoordinateN(i));
        }
        prefixCoordinates.add(point.getCoordinate());
        return prefixCoordinates;
    }

    /**
     * 根据GPS点和路段计算投影点的静态方法
     *
     * @param gpsPoint    GPS点
     * @param roadSegment 路段
     * @return 投影点
     */
    public static ProjectPoint project(Point gpsPoint, RoadSegment roadSegment) {
        DistanceOp distanceOp = new DistanceOp(roadSegment.getRoadLine(), gpsPoint);
        GeometryLocation projectLocation = distanceOp.nearestLocations()[0];
        Point projectPoint = gpsPoint.getFactory().createPoint(projectLocation.getCoordinate());
        int segmentIndex = projectLocation.getSegmentIndex();
        double projectDistanceInM = DistanceUtil.distInMeter(gpsPoint, projectPoint);
        double offsetDistanceInM = roadSegment.distanceFromStartInM(segmentIndex) + DistanceUtil.distInMeter(roadSegment.getPointN(segmentIndex), projectPoint);
        return new ProjectPoint(roadSegment, segmentIndex, projectPoint, projectDistanceInM, offsetDistanceInM);
    }
}
