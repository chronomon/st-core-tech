package com.chronomon.address.tree;

import java.util.*;

/**
 * 树
 *
 * @param <T> 节点数据类型
 * @author yuzisheng
 * @date 2023-11-09
 */
public class Tree<T> {
    /**
     * 根节点
     */
    private final Node<T> rootNode;
    /**
     * 建立节点唯一标识到节点的映射
     */
    private final Map<String, Node<T>> idToNodeMap = new HashMap<>();

    /**
     * 初始化构造方法
     *
     * @param rootNode 根节点
     */
    public Tree(Node<T> rootNode) {
        this.rootNode = rootNode;
        this.rootNode.setChildNodes(new LinkedList<>());
        idToNodeMap.put(rootNode.getNodeId(), rootNode);
    }

    /**
     * 获取根节点
     *
     * @return 根节点
     */
    public Node<T> getRootNode() {
        return rootNode;
    }

    /**
     * 获取所有节点
     */
    public List<Node<T>> getNodes() {
        return new ArrayList<>(idToNodeMap.values());
    }

    /**
     * 获取节点数
     */
    public int getNodeNum() {
        return idToNodeMap.size();
    }

    /**
     * 判断是否包含该节点
     *
     * @param nodeId 节点唯一标识
     * @return 若树包含该节点则返回true，否则返回false
     */
    public boolean hasNode(String nodeId) {
        return idToNodeMap.containsKey(nodeId);
    }

    /**
     * 根据节点唯一标识获取节点
     *
     * @param nodeId 节点唯一标识
     * @return 对应的节点
     */
    public Node<T> getNode(String nodeId) {
        return idToNodeMap.get(nodeId);
    }

    /**
     * 新增节点
     *
     * @param node 待新增的节点
     */
    public void addNode(Node<T> node) {
        // NOTE：根节点不可替换，且若节点重复，则新节点会覆盖旧节点
        if (!node.getNodeId().equals(rootNode.getNodeId())) {
            idToNodeMap.put(node.getNodeId(), node);
        }
    }

    /**
     * 删除节点
     *
     * @param nodeId 待删除的节点
     */
    public void deleteNode(String nodeId) {
        if (!idToNodeMap.containsKey(nodeId) || nodeId.equals(rootNode.getNodeId())) {
            return;
        }

        Node<T> node = getNode(nodeId);
        Node<T> parentNode = node.getParentNode();
        idToNodeMap.remove(nodeId);
        parentNode.getChildNodes().remove(node);
    }

    /**
     * 计算节点深度，默认根节点深度为1，依次递增
     *
     * @param nodeId 节点唯一标识
     * @return 节点深度
     */
    public int getNodeLevel(String nodeId) {
        Node<T> node = getNode(nodeId);
        int depth = 1;
        while (true) {
            node = node.getParentNode();
            if (node == null) {
                break;
            }
            depth += 1;
        }
        return depth;
    }

    /**
     * 搜索指定节点与根节点的路径
     *
     * @param nodeId 指定节点
     * @return 指定节点与根节点之间的路径
     */
    public List<Node<T>> findPathToRoot(String nodeId) {
        return findPath(nodeId, rootNode.getNodeId());
    }

    /**
     * 判断指定节点是否为叶子节点
     *
     * @param nodeId 指定节点
     * @return 若为叶子节点则返回true，否则返回false
     */
    public Boolean isLeaf(String nodeId) {
        Node<T> node = getNode(nodeId);
        List<Node<T>> children = node.getChildNodes();
        return children == null || children.isEmpty();
    }

