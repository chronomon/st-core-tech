package com.chronomon.analysis.path;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * 贪婪最佳优先搜索
 *
 * @author yuzisheng
 * @date 2023-12-07
 */
public class GBFSModel {

    private final SimpleGraph<GBFSNode, DefaultEdge> graph;

    GBFSModel(SimpleGraph<GBFSNode, DefaultEdge> graph) {
        this.graph = graph;
    }

    /**
     * 当前节点到目标节点的代价
     */
    public double hn(GBFSNode currentNode, GBFSNode targetNode) {
        // 这里使用欧式距离，也可使用曼哈顿距离
        // return Math.abs(currentNode.getX() - targetNode.getX()) + Math.abs(currentNode.getY() - targetNode.getY());
        return Math.sqrt(Math.pow(currentNode.getX() - targetNode.getX(), 2) + Math.pow(currentNode.getY() - targetNode.getY(), 2));
    }

    public List<GBFSNode> getPath(GBFSNode startNode, GBFSNode endNode) {
        List<GBFSNode> path = new ArrayList<>();
        path.add(startNode);
        List<GBFSNode> nodes = Graphs.neighborListOf(graph, startNode);
        while (!nodes.isEmpty()) {
            if (nodes.contains(endNode)) {
                path.add(endNode);
                return path;
            }

            double minFn = Double.MAX_VALUE;
            GBFSNode nextNode = null;
            for (GBFSNode node : nodes) {
                double fn = hn(node, endNode);
                if (fn < minFn) {
                    minFn = fn;
                    nextNode = node;
                }
            }
            path.add(nextNode);
            nodes = Graphs.neighborListOf(graph, nextNode);
        }

        return null;
    }

    public static void main(String[] args) {
        String[] grid = {
                "........",
                ".......E",
                "........",
                "........",
                "........",
                "........",
                "S.......",
                "........"};
        GBFSNode startNode = null;  // (6,0)
        GBFSNode endNode = null;  // (1,7)
        SimpleGraph<GBFSNode, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        GBFSNode[][] nodes = new GBFSNode[grid.length][grid[0].length()];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length(); j++) {
                nodes[i][j] = new GBFSNode(i, j);
                graph.addVertex(nodes[i][j]);
                if (grid[i].charAt(j) == 'S') {
                    startNode = nodes[i][j];
                } else if (grid[i].charAt(j) == 'E') {
                    endNode = nodes[i][j];
                }
            }
        }
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length() - 1; j++) {
                if (nodes[i][j] == null || nodes[i][j + 1] == null) {
                    continue;
                }
                graph.addEdge(nodes[i][j], nodes[i][j + 1]);
            }
        }
        for (int i = 0; i < grid.length - 1; i++) {
            for (int j = 0; j < grid[0].length(); j++) {
                if (nodes[i][j] == null || nodes[i + 1][j] == null) {
                    continue;
                }
                graph.addEdge(nodes[i][j], nodes[i + 1][j]);
            }
        }
        System.out.println("StartNode: " + startNode);
        System.out.println("EndNode: " + endNode);

        GBFSModel gbfsModel = new GBFSModel(graph);
        List<GBFSNode> path = gbfsModel.getPath(startNode, endNode);
        System.out.println(path.size() - 1);  // 12
        System.out.println(path);  // [(6,0),(6,1),(6,2),(6,3),(5,3),(5,4),(4,4),(4,5),(3,5),(3,6),(2,6),(2,7),(1,7)]
    }
}

class GBFSNode {
    private final int x;
    private final int y;

    GBFSNode(int x, int y) {
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