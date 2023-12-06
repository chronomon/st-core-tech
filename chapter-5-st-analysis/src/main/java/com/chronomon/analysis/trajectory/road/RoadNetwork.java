package com.chronomon.analysis.trajectory.road;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.distance.GeometryLocation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 路网对象：根据路段的联通关系构建的图对象，用于计算最短路径
 *
 * @author wangrubin3
 * @date 2023-10-27
 */
public class RoadNetwork {
    private final AtomicInteger roadIdAssigner = new AtomicInteger(1);

    private final AtomicInteger roadNodeIdAssigner = new AtomicInteger(1);

    private final Map<Coordinate, RoadNode> roadNodeMap = new HashMap<>();

    private final Map<Integer, RoadSegment> roadSegmentMap;

    public RoadNetwork(List<RoadSegment> roadSegmentList, boolean fixConnect) {
        this.roadSegmentMap = new HashMap<>(roadSegmentList.size());
        for (RoadSegment segment : roadSegmentList) {
            addSegment(segment);
        }
        if (fixConnect) {
            fixConnection();
        }
    }

    private void addSegment(RoadSegment segment) {
        Point from = segment.getPointN(0);
        Point to = segment.getPointN(segment.getNumPoints() - 1);

        int roadId = roadIdAssigner.getAndIncrement();
        segment.setRoadId(roadId);
        segment.setFromNode(getRoadNode(from, segment.direction, true));
        segment.setToNode(getRoadNode(to, segment.direction, false));

        this.roadSegmentMap.put(roadId, segment);
    }

    private RoadNode getRoadNode(Point point, DirectionEnum direction, boolean isFrom) {
        RoadNode roadNode = roadNodeMap.get(point.getCoordinate());
        if (roadNode == null) {
            roadNode = new RoadNode(roadNodeIdAssigner.getAndIncrement(), point);
            roadNodeMap.put(point.getCoordinate(), roadNode);
        }

        if (direction == DirectionEnum.DUAL_DIRECT) {
            roadNode.inCount += 1;
            roadNode.outCount += 1;
        } else if (isFrom) {
            roadNode.outCount += 1;
        } else {
            roadNode.inCount += 1;
        }
        return roadNode;
    }

    private volatile RoadGraph directedGraph = null;

    private volatile STRtree roadRtree = null;


    /**
     * 修复路网
     * 1. 在路段交点处增加节点
     * 2. 对于没有方向的路段，根据相交路段的方向设置一个最优可能的方向值
     */
    private void fixConnection() {
        getRoadRtree();
        Set<RoadNode> roadNodeSet = roadNodeMap.values()
                .stream().filter(node -> node.inCount == 0 || node.outCount == 0)
                .collect(Collectors.toSet());

        Map<Integer, Set<RoadSegment>> truncatedRoadMap = new HashMap<>();
        for (RoadNode roadNode : roadNodeSet) {
            Envelope queryEnv = roadNode.geom.getEnvelopeInternal();
            RoadSegmentVisitor visitor = new RoadSegmentVisitor(queryEnv);
            roadRtree.query(queryEnv, visitor);
            List<RoadSegment> candidateSegmentList = visitor.segmentList;
            if (candidateSegmentList.size() > 2) {
                continue;
            }
            for (RoadSegment segment : candidateSegmentList) {
                if (segment.getFromNode().nodeId == roadNode.nodeId ||
                        segment.getToNode().nodeId == roadNode.nodeId) {
                    continue;
                }

                Set<RoadSegment> subRoadSegmentSet = truncatedRoadMap.get(segment.getRoadId());
                if (subRoadSegmentSet != null) {
                    segment = truncatedRoadMap.get(segment.getRoadId()).stream()
                            .filter(subSegment -> subSegment.intersects(roadNode.geom))
                            .findFirst().get();
                }

                DistanceOp distanceOp = new DistanceOp(segment.getRoadLine(), roadNode.geom);
                assert distanceOp.distance() == 0.0;
                GeometryLocation locationOnSegment = distanceOp.nearestLocations()[0];
                int segmentIndex = locationOnSegment.getSegmentIndex();
                List<RoadSegment> twoHalfSegments = truncateRoadSegment(segment, roadNode, segmentIndex);

                if (subRoadSegmentSet != null) {
                    subRoadSegmentSet.remove(segment);
                    subRoadSegmentSet.addAll(twoHalfSegments);
                } else {
                    truncatedRoadMap.put(segment.getRoadId(), new HashSet<>(twoHalfSegments));
                }
            }
        }
        roadRtree = null; // 因为Segment发生了变化，原来的RTree失效
    }

