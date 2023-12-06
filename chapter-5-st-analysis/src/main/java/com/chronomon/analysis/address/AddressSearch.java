package com.chronomon.analysis.address;

import com.chronomon.analysis.address.tree.Node;
import com.chronomon.analysis.address.tree.Tree;
import lombok.Data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于滑窗模糊匹配的地址搜索
 *
 * @author yuzisheng
 * @date 2023-11-10
 */
public class AddressSearch {
    /**
     * 地理层级树
     */
    private Tree<Region> tree;
    /**
     * NGRAM索引
     */
    private Map<Integer, Map<String, Map<String, String>>> ngramIndex;
    /**
     * IDF索引
     */
    private Map<String, Double> idfIndex;
    /**
     * 最大滑窗大小
     */
    private final Integer MAX_WINDOW_SIZE = 10;

    /**
     * @param rootRegionId 根节点标识
     * @param regions      节点集合
     */
    AddressSearch(String rootRegionId, List<Region> regions) {
        buildTree(rootRegionId, regions);
        buildNgramIndex();
        buildIdfIndex();
    }

    /**
     * 基于滑窗模糊匹配的地址搜索
     */
    public Region search(String address) {
        // 第一步：匹配节点
        Map<String, MatchNode> matchedNodes = matchNode(address);

        // 第二步：匹配路径
        List<List<Node<Region>>> matchedPaths = matchPath(tree.getRootNode().getNodeId(), matchedNodes);

        // 第三步：计算分数
        for (List<Node<Region>> path : matchedPaths) {
            double pathScore = 0.0;
            for (Node<Region> node : path) {
                double nodeScore = calNodeScore(node, matchedNodes.get(node.getNodeId()));
                pathScore += nodeScore;
                matchedNodes.get(node.getNodeId()).setNodeScore(nodeScore);
                matchedNodes.get(node.getNodeId()).setPathScore(pathScore);
            }
        }

        // 第四步：排序返回
        List<Node<Region>> nodes = new ArrayList<>();
        matchedPaths.forEach(nodes::addAll);
        nodes = nodes.stream().distinct().collect(Collectors.toList());
        nodes.sort((n1, n2) -> matchedNodes.get(n2.getNodeId()).getPathScore().compareTo(matchedNodes.get(n1.getNodeId()).getPathScore()));
        return nodes.isEmpty() ? tree.getRootNode().getNodeData() : nodes.get(0).getNodeData();
    }

    /**
     * 构建地理层级树
     */
    private void buildTree(String rootRegionId, List<Region> regions) {
        Node<Region> rootNode = null;
        Map<String, Node<Region>> idToNodeMap = new HashMap<>();

        // 找到根节点，同时并建立相关索引
        for (Region region : regions) {
            Node<Region> node = new Node<>(region.getRegionId(), region);
            idToNodeMap.put(node.getNodeId(), node);
            if (node.getNodeId().equals(rootRegionId)) {
                rootNode = node;
            }
        }
        if (rootNode == null) {
            throw new RuntimeException("地理层级树构建失败：根节点未找到");
        }

        // 建立地址层级树，两步走：先加点，后加边
        tree = new Tree<>(rootNode);
        String rootNodeId = rootNode.getNodeId();
        idToNodeMap.forEach((id, node) -> {
            if (!id.equals(rootNodeId)) {
                tree.addNode(node);
            }
        });

        idToNodeMap.forEach((id, node) -> {
            String parentRegionId = node.getNodeData().getParentRegionId();
            if (idToNodeMap.containsKey(parentRegionId)) {
                idToNodeMap.get(parentRegionId).addChildNode(node);
            }
        });
    }

    /**
     * 构建滑窗分词索引
     */
    private void buildNgramIndex() {
        ngramIndex = new HashMap<>();
        for (int n = 2; n <= MAX_WINDOW_SIZE; n++) {
            ngramIndex.put(n, new HashMap<>());
        }
        Stack<Node<Region>> nodeStack = new Stack<>();
        nodeStack.add(tree.getRootNode());
        while (!nodeStack.isEmpty()) {
            Node<Region> node = nodeStack.pop();
            String nodeName = node.getNodeData().getRegionName();
            List<String> ngrams = getNgrams(nodeName);
            for (String ngram : ngrams) {
                int n = ngram.length();
                if (!ngramIndex.get(n).containsKey(ngram)) {
                    ngramIndex.get(n).put(ngram, new HashMap<>());
                }
                ngramIndex.get(n).get(ngram).put(node.getNodeData().getRegionId(), nodeName);
            }
            if (node.getChildNodes() != null) {
                for (Node<Region> child : node.getChildNodes()) {
                    nodeStack.push(child);
                }
            }
        }
    }

    /**
     * 计算GRAM的IDF
     */
    private void buildIdfIndex() {
        double idfAlpha = 10.0;
        for (Integer n : ngramIndex.keySet()) {
            Map<String, Map<String, String>> ngramToRegionMap = ngramIndex.get(n);
            for (String ngram : ngramToRegionMap.keySet()) {
                if (ngramToRegionMap.get(ngram).isEmpty()) {
                    return;
                }
            }
        }

        // 第一步：统计频次
        idfIndex = new HashMap<>();
        for (Map<String, Map<String, String>> sizeBucket : ngramIndex.values()) {
            for (Map.Entry<String, Map<String, String>> entry : sizeBucket.entrySet()) {
                idfIndex.put(entry.getKey(), (double) entry.getValue().size());
            }
        }

        // 第二步：计算IDF
        double maxX = tree.getNodeNum() / Collections.min(idfIndex.values());
        double minX = tree.getNodeNum() / Collections.max(idfIndex.values());
        idfIndex.forEach((ngram, count) -> {
            double x = tree.getNodeNum() / count;
            double normX = (x - minX) / maxX;
            idfIndex.replace(ngram, idfIndex.get(ngram), (2 / (1 + Math.exp(-idfAlpha * normX)) - 1));
        });
    }

