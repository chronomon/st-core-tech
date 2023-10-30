package com.chronomon.tile.vector.index

import org.apache.spark.Partitioner
import org.locationtech.jts.geom.Envelope

import scala.collection.mutable.ArrayBuffer

/**
 * Quad tree index for spatial partition
 *
 * @param root global spatial boundary of dataset
 */
class QuadTree(root: QuadNode) extends Serializable {
  private var leafNodes: Array[QuadNode] = _

  def build(samples: Array[Envelope], partitionDeep: Int): Array[(QuadNode, Array[Envelope])] = {
    var leafNodeWithSamples = Array((root, samples))
    var step = partitionDeep
    while (step > 0) {
      leafNodeWithSamples = leafNodeWithSamples.flatMap {
        case (node, samples) =>
          node.assign(samples)
      }
      step -= 1
    }
    val sampleThreshold = Math.max((samples.length / leafNodeWithSamples.length) * 5, 5000)
    val bankedNodeWithSamples = new ArrayBuffer[(QuadNode, Array[Envelope])]()
    this.leafNodes = leafNodeWithSamples.map {
      case nodeWithSample@(leafNode, samples) =>
        if (samples.length > sampleThreshold) {
          bankedNodeWithSamples += nodeWithSample
          leafNode.isBalance = false
        }
        leafNode
    }
    bankedNodeWithSamples.toArray
  }

  def leafNode(partitionId: Int): QuadNode = leafNodes(partitionId)

  def intersectBottomTiles(geomEnv: Envelope): Array[Int] = {
    val tileIdCollector = new ArrayBuffer[Int]()
    root.intersection(geomEnv, tileIdCollector)
    tileIdCollector.toArray
  }

  def getPartitioner: Partitioner = {
    val partitionNum = leafNodes.length
    new Partitioner {
      override def numPartitions: Int = partitionNum

      override def getPartition(key: Any): Int = key.asInstanceOf[Int]
    }
  }
}