    /**
     * 判断两节点是否为父子关系
     *
     * @param parentNodeId 父节点
     * @param childNodeId  子节点
     * @return 若为父子则返回true，否则返回false
     */
    public boolean isParentAndChild(String parentNodeId, String childNodeId) {
        Node<T> parentNode = getNode(parentNodeId);
        Node<T> childNode = getNode(childNodeId);
        if (parentNode == null || childNode == null) {
            return false;
        }

        List<Node<T>> childPathToRoot = findPathToRoot(childNodeId);
        for (int i = 0; i < childPathToRoot.size() - 1; i++) {
            if (childPathToRoot.get(i).getNodeId().equals(parentNodeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 搜寻两节点之间的路径
     *
     * @param nodeId1 节点1
     * @param nodeId2 节点2
     * @return 节点1与节点2之间的路径，若路径不存在则返回空列表
     */
    public List<Node<T>> findPath(String nodeId1, String nodeId2) {
        ArrayList<Node<T>> path = new ArrayList<>();
        // 计算节点层级，同时判断节点是否存在
        int nodeLevel1 = getNodeLevel(nodeId1);
        int nodeLevel2 = getNodeLevel(nodeId2);
        // 若两节点相同则直接返回该节点作为路径
        if (nodeId1.equals(nodeId2)) {
            path.add(getNode(nodeId1));
            return path;
        }
        // 若两节点不同且位于同一层级则路径必不存在
        if (nodeLevel1 == nodeLevel2) {
            return path;
        }

        // 判断起点和终点
        Node<T> startNode;
        Node<T> endNode;
        if (nodeLevel1 < nodeLevel2) {
            startNode = getNode(nodeId1);
            endNode = getNode(nodeId2);
        } else {
            startNode = getNode(nodeId2);
            endNode = getNode(nodeId1);
        }
        // 路径查询原理：树中的两节点最多仅存在一条路径，自底向上搜索即可
        boolean isPathExist = false;
        path.add(endNode);
        while (true) {
            endNode = endNode.getParentNode();
            if (endNode == null) {
                break;
            }
            if (endNode.getNodeId().equals(startNode.getNodeId())) {
                path.add(startNode);
                isPathExist = true;
                break;
            }
            path.add(endNode);
        }
        if (!isPathExist) {
            path.clear();
        }
        // 反转路径为自顶向下
        Collections.reverse(path);
        return path;
    }

    /**
     * 搜索多个节点之间的最优路径，规则：路径最长且包含给定节点数最多
     *
     * @param nodeIds 节点列表
     * @return 多个节点之间的最优路径
     */
    public List<Node<T>> findBestPath(List<String> nodeIds) {
        // 去除重复节点以避免不必要计算
        ArrayList<String> noDupNodeIds = new ArrayList<>();
        for (String nodeId : nodeIds) {
            if (!noDupNodeIds.contains(nodeId)) {
                noDupNodeIds.add(nodeId);
            }
        }

        List<Node<T>> finalPath = new ArrayList<>();
        int finalNodeNumInPath = 0;
        for (int i = 0; i < noDupNodeIds.size(); i++) {
            for (int j = i; j < noDupNodeIds.size(); j++) {
                List<Node<T>> path = findPath(noDupNodeIds.get(i), noDupNodeIds.get(j));
                int nodeNumInPath = 0;
                for (Node<T> node : path) {
                    if (noDupNodeIds.contains(node.getNodeId())) {
                        nodeNumInPath += 1;
                    }
                }
                if (path.size() > finalPath.size() || (path.size() == finalPath.size() && nodeNumInPath > finalNodeNumInPath)) {
                    finalPath = path;
                    finalNodeNumInPath = nodeNumInPath;
                }
            }
        }
        return finalPath;
    }

    /**
     * 指定父节点，搜索子树下多个节点之间的最优全路径，规则：从上到下逐层匹配，直至匹配失败
     *
     * @param parentNodeId 父节点
     * @param nodeIds      节点列表
     * @return 多个节点之间的最优全路径
     */
    public List<Node<T>> findFullPathInSubTree(String parentNodeId, List<String> nodeIds) {
        Node<T> parentNode = getNode(parentNodeId);
        if (parentNode == null) {
            return null;
        }

        ArrayList<String> childrenNodeIds = new ArrayList<>();
        for (String nodeId : nodeIds) {
            if (isParentAndChild(parentNodeId, nodeId) && !childrenNodeIds.contains(nodeId)) {
                childrenNodeIds.add(nodeId);
            }
        }

        List<Node<T>> finalFullPath = new ArrayList<>();
        for (String childrenNodeId : childrenNodeIds) {
            List<Node<T>> path = findPath(parentNodeId, childrenNodeId);
            // NOTE：判断该路径是否为全节点匹配路径
            boolean isFullMatchPath = path != null && path.size() >= 2;
            if (!isFullMatchPath) {
                continue;
            }
            for (int i = 1; i < path.size(); i++) {
                if (!childrenNodeIds.contains(path.get(i).getNodeId())) {
                    isFullMatchPath = false;
                    break;
                }
            }
            if (isFullMatchPath && path.size() > finalFullPath.size()) {
                finalFullPath = path;
            }
        }
        return finalFullPath;
    }

    /**
     * 获取下一层级的子节点
     *
     * @param parentNodeId 父节点唯一标识
     * @return 下一层级的子节点
     */
    public List<Node<T>> getChildrenNextLevel(String parentNodeId) {
        if (!idToNodeMap.containsKey(parentNodeId)) {
            throw new RuntimeException("该父节点不存在：" + parentNodeId);
        }
        int parentNodeLevel = getNodeLevel(parentNodeId);
        return getChildrenByLevel(parentNodeId, parentNodeLevel + 1);
    }

    /**
     * 获取指定层级的子节点，若指定层级大于父节点层级，则返回空
     *
     * @param parentNodeId  父节点唯一标识
     * @param childrenLevel 子节点层级
     * @return 指定层级的子节点
     */
    public List<Node<T>> getChildrenByLevel(String parentNodeId, Integer childrenLevel) {
        ArrayList<Node<T>> childrenInSpecifiedLevel = new ArrayList<>();
        if (!idToNodeMap.containsKey(parentNodeId)) {
            return childrenInSpecifiedLevel;
        }

        Node<T> parentNode = getNode(parentNodeId);
        int parentNodeLevel = getNodeLevel(parentNodeId);
        // 若父节点层级大于等于子节点，则返回空节点
        if (parentNodeLevel >= childrenLevel) {
            return childrenInSpecifiedLevel;
        }

        // 层序遍历获取指定层级的子节点
        Queue<Node<T>> queue = new LinkedList<>();
        queue.offer(parentNode);
        while (!queue.isEmpty()) {
            parentNodeLevel += 1;
            if (parentNodeLevel > childrenLevel) {
                break;
            }
            int queueSize = queue.size();
            for (int i = 0; i < queueSize; i++) {
                Node<T> node = queue.poll();
                for (Node<T> child : node.getChildNodes()) {
                    queue.offer(child);
                    if (parentNodeLevel == childrenLevel) {
                        childrenInSpecifiedLevel.add(child);
                    }
                }
            }
        }
        return childrenInSpecifiedLevel;
    }
}