    /**
     * 匹配节点
     */
    private Map<String, MatchNode> matchNode(String address) {
        Map<String, MatchNode> nodes = new HashMap<>();
        List<String> ngrams = getNgrams(address);
        for (String ngram : ngrams) {
            int n = ngram.length();
            if (ngramIndex.get(n).containsKey(ngram)) {
                Map<String, String> nodeIdToNameMap = ngramIndex.get(n).get(ngram);
                nodeIdToNameMap.forEach((nodeId, nodeName) -> {
                    if (!nodes.containsKey(nodeId)) {
                        nodes.put(nodeId, new MatchNode());
                    }

                    int start = nodeName.indexOf(ngram);
                    nodes.get(nodeId).getMatchIndex().addAll(
                            Stream.iterate(start, item -> item + 1).limit(ngram.length()).collect(Collectors.toSet())
                    );
                    nodes.get(nodeId).setMatchNgram(ngram);
                    nodes.get(nodeId).setNodeName(nodeName);
                });
            }
        }
        return nodes;
    }

    /**
     * 匹配路径
     */
    private List<List<Node<Region>>> matchPath(String parentRegionId, Map<String, MatchNode> matchedNodes) {
        List<List<Node<Region>>> paths = new ArrayList<>();
        List<String> lowerNodeIds = getLowerNodes(tree.getNode(parentRegionId), matchedNodes);
        for (String lowerNodeId : lowerNodeIds) {
            List<Node<Region>> path = new ArrayList<>();
            Node<Region> node = tree.getNode(lowerNodeId);
            while (node != null) {
                if (matchedNodes.containsKey(node.getNodeId())) {
                    path.add(node);
                }
                if (node.getNodeId().equals(parentRegionId)) {
                    break;
                }
                node = node.getParentNode();
            }
            Collections.reverse(path);
            paths.add(path);
        }
        return paths;
    }

    /**
     * 获取子树底层节点，其定义为：该节点被匹配，且子孙节点没有一个被匹配
     */
    private List<String> getLowerNodes(Node<Region> parentNode, Map<String, MatchNode> matchedNodes) {
        List<String> lowerNodeIds = new ArrayList<>();
        for (Node<Region> childNode : parentNode.getChildNodes()) {
            // 楼栋、单元、门牌需连续匹配
            if (childNode.getNodeData().getRegionLevel() > 6 && !matchedNodes.containsKey(parentNode.getNodeId())) {
                continue;
            }
            lowerNodeIds.addAll(getLowerNodes(childNode, matchedNodes));
        }

        if (lowerNodeIds.isEmpty() && matchedNodes.containsKey(parentNode.getNodeId())) {
            lowerNodeIds.add(parentNode.getNodeId());
        }
        return lowerNodeIds;
    }

    /**
     * 计算节点分数
     */
    private double calNodeScore(Node<Region> node, MatchNode matchNode) {
        double idf = idfIndex.get(matchNode.getMatchNgram());
        double[] weights = {0.0, 1.0, 1.0, 1.2, 1.0, 0.9, 1.0, 0.3, 0.3, 0.3};
        return weights[node.getNodeData().getRegionLevel()] * idf * matchNode.getMatchNgram().length() * Math.sqrt((double) matchNode.getMatchIndex().size() / matchNode.getNodeName().length());
    }

    /**
     * 滑窗分词
     */
    private List<String> getNgrams(String text) {
        List<String> ngrams = new ArrayList<>();
        if (text.length() < 2) {
            return ngrams;
        }

        for (int n = 2; n <= MAX_WINDOW_SIZE; n++) {
            for (int i = 0; i <= text.length() - n; i++) {
                ngrams.add(text.substring(i, i + n));
            }
        }
        return ngrams;
    }

    @Data
    static class MatchNode {
        /**
         * 最长匹配字符串
         */
        private String matchNgram;
        /**
         * 非连续匹配字符串下标
         */
        private Set<Integer> matchIndex = new HashSet<>();
        /**
         * 匹配节点名称
         */
        private String nodeName;
        /**
         * 匹配节点分数
         */
        private Double nodeScore;
        /**
         * 匹配路径分数
         */
        private Double pathScore;
    }

    public static void main(String[] args) throws Exception {
        // 省、市、区、街道、社区、小区、楼栋、单元、门牌号，层级分别为1-9
        String filePath = Objects.requireNonNull(AddressSearch.class.getResource("/tree.txt")).getPath();
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<Region> regions = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] split = line.split("\t");
            regions.add(new Region(split[0], split[1], split[2], Integer.parseInt(split[3]), split[4]));
        }

        AddressSearch model = new AddressSearch("dcd152a4163d48aabe016024b713033b", regions);
        Region result = model.search("达富雅园1号楼1单元101号");
        System.out.println(result.getRegionAddress());  // 北京市|北京市|通州区|北苑街道|玉带路社区|达富雅园|1号楼|1单元|101号
    }
}
