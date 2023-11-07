package com.chronomon.path;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.Arrays;

/**
 * 迪杰斯特拉搜索
 *
 * @author yuzisheng
 * @date 2023-11-05
 */
public class DijkstraModel {
    /**
     * 迪杰斯特拉搜索模型
     *
     * @see DijkstraShortestPath
     */
    private final DijkstraShortestPath<String, DefaultWeightedEdge> dijkstraModel;

    DijkstraModel(SimpleWeightedGraph<String, DefaultWeightedEdge> graph) {
        dijkstraModel = new DijkstraShortestPath<>(graph);
    }

    /**
     * 最短路径搜索
     *
     * @param startVertex 起点
     * @param endVertex   终点
     * @return 最短路径
     * @see DijkstraShortestPath#getPath(Object, Object)
     */
    public GraphPath<String, DefaultWeightedEdge> getShortestPath(String startVertex, String endVertex) {
        return dijkstraModel.getPath(startVertex, endVertex);
    }

    public static void main(String[] args) {
        SimpleWeightedGraph<String, DefaultWeightedEdge> graph = new SimpleWeightedGraph(DefaultWeightedEdge.class);
        for (String v : Arrays.asList("A", "B", "C", "D", "E", "F")) {
            graph.addVertex(v);
        }

        graph.addEdge("A", "B");
        graph.setEdgeWeight("A", "B", 7);
        graph.addEdge("A", "C");
        graph.setEdgeWeight("A", "C", 7);
        graph.addEdge("A", "F");
        graph.setEdgeWeight("A", "F", 14);
        graph.addEdge("B", "C");
        graph.setEdgeWeight("B", "C", 10);
        graph.addEdge("B", "D");
        graph.setEdgeWeight("B", "D", 15);
        graph.addEdge("C", "D");
        graph.setEdgeWeight("C", "D", 11);
        graph.addEdge("C", "F");
        graph.setEdgeWeight("C", "F", 2);
        graph.addEdge("D", "E");
        graph.setEdgeWeight("D", "E", 6);
        graph.addEdge("E", "F");
        graph.setEdgeWeight("E", "F", 9);

        DijkstraModel dijkstraModel = new DijkstraModel(graph);
        System.out.println(dijkstraModel.getShortestPath("A", "E"));  // ACFE=9+2+9=20
    }
}
