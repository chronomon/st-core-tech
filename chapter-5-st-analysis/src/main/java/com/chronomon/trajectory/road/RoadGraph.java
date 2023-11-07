package com.chronomon.trajectory.road;

import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.util.SupplierUtil;

/**
 * 路网图对象：继承了jgrapht包中对Graph的抽象类
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class RoadGraph extends AbstractBaseGraph<RoadNode, IRoadSegment> {
    protected RoadGraph(boolean directed) {
        super(null,
                SupplierUtil.createSupplier(RoadSegment.class),
                new DefaultGraphType.Builder(directed, !directed)
                        .weighted(true)
                        .allowMultipleEdges(false)
                        .allowSelfLoops(true)
                        .build());
    }

    public boolean addRoadSegment(IRoadSegment roadSegment) {
        addVertex(roadSegment.getFromNode());
        addVertex(roadSegment.getToNode());
        boolean success = addEdge(roadSegment.getFromNode(), roadSegment.getToNode(), roadSegment);
        if (success) {
            setEdgeWeight(roadSegment, roadSegment.getLengthInM());
        }
        return success;
    }
}
