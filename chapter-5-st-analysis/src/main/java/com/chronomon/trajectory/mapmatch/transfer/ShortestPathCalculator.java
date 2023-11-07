package com.chronomon.trajectory.mapmatch.transfer;


import com.chronomon.trajectory.road.IRoadSegment;
import com.chronomon.trajectory.road.RoadNetwork;
import com.chronomon.trajectory.road.RoadNode;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraManyToManyShortestPaths;
import org.jgrapht.alg.util.Pair;

import java.util.*;

public class ShortestPathCalculator {

    // jgrapht原生的ManyToMany最短路径计算有Bug，而且效率低
    // private final DijkstraManyToManyShortestPaths<RoadNode, RoadSegment> algorithm;
    private final BidirectionalDijkstraManyToManyShortestPaths<RoadNode, IRoadSegment> algorithm;


    public ShortestPathCalculator(RoadNetwork rn) {
        //this.algorithm = new DijkstraManyToManyShortestPaths<>(rn.getRoadGraph());
        this.algorithm = new BidirectionalDijkstraManyToManyShortestPaths<>(rn.getRoadGraph());
    }

    public ShortestPathSet calculate(Set<RoadNode> fromPoints, Set<RoadNode> toPoints) {
        Map<Pair<RoadNode, RoadNode>, GraphPath<RoadNode, IRoadSegment>> paths = algorithm.getPaths(fromPoints, toPoints);
        Map<SourceAndDest, ShortestPath> result = new HashMap<>();
        for (RoadNode fromPoint : fromPoints) {
            for (RoadNode toPoint : toPoints) {
                if (fromPoint.equals(toPoint)) {
                    // 两点相同，最短路径长度为0.0，不用经过任何路段
                    result.put(new SourceAndDest(fromPoint, toPoint), new ShortestPath(0.0, Collections.emptyList()));
                } else {
                    GraphPath<RoadNode, IRoadSegment> path = paths.get(new Pair<>(fromPoint, toPoint));
                    if (path != null && path.getWeight() != Double.POSITIVE_INFINITY) {
                        // 说明两点之间存在最短路径，加入结果集
                        result.put(new SourceAndDest(fromPoint, toPoint), new ShortestPath(path.getWeight(), path.getEdgeList()));
                    }
                }
            }
        }
        return new ShortestPathSet(result);
    }

    public static final class ShortestPathSet {
        private final Map<SourceAndDest, ShortestPath> shortestPathMap;

        public ShortestPathSet(Map<SourceAndDest, ShortestPath> shortestPathMap) {
            this.shortestPathMap = shortestPathMap;
        }

        public Optional<ShortestPath> getShortestPath(RoadNode sourceNode, RoadNode destNode) {
            return Optional.ofNullable(shortestPathMap.get(new SourceAndDest(sourceNode, destNode)));
        }
    }

    private static class SourceAndDest {
        public final RoadNode sourceNode;

        public final RoadNode destNode;

        public SourceAndDest(RoadNode sourceNode, RoadNode destNode) {
            this.sourceNode = sourceNode;
            this.destNode = destNode;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(sourceNode.nodeId + destNode.nodeId);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SourceAndDest) {
                SourceAndDest other = (SourceAndDest) obj;
                return this.sourceNode.equals(other.sourceNode) &&
                        this.destNode.equals(other.destNode);
            }
            return false;
        }
    }

    public static class ShortestPath {
        public final double pathLength;

        public final List<IRoadSegment> segmentList;

        public ShortestPath(double pathLength, List<IRoadSegment> segmentList) {
            this.pathLength = pathLength;
            this.segmentList = segmentList;
        }
    }
}