    private List<RoadSegment> truncateRoadSegment(RoadSegment segment, RoadNode roadNode, int segmentIndex) {
        List<Coordinate> firstPart = new ArrayList<>(segmentIndex + 2);
        List<Coordinate> secondPart = new ArrayList<>(segment.getNumPoints() - segmentIndex);
        secondPart.add(roadNode.geom.getCoordinate());
        for (int i = 0; i < segment.getNumPoints(); i++) {
            if (i <= segmentIndex) {
                firstPart.add(segment.getCoordinateN(i));
            } else {
                secondPart.add(segment.getCoordinateN(i));
            }
        }
        firstPart.add(roadNode.geom.getCoordinate());

        // 删除掉原来的，新增分段后的
        roadSegmentMap.remove(segment.getRoadId());
        LineString firstLine = segment.getRoadLine().getFactory().createLineString(firstPart.toArray(new Coordinate[0]));
        LineString secondLine = segment.getRoadLine().getFactory().createLineString(secondPart.toArray(new Coordinate[0]));

        int firstRoadId = roadIdAssigner.getAndIncrement();
        RoadSegment firstSegment = new RoadSegment(firstLine, segment.direction);
        firstSegment.setRoadId(firstRoadId);
        firstSegment.setFromNode(segment.getFromNode());
        firstSegment.setToNode(roadNode);
        roadSegmentMap.put(firstRoadId, firstSegment);

        int secondRoadId = roadIdAssigner.getAndIncrement();
        RoadSegment secondSegment = new RoadSegment(secondLine, segment.direction);
        secondSegment.setRoadId(secondRoadId);
        secondSegment.setFromNode(roadNode);
        secondSegment.setToNode(segment.getToNode());
        roadSegmentMap.put(secondRoadId, secondSegment);

        return Arrays.asList(firstSegment, secondSegment);
    }

    public RoadGraph getRoadGraph() {
        if (directedGraph == null) {
            synchronized (this) {
                if (directedGraph == null) {
                    directedGraph = new RoadGraph(true);
                    new ArrayList<>(roadSegmentMap.values()).forEach(segment -> {
                        boolean success = directedGraph.addRoadSegment(segment);
                        if (!success) {
                            // 添加边失败，说明存在环，需要将当前路段一分为二
                            LineString roadLine = segment.getRoadLine();
                            List<Coordinate> firstPart = new ArrayList<>(2);
                            List<Coordinate> secondPart = new ArrayList<>(roadLine.getNumPoints());

                            if (roadLine.getNumPoints() > 2) {
                                firstPart.add(roadLine.getCoordinateN(0));
                                firstPart.add(roadLine.getCoordinateN(1));
                                for (int i = 1; i < roadLine.getNumPoints(); i++) {
                                    secondPart.add(roadLine.getCoordinateN(i));
                                }
                            } else {
                                Coordinate startPoint = roadLine.getCoordinateN(0);
                                Coordinate endPoint = roadLine.getCoordinateN(1);
                                Coordinate minPoint = LineSegment.midPoint(startPoint, endPoint);
                                firstPart.add(startPoint);
                                firstPart.add(minPoint);
                                secondPart.add(minPoint);
                                secondPart.add(endPoint);
                            }

                            RoadSegment firstSegment = new RoadSegment(roadLine.getFactory().createLineString(firstPart.toArray(new Coordinate[2])), segment.direction);
                            RoadSegment secondSegment = new RoadSegment(roadLine.getFactory().createLineString(secondPart.toArray(new Coordinate[0])), segment.direction);
                            addSegment(firstSegment);
                            addSegment(secondSegment);

                            directedGraph.addRoadSegment(firstSegment);
                            directedGraph.addRoadSegment(secondSegment);
                            firstSegment.getReversedOne().map(directedGraph::addRoadSegment);
                            secondSegment.getReversedOne().map(directedGraph::addRoadSegment);
                        } else {
                            segment.getReversedOne().map(directedGraph::addRoadSegment);
                        }
                    });
                    roadRtree = null; // 因为Segment发生了变化，原来的RTree失效
                }
            }
        }
        return directedGraph;
    }

    public STRtree getRoadRtree() {
        if (null == roadRtree) {
            synchronized (this) {
                roadRtree = new STRtree();
                roadSegmentMap.values().forEach(segment -> {
                    Envelope env = segment.getEnvelop();
                    roadRtree.insert(env, segment);
                });
                roadRtree.build();
            }

        }
        return roadRtree;
    }

}
