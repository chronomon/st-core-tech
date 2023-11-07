package com.chronomon.path;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.Arrays;

/**
 * 广度优先搜索
 *
 * @author yuzisheng
 * @date 2023-11-05
 */
public class BFSModel {
    /**
     * 广度优先搜索模型
     *
     * @see BFSShortestPath
     */
    private final BFSShortestPath<String, DefaultEdge> bfsModel;

    BFSModel(SimpleGraph<String, DefaultEdge> graph) {
        bfsModel = new BFSShortestPath<>(graph);
    }

    /**
     * 最短路径搜索
     *
     * @param startVertex 起点
     * @param endVertex   终点
     * @return 最短路径
     * @see BFSShortestPath#getPath(Object, Object)
     */
    public GraphPath<String, DefaultEdge> getShortestPath(String startVertex, String endVertex) {
        return bfsModel.getPath(startVertex, endVertex);
    }

    public static void main(String[] args) {
        SimpleGraph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        for (String v : Arrays.asList("A", "B", "C", "D", "E", "F")) {
            graph.addVertex(v);
        }
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("A", "F");
        graph.addEdge("B", "C");
        graph.addEdge("B", "D");
        graph.addEdge("C", "D");
        graph.addEdge("C", "F");
        graph.addEdge("D", "E");
        graph.addEdge("E", "F");

        BFSModel bfsModel = new BFSModel(graph);
        System.out.println(bfsModel.getShortestPath("A", "E"));  // AFE
    }
}
