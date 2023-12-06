package com.chronomon.visualization.vector.consume

import com.chronomon.visualization.vector.feature.WrapFeature

case class LayerInfo(layerName: String, zoomBound: ZoomBound, attrFields: Array[String], gradientFunc: WrapFeature => ZoomBound = null) {
  private var layerId: Byte = _

  def setLayerId(layerId: Byte): Unit = {
    this.layerId = layerId
  }

  def getLayerId: Byte = {
    require(null != layerId, "layer id can't be null")
    layerId
  }
}
