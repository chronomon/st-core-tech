package com.chronomon.analysis.address.tree;

import java.util.LinkedList;
import java.util.List;

/**
 * 节点
 *
 * @param <T> 节点数据类型
 * @author yuzisheng
 * @date 2023-11-09
 */
public class Node<T> {
    /**
     * 节点唯一标识
     */
    private final String nodeId;
    /**
     * 节点数据
     */
    private final T nodeData;
    /**
     * 父节点
     */
    private Node<T> parentNode;
    /**
     * 子节点列表
     */
    private List<Node<T>> childNodes;

    public Node(String nodeId, T nodeData) {
        this.nodeId = nodeId;
        this.nodeData = nodeData;
        this.childNodes = new LinkedList<>();
    }

    public T getNodeData() {
        return nodeData;
    }

    public String getNodeId() {
        return nodeId;
    }

    public Node<T> getParentNode() {
        return parentNode;
    }

    public List<Node<T>> getChildNodes() {
        return childNodes;
    }

    public void setParentNode(Node<T> parentNode) {
        this.parentNode = parentNode;
    }

    public void setChildNodes(List<Node<T>> childNodes) {
        this.childNodes = childNodes;
    }

    /**
     * 添加子节点
     *
     * @param childNode 子节点
     */
    public void addChildNode(Node<T> childNode) {
        childNode.setParentNode(this);
        this.childNodes.add(childNode);
    }
}
