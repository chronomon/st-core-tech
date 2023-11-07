package com.chronomon.trajectory.road;

import com.chronomon.trajectory.model.DefaultUtil;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.ItemVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * 对MBR与查询范围相交的路段（RoadSegment）做二次过滤和收集
 *
 * @author wangrubin
 * @date 2023-10-27
 */
public class RoadSegmentVisitor implements ItemVisitor {

    private final Geometry queryEnv;

    public final List<RoadSegment> segmentList = new ArrayList<>();

    public RoadSegmentVisitor(Envelope queryEnv) {
        this.queryEnv = DefaultUtil.GEOMETRY_FACTORY.toGeometry(queryEnv);
    }

    @Override
    public void visitItem(Object item) {
        if (((RoadSegment) item).intersects(queryEnv)) {
            segmentList.add((RoadSegment) item);
        }
    }
}