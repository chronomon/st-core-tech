package com.chronomon.analysis.path;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * A*搜索
 *
 * @author yuzisheng
 * @date 2023-11-06
 */
public class AStarModel {
    /**
     * A*搜索模型
     *
     * @see AStarShortestPath
     */
    private final AStarShortestPath<AStarNode, DefaultWeightedEdge> aStarModel;

    AStarModel(SimpleWeightedGraph<AStarNode, DefaultWeightedEdge> graph) {
        aStarModel = new AStarShortestPath<>(graph, new ManhattanDistance());
    }

    /**
     * 最短路径搜索
     *
     * @param startNode 起点
     * @param endNode   终点
     * @return 最短路径
     * @see AStarShortestPath#getPath(Object, Object)
     */
    public GraphPath<AStarNode, DefaultWeightedEdge> getShortestPath(AStarNode startNode, AStarNode endNode) {
        return aStarModel.getPath(startNode, endNode);
    }

    public static void main(String[] args) {
        String[] labyrinth = {
                "......E.",
                "..#####.",
                "......#.",
                "......#.",
                "......#.",
                "......#.",
                "S######.",
                "........"};
        AStarNode startNode = null;  // (6,0)
        AStarNode endNode = null;  // (0,6)
        SimpleWeightedGraph<AStarNode, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        // 加载迷宫
        AStarNode[][] nodes = new AStarNode[labyrinth.length][labyrinth[0].length()];
        for (int i = 0; i < labyrinth.length; i++) {
            for (int j = 0; j < labyrinth[0].length(); j++) {
                if (labyrinth[i].charAt(j) == '#') {
                    continue;
                }
                nodes[i][j] = new AStarNode(i, j);
                graph.addVertex(nodes[i][j]);
                if (labyrinth[i].charAt(j) == 'S') {
                    startNode = nodes[i][j];
                } else if (labyrinth[i].charAt(j) == 'E') {
                    endNode = nodes[i][j];
                }
            }
        }
        for (int i = 0; i < labyrinth.length; i++) {
            for (int j = 0; j < labyrinth[0].length() - 1; j++) {
                if (nodes[i][j] == null || nodes[i][j + 1] == null) {
                    continue;
                }
                Graphs.addEdge(graph, nodes[i][j], nodes[i][j + 1], 1);
            }
        }
        for (int i = 0; i < labyrinth.length - 1; i++) {
            for (int j = 0; j < labyrinth[0].length(); j++) {
                if (nodes[i][j] == null || nodes[i + 1][j] == null) {
                    continue;
                }
                Graphs.addEdge(graph, nodes[i][j], nodes[i + 1][j], 1);
            }
        }
        System.out.println("StartNode: " + startNode);
        System.out.println("EndNode: " + endNode);

        AStarModel aStarModel = new AStarModel(graph);
        GraphPath<AStarNode, DefaultWeightedEdge> shortestPath = aStarModel.getShortestPath(startNode, endNode);
        System.out.println(shortestPath.getWeight());  // 12
        System.out.println(shortestPath.getLength());  // 12
        System.out.println(shortestPath.getVertexList());  // [(6,0),(5,0),(4,0),(3,0),(2,0),(1,0),(0,0),(0,1),(0,2),(0,3),(0,4),(0,5),(0,6)]
    }
}

class AStarNode {
    private final int x;
    private final int y;

    AStarNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}

/**
 * 曼哈顿距离
 */
class ManhattanDistance implements AStarAdmissibleHeuristic<AStarNode> {
    @Override
    public double getCostEstimate(AStarNode sourceVertex, AStarNode targetVertex) {
        return Math.abs(sourceVertex.getX() - targetVertex.getX()) + Math.abs(sourceVertex.getY() - targetVertex.getY());
    }

    @Override
    public <E> boolean isConsistent(Graph<AStarNode, E> graph) {
        return true;
    }
}
