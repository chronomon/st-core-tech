package com.chronomon.tile.vector

import com.chronomon.tile.vector.consume.{LayerInfo, TileGenerator, TilePersister}
import com.chronomon.tile.vector.crs.VectorCRS
import com.chronomon.tile.vector.feature.WrapFeature
import org.apache.spark.rdd.RDD

private[vector] class LayerPreparer(crs: VectorCRS, persiter: TilePersister) extends Serializable {
  def combine(layers: Array[(LayerInfo, RDD[WrapFeature])]): (RDD[WrapFeature], TileGenerator) = {
    val layerInfos = new Array[LayerInfo](layers.length)
    //Bind layer id
    val layerWithId = layers.zipWithIndex.map {
      case ((layerInfo, layerRdd), layerId) =>
        layerInfo.setLayerId(layerId.toByte)
        layerInfos(layerId) = layerInfo
        (layerId, layerRdd)
    }
    val combineLayer = projectAndCombine(layerWithId)
    val tileGenerator = new TileGenerator(layerInfos, persiter)
    (combineLayer, tileGenerator)
  }

  private def projectAndCombine(layerWithId: Array[(Int, RDD[WrapFeature])]): RDD[WrapFeature] = {
    //Convert to projection coordinate
    layerWithId.map {
      case (layerId, layerRdd) =>
        layerRdd.map(feature => {
          crs.asProject(feature.geom)
          feature.layerId = layerId.toByte
          feature.projectEnv = feature.geom.getEnvelopeInternal
          feature
        })
    }.reduce((rdd1, rdd2) => {
      rdd1.union(rdd2)
    })
  }
}
