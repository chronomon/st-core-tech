package com.chronomon.tile.vector.index

import com.chronomon.tile.vector.feature.{TileCoord, TileInfo}
import org.locationtech.jts.geom.Envelope

import scala.collection.mutable.ArrayBuffer

class QuadNode(val tileInfo: TileInfo) extends Serializable {
  private[index] var isBalance: Boolean = true
  protected val subnodes = new Array[QuadNode](4)

  def assign(samples: Array[Envelope]): Array[(QuadNode, Array[Envelope])] = {
    dive()
    Array(0, 1, 2, 3).map(childId => {
      val subEnv = subnodes(childId).tileInfo.env
      val subSamples = for (sample <- samples if subEnv.intersects(sample)) yield sample
      (subnodes(childId), subSamples)
    })
  }

  /**
   * Split quad node recursively until it's zoom level equals maxZoomLevel
   * binding row and column number for each quad node, using (0,0) refer to top left corner
   *
   * @param bottomZoomLevel bottom zoom level
   */
  def split(bottomZoomLevel: Int, leafNodes: ArrayBuffer[QuadNode]): Unit = {
    if (tileInfo.tileCoord.zoomLevel < bottomZoomLevel) {
      dive()
      for (index <- 0 until 4) {
        subnodes(index).split(bottomZoomLevel, leafNodes)
      }
    } else {
      leafNodes += this
    }
  }

  /**
   * Dive current quad node to next lower level
   */
  private def dive(): Unit = {
    for (childId <- 0 until 4) {
      subnodes(childId) = createNode(this.tileInfo.child(childId))
    }
  }

  /**
   * create a quad node
   *
   * @param tileInfo tile info represented by this quad node
   * @return a new quad node or its implement object
   */
  protected def createNode(tileInfo: TileInfo): QuadNode = new QuadNode(tileInfo)

  def intersection(geomEnv: Envelope, collector: ArrayBuffer[Int]): Unit = {
    if (tileInfo.env.intersects(geomEnv)) {
      if (isLeaf) {
        if (isBalance) collector += this.tileInfo.tileId
      } else {
        for (index <- 0 until 4) {
          subnodes(index).intersection(geomEnv, collector)
        }
      }
    }
  }

  protected def isLeaf: Boolean = subnodes.head == null
}

object QuadNode {
  /**
   * Tight the root as small as possible while conforming that root covers the whole dataset
   *
   * @param globalBound global bound of dataset which will be sliced
   * @param crsBound    bound of coordinate reference system, which covers the globalBound
   * @return a minimum quad node which covers the dataset
   */
  def tightRoot(globalBound: Envelope, crsBound: Envelope): QuadNode = {
    var root = new QuadNode(TileInfo(TileCoord(0, 0, 0), crsBound, 0))
    var subNode: Option[QuadNode] = Some(root)
    do {
      root = subNode.get
      root.dive()
      subNode = root.subnodes.find(_.tileInfo.env.contains(globalBound))
    } while (subNode.nonEmpty)
    root
  }
}