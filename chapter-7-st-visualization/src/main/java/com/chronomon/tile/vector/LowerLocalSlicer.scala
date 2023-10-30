package com.chronomon.tile.vector

import com.chronomon.tile.vector.consume.TileGenerator
import com.chronomon.tile.vector.consume.TileGenerator.EXTENT
import com.chronomon.tile.vector.feature.{TileCoord, TileInfo, WrapFeature}
import com.chronomon.tile.vector.index.QuadNode
import org.locationtech.jts.geom.Envelope

import scala.collection.mutable.ArrayBuffer

class LowerLocalSlicer[T](localRoot: QuadNode, consumer: TileGenerator) {
  private val minZoomLevel = consumer.minLevel
  private val maxZoomLevel = consumer.maxLevel
  private val root = new QuadNodeWithData(localRoot.tileInfo)

  def slice(projectFeatures: Array[WrapFeature]): (TileInfo, Array[WrapFeature]) = {
    val pixelFeatures = asPixelAndBuild(projectFeatures)
    val topLevel = Math.max(minZoomLevel, root.tileCoord.zoomLevel)
    for (zoomLevel <- (topLevel to maxZoomLevel).reverse) {
      val deltaZoomLevel = zoomLevel - root.tileCoord.zoomLevel
      val minRowNum = root.tileCoord.rowNum << deltaZoomLevel
      val minColumnNum = root.tileCoord.columnNum << deltaZoomLevel
      root.generateTile(minRowNum, minColumnNum, zoomLevel)
      // reduceFeatures会将上级不会出现的Feature打上active=false的tag
      consumer.reduceFeatures(zoomLevel, pixelFeatures)
      // 对于上级已经不需要的要素，不在执行scrollUp操作
      if (zoomLevel != topLevel) pixelFeatures.filter(_.active).foreach(_.scrollUp())
    }
    if (topLevel > minZoomLevel) {
      (root.tileInfo, pixelFeatures.filter(_.active))
    } else null
  }

  private def asPixelAndBuild(projectFeatures: Array[WrapFeature]): Array[WrapFeature] = {
    //build quadtree index
    val bottomLevel = Math.max(root.tileInfo.tileCoord.zoomLevel, consumer.maxLevel)
    val leafNodes = new ArrayBuffer[QuadNode]()
    root.split(bottomLevel, leafNodes)

    //convert features to pixel coordinates and insert into quadtree
    val referX = root.tileInfo.env.getMinX
    val referY = root.tileInfo.env.getMaxY
    val tileWidth = leafNodes.head.tileInfo.env.getWidth
    val tileHeight = leafNodes.head.tileInfo.env.getHeight
    projectFeatures.map(feature => {
      //convert projection coordinates to pixel coordinates
      feature.geom.getCoordinates.foreach(coord => {
        val pixelX = Math.round((coord.getX - referX) / tileWidth * EXTENT)
        val pixelY = Math.round((referY - coord.getY) / tileHeight * EXTENT)
        coord.setX(pixelX)
        coord.setY(pixelY)
        coord
      })
      feature.geom.geometryChanged()
      root.insert(feature.projectEnv, feature)
      feature
    })
  }

  private class QuadNodeWithData(tileInfo: TileInfo) extends QuadNode(tileInfo) {
    val tileCoord: TileCoord = tileInfo.tileCoord
    val wrapFeatures = new ArrayBuffer[WrapFeature]()

    override protected def createNode(tileInfo: TileInfo): QuadNode = {
      new QuadNodeWithData(tileInfo)
    }

    def insert(geomEnv: Envelope, wrappedFeature: WrapFeature): Unit = {
      if (this.tileInfo.env.intersects(geomEnv)) {
        this.wrapFeatures += wrappedFeature
        if (!isLeaf) {
          for (subIndex <- 0 until 4) {
            this.subnodes(subIndex).asInstanceOf[QuadNodeWithData].insert(geomEnv, wrappedFeature)
          }
        }
      }
    }

    def generateTile(minRow: Int, minColumn: Int, zoomLevel: Int): Unit = {
      if (this.tileCoord.zoomLevel == zoomLevel) {
        val currMinPixelX = (this.tileCoord.columnNum - minColumn) * EXTENT
        val currMinPixelY = (this.tileCoord.rowNum - minRow) * EXTENT
        wrapFeatures.foreach(wrapFeature => {
          wrapFeature.relocate(currMinPixelX, currMinPixelY)
        })
        consumer.generateTile(tileInfo.tileCoord, wrapFeatures.toArray)
      } else {
        for (subIndex <- 0 until 4) {
          this.subnodes(subIndex).asInstanceOf[QuadNodeWithData].generateTile(minRow, minColumn, zoomLevel)
        }
      }
    }
  }
}
