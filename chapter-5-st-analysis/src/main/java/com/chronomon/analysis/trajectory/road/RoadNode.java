package com.chronomon.analysis.trajectory.road;

import org.locationtech.jts.geom.Point;

public class RoadNode {

    public final int nodeId;

    public final Point geom;

    public int inCount = 0;

    public int outCount = 0;

    public RoadNode(int nodeId, Point geom) {
        this.nodeId = nodeId;
        this.geom = geom;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(nodeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RoadNode) {
            return this.nodeId == ((RoadNode) obj).nodeId;
        } else {
            return false;
        }
    }
}
