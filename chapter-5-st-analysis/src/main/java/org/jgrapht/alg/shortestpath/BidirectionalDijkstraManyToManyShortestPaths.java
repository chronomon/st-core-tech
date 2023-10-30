package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.ContractionHierarchyBidirectionalDijkstra.ContractionSearchFrontier;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.GraphWalk;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This class is not thread safe
 */
public class BidirectionalDijkstraManyToManyShortestPaths<V, E> {

    private final Graph<V, E> graph;

    private final Supplier<AddressableHeap<Double, Pair<V, E>>> heapSupplier;

    private Map<V, ForwardFrontier<V, E>> forwardFrontiers;

    private Map<V, ContractionSearchFrontier<V, E>> backwardFrontiers;

    public BidirectionalDijkstraManyToManyShortestPaths(Graph<V, E> graph) {
        this.graph = graph;
        this.heapSupplier = PairingHeap::new;
    }

    public Map<Pair<V, V>, GraphPath<V, E>> getPaths(Set<V> startNodes, Set<V> endNodes) {
        getAllPaths(startNodes, endNodes);
        Map<Pair<V, V>, GraphPath<V, E>> pathMap = new HashMap<>();
        for (V source : startNodes) {
            for (V target : endNodes) {
                Pair<V, V> key = new Pair<>(source, target);
                GraphPath<V, E> path;
                if (source.equals(target)) {
                    path = GraphWalk.singletonWalk(graph, source, 0.0D);
                } else {
                    ForwardFrontier<V, E> forwardFrontier = forwardFrontiers.get(source);
                    ContractionSearchFrontier<V, E> backwardFrontier = backwardFrontiers.get(target);
                    double bestPath = forwardFrontier.bestTupleMap.get(target).getSecond();
                    V commonVertex = forwardFrontier.bestTupleMap.get(target).getFirst();
                    path = createPath(forwardFrontier, backwardFrontier, bestPath, source, commonVertex, target);
                }
                pathMap.put(key, path);
            }
        }
        return pathMap;
    }

    private void getAllPaths(Set<V> sources, Set<V> sinks) {
        forwardFrontiers = new HashMap<>(sources.size());
        backwardFrontiers = new HashMap<>(sinks.size());
        final EdgeReversedGraph<V, E> reversedGraph = new EdgeReversedGraph<V, E>(graph);

        for (V source : sources) {
            ContractionSearchFrontier<V, E> forwardFrontier =
                    new ContractionSearchFrontier<V, E>(graph, heapSupplier);
            forwardFrontier.updateDistance(source, null, 0d);
            forwardFrontiers.put(source, new ForwardFrontier<>(source, forwardFrontier, sinks, this.graph));
        }
        for (V sink : sinks) {
            ContractionSearchFrontier<V, E> backwardFrontier =
                    new ContractionSearchFrontier<>(reversedGraph, heapSupplier);
            backwardFrontier.updateDistance(sink, null, 0d);
            backwardFrontiers.put(sink, backwardFrontier);
        }
        while (!forwardFrontiers.values().stream().allMatch(i -> i.isStop) || !backwardFrontiers.values().stream().allMatch(i -> i.isFinished)) {
            forwardMove();
            backwardMove();
        }
    }

    private void forwardMove() {
        for (ForwardFrontier<V, E> forwardFrontier : forwardFrontiers.values()) {
            if (forwardFrontier.isStop) {
                continue;
            }
            if (forwardFrontier.frontier.heap.isEmpty() ||
                    backwardFrontiers.entrySet().stream().allMatch(entry -> forwardFrontier.isForwardStop(entry.getKey(), entry.getValue()))) {
                forwardFrontier.isStop = true;
            } else {

                AddressableHeap.Handle<Double, Pair<V, E>> node = forwardFrontier.frontier.heap.deleteMin();
                V v = node.getValue().getFirst();
                double vDistance = node.getKey();

                for (E e : forwardFrontier.frontier.graph.outgoingEdgesOf(v)) {
                    V u = Graphs.getOppositeVertex(forwardFrontier.frontier.graph, e, v);
                    double eWeight = forwardFrontier.frontier.graph.getEdgeWeight(e);
                    forwardFrontier.frontier.updateDistance(u, e, vDistance + eWeight);
                    for (Map.Entry<V, ContractionSearchFrontier<V, E>> entry : backwardFrontiers.entrySet()) {
                        V sink = entry.getKey();
                        final Pair<V, Double> tuple2 = forwardFrontier.bestTupleMap.get(sink);
                        double bestDist = tuple2.getSecond();
                        double pathDistance = vDistance + eWeight + entry.getValue().getDistance(u);
                        if (pathDistance < bestDist) {
                            forwardFrontier.bestTupleMap.put(sink, new Pair<>(u, pathDistance));
                        }
                    }
                }
            }
        }
    }

