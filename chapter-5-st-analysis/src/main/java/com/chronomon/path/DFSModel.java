package com.chronomon.path;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 深度优先搜索
 *
 * @author yuzisheng
 * @date 2023-11-07
 */
public class DFSModel {
    /**
     * 有向无权图
     */
    private final DefaultDirectedGraph<String, DefaultEdge> graph;
    /**
     * 深度优先搜索模型
     *
     * @see DepthFirstIterator
     */
    private DepthFirstIterator dfsModel;

    DFSModel(DefaultDirectedGraph<String, DefaultEdge> graph) {
        this.graph = graph;
    }

    /**
     * 路径搜索
     *
     * @param startVertex 起点
     * @param endVertex   终点
     * @return 路径
     * @see DepthFirstIterator#next()
     */
    public List<String> getShortestPath(String startVertex, String endVertex) {
        dfsModel = new DepthFirstIterator<>(graph, startVertex);
        List<String> traverse = new ArrayList<>();
        while (dfsModel.hasNext()) {
            traverse.add((String) dfsModel.next());
        }
        if (traverse.contains(endVertex)) {
            int endIndex = traverse.indexOf(endVertex);
            return traverse.subList(0, endIndex + 1);
        } else {
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (String v : Arrays.asList("A", "B", "C", "D", "E", "F")) {
            graph.addVertex(v);
        }

        graph.addEdge("A", "F");
        graph.addEdge("A", "C");
        graph.addEdge("A", "B");
        graph.addEdge("B", "D");
        graph.addEdge("B", "C");
        graph.addEdge("C", "F");
        graph.addEdge("C", "D");
        graph.addEdge("D", "E");
        graph.addEdge("F", "E");

        DFSModel dfsModel = new DFSModel(graph);
        System.out.println(dfsModel.getShortestPath("A", "E"));  // [A, B, C, D, E, F]
    }
}
