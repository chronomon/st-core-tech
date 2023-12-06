package com.chronomon.analysis.trajectory.road;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 反向路段：双向路段的反向路段，与双向路段对象相互引用，共享变量和计算结果
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class ReversedRoadSegment implements IRoadSegment {

    /**
     * 反向路对应的双向路对象
     */
    private final RoadSegment baseOne;

    /**
     * 反向路的坐标序列，与baseOne的坐标序列正好相反
     */
    private final List<Coordinate> coordinateList;

    public ReversedRoadSegment(RoadSegment baseOne) {
        this.baseOne = baseOne;
        coordinateList = new ArrayList<>(baseOne.getCoordinateList());
        Collections.reverse(coordinateList);
    }

    @Override
    public int getRoadId() {
        return -baseOne.getRoadId();
    }

    @Override
    public RoadNode getFromNode() {
        return baseOne.getToNode();
    }

    @Override
    public RoadNode getToNode() {
        return baseOne.getFromNode();
    }

    @Override
    public Coordinate getCoordinateN(int n) {
        return baseOne.getCoordinateN(baseOne.getNumPoints() - n - 1);
    }

    @Override
    public Point getPointN(int n) {
        return baseOne.getPointN(baseOne.getNumPoints() - n - 1);
    }

    @Override
    public int getNumPoints() {
        return baseOne.getNumPoints();
    }

    @Override
    public Envelope getEnvelop() {
        return baseOne.getEnvelop();
    }

    @Override
    public boolean intersects(Geometry geom) {
        return baseOne.intersects(geom);
    }

    @Override
    public List<Coordinate> getCoordinateList() {
        return coordinateList;
    }

    @Override
    public double getLengthInM() {
        return baseOne.getLengthInM();
    }

    @Override
    public double distanceFromStartInM(int index) {
        return baseOne.getLengthInM() - baseOne.distanceFromStartInM(baseOne.getNumPoints() - index - 1);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getRoadId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReversedRoadSegment) {
            return this.getRoadId() == ((ReversedRoadSegment) obj).getRoadId();
        } else {
            return false;
        }
    }
}