    private void backwardMove() {
        for (Map.Entry<V, ContractionSearchFrontier<V, E>> entry : backwardFrontiers.entrySet()) {
            V sink = entry.getKey();
            ContractionSearchFrontier<V, E> backwardFrontier = entry.getValue();
            if (backwardFrontier.isFinished) {
                continue;
            }
            if (backwardFrontier.heap.isEmpty() ||
                    forwardFrontiers.values().stream().allMatch(frontier -> {
                        if (frontier.isStop || frontier.frontier.heap.isEmpty()) {
                            return true;
                        }
                        final Double bestDist = frontier.bestTupleMap.get(sink).getSecond();
                        return backwardFrontier.heap.findMin().getKey() + frontier.frontier.heap.findMin().getKey() > bestDist;
                    })) {
                backwardFrontier.isFinished = true;
            } else {

                AddressableHeap.Handle<Double, Pair<V, E>> node = backwardFrontier.heap.deleteMin();
                V v = node.getValue().getFirst();
                double vDistance = node.getKey();

                for (E e : backwardFrontier.graph.outgoingEdgesOf(v)) {
                    V u = Graphs.getOppositeVertex(backwardFrontier.graph, e, v);
                    double eWeight = backwardFrontier.graph.getEdgeWeight(e);
                    double newDist = vDistance + eWeight;
                    backwardFrontier.updateDistance(u, e, newDist);
                    for (ForwardFrontier<V, E> forwardFrontier : forwardFrontiers.values()) {
                        final Pair<V, Double> tuple2 = forwardFrontier.bestTupleMap.get(sink);
                        double bestDist = tuple2.getSecond();
                        double pathDist = newDist + forwardFrontier.frontier.getDistance(u);
                        if (pathDist < bestDist) {
                            forwardFrontier.bestTupleMap.put(sink, new Pair<>(u, pathDist));
                        }
                    }
                }
            }
        }
    }

    private GraphPath<V, E> createPath(BaseBidirectionalShortestPathAlgorithm.BaseSearchFrontier<V, E> forwardFrontier,
                                       BaseBidirectionalShortestPathAlgorithm.BaseSearchFrontier<V, E> backwardFrontier,
                                       double weight, V source, V commonVertex, V sink) {

        LinkedList<E> edgeList = new LinkedList();
        LinkedList<V> vertexList = new LinkedList();
        vertexList.add(commonVertex);
        V v = commonVertex;

        while (true) {
            E e = forwardFrontier.getTreeEdge(v);
            if (e == null) {
                v = commonVertex;

                while (true) {
                    e = backwardFrontier.getTreeEdge(v);
                    if (e == null) {
                        return new GraphWalk(this.graph, source, sink, vertexList, edgeList, weight);
                    }
                    edgeList.addLast(e);
                    v = Graphs.getOppositeVertex(backwardFrontier.graph, e, v);
                    vertexList.addLast(v);
                }
            }

            edgeList.addFirst(e);
            v = Graphs.getOppositeVertex(forwardFrontier.graph, e, v);
            vertexList.addFirst(v);
        }
    }

    private static class ForwardFrontier<V, E> extends BaseBidirectionalShortestPathAlgorithm.BaseSearchFrontier<V, E> {

        protected final V vertex;

        protected BidirectionalDijkstraShortestPath.DijkstraSearchFrontier<V, E> frontier;

        protected Map<V, Pair<V, Double>> bestTupleMap;

        protected boolean isStop = false;

        ForwardFrontier(V vertex, BidirectionalDijkstraShortestPath.DijkstraSearchFrontier<V, E> frontier, Set<V> sinks, Graph<V, E> graph) {
            super(graph);
            this.vertex = vertex;
            this.frontier = frontier;
            bestTupleMap = new HashMap<>(sinks.size());
            for (V sink : sinks) {
                bestTupleMap.put(sink, new Pair<>(null, Double.POSITIVE_INFINITY));
            }
        }

        private boolean isForwardStop(V otherVertex, BidirectionalDijkstraShortestPath.DijkstraSearchFrontier<V, E> otherFrontier) {
            if (otherFrontier.heap.isEmpty()) {
                return true;
            }
            final Pair<V, Double> tuple = bestTupleMap.get(otherVertex);
            return frontier.heap.findMin().getKey() + otherFrontier.heap.findMin().getKey() > tuple.getSecond();
        }

        @Override
        public double getDistance(V v) {
            AddressableHeap.Handle<Double, Pair<V, E>> node = frontier.seen.get(v);
            return node == null ? 1.0D / 0.0 : node.getKey();
        }

        @Override
        public E getTreeEdge(V v) {
            AddressableHeap.Handle<Double, Pair<V, E>> node = frontier.seen.get(v);
            return node == null ? null : (node.getValue()).getSecond();
        }
    }
}
