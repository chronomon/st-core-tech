package com.chronomon.analysis.trajectory.road;

import com.chronomon.analysis.trajectory.model.DistanceUtil;
import org.locationtech.jts.geom.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 路段对象：既有可能是正向路段，也有可能是双向路段
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class RoadSegment implements IRoadSegment {

    /**
     * 路段的空间线
     */
    public final LineString roadLine;

    /**
     * 路段的方向：正向或者双向
     */
    public final DirectionEnum direction;

    /**
     * 路段在整个路网中的唯一ID，只有在构建路网时才有用
     */
    private int roadId = -1;

    /**
     * 路段起点：是路网图结构（Graph）中的点
     */
    private RoadNode fromNode;

    /**
     * 路段终点：对应路网图结构（Graph）中的点
     */
    private RoadNode toNode;
    private ReversedRoadSegment reversedRoadSegment = null;

    public RoadSegment(LineString roadLine, DirectionEnum direction) {
        if (direction == DirectionEnum.UN_KNOWN || direction == DirectionEnum.DUAL_DIRECT) {
            // 双向路：需要创建其反向路
            this.direction = DirectionEnum.DUAL_DIRECT;
            this.roadLine = roadLine;
            this.reversedRoadSegment = new ReversedRoadSegment(this);
        } else if (direction == DirectionEnum.BACKWARD_DIRECT) {
            // 反向路：需要变为正向
            this.direction = DirectionEnum.FORWARD_DIRECT;
            this.roadLine = roadLine.reverse();
        } else {
            // 正向路：不做多余处理
            this.direction = direction;
            this.roadLine = roadLine;
        }
    }

    public void setRoadId(int roadId) {
        this.roadId = roadId;
    }

    public void setFromNode(RoadNode fromNode) {
        this.fromNode = fromNode;
    }

    public void setToNode(RoadNode toNode) {
        this.toNode = toNode;
    }

    public LineString getRoadLine() {
        return roadLine;
    }

    public Optional<ReversedRoadSegment> getReversedOne() {
        return Optional.ofNullable(reversedRoadSegment);
    }

    @Override
    public int getRoadId() {
        if (roadId == -1) {
            throw new RuntimeException("在构建路网之前，无法获取路段编号");
        }
        return roadId;
    }

    @Override
    public RoadNode getFromNode() {
        if (fromNode == null) {
            throw new RuntimeException("在构建路网之前，无法获取路段节点");
        }
        return fromNode;
    }

    @Override
    public RoadNode getToNode() {
        if (toNode == null) {
            throw new RuntimeException("在构建路网之前，无法获取路段节点");
        }
        return toNode;
    }

    @Override
    public Coordinate getCoordinateN(int n) {
        return roadLine.getCoordinateN(n);
    }

    @Override
    public Point getPointN(int n) {
        return roadLine.getPointN(n);
    }

    @Override
    public int getNumPoints() {
        return roadLine.getNumPoints();
    }

    @Override
    public Envelope getEnvelop() {
        return roadLine.getEnvelopeInternal();
    }

    @Override
    public boolean intersects(Geometry geom) {
        return roadLine.intersects(geom);
    }

    @Override
    public List<Coordinate> getCoordinateList() {
        return Arrays.asList(roadLine.getCoordinates());
    }

    @Override
    public double getLengthInM() {
        return distanceFromStartInM(roadLine.getNumPoints() - 1);
    }

    public double distanceFromStartInM(int index) {
        if (index == 0) return 0.0;
        Coordinate coordinate = roadLine.getCoordinateN(index);
        double distance = coordinate.getZ();
        if (Double.isNaN(distance)) {
            double preStepDistance = distanceFromStartInM(index - 1);
            double currentStepDistance = DistanceUtil.distInMeter(
                    roadLine.getPointN(index - 1), roadLine.getPointN(index));
            distance = preStepDistance + currentStepDistance;
            coordinate.setZ(distance);
        }
        return distance;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(roadId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RoadSegment) {
            return this.roadId == ((RoadSegment) obj).roadId;
        } else {
            return false;
        }
    }
}
