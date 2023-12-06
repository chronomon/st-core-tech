package com.chronomon.visualization.vector.consume

import TileGenerator.EXTENT
import com.chronomon.visualization.vector.feature.{TileCoord, WrapFeature}
import com.chronomon.visualization.vector.source.FixedVectorTileEncoder

import scala.collection.JavaConverters.mapAsJavaMapConverter

private[vector] class TileGenerator(layerInfos: Array[LayerInfo], persister: TilePersister) extends Serializable {

  def maxLevel: Int = {
    layerInfos.map(_.zoomBound.maxLevel).max
  }

  def minLevel: Int = {
    layerInfos.map(_.zoomBound.minLevel).min
  }

  def generateTile(tileCoord: TileCoord, tileFeatures: Array[WrapFeature]): Unit = {
    val validFeatures = tileFeatures.filter(_.active)
    if (validFeatures.nonEmpty) {
      persister.persist(tileCoord, encode(validFeatures))
    }
  }

  /**
   * 清除上层显示不需要的要素
   *
   * @param tileZoomLevel 当前图层级别
   * @param tileFeatures  要素
   * @return
   */
  def reduceFeatures(tileZoomLevel: Int, tileFeatures: Array[WrapFeature]): Array[WrapFeature] = {
    tileFeatures.filter(feature => {
      val layerInfo = layerInfos(feature.layerId)
      val meet = if (layerInfo.gradientFunc != null) {
        // 如果有属性层级限制，则按照属性值来判断要素是否保留
        layerInfo.gradientFunc(feature).minLevel < tileZoomLevel
      } else {
        // 如果没有属性层级限制，则按照图层的层级限制来判断要素是否保留
        layerInfo.zoomBound.minLevel < tileZoomLevel
      }
      if (!meet) {
        feature.active = false
      }
      meet
    })
  }

  private def encode(tileFeatures: Array[WrapFeature]): Array[Byte] = {
    val encoder = new FixedVectorTileEncoder(EXTENT, 8, false, true)
    tileFeatures.foreach(feature => {
      val layerInfo = layerInfos(feature.layerId)
      encoder.addFeature(layerInfo.layerName, layerInfo.attrFields.zip(feature.attributes).toMap.asJava, feature.geom)
    })
    encoder.encode()
  }
}

object TileGenerator {
  val EXTENT = 4096
}
