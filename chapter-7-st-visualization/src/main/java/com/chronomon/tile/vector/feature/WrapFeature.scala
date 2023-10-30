package com.chronomon.tile.vector.feature

import org.locationtech.jts.geom.{Envelope, Geometry}
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier

case class WrapFeature(var geom: Geometry, attributes: Array[_ <: Any]) {
  var layerId: Byte = _
  var projectEnv: Envelope = _
  var active: Boolean = true
  private var referPixelCoordinate: (Int, Int) = (0, 0)

  /**
   * change the refer pixel coordinate to the target one, and scroll to a smaller zoom level
   *
   * @param targetReferPixelCoord target reference pixel coordinate
   */
  def scrollUp(targetReferPixelCoord: (Int, Int)): Unit = {
    translateAndScroll(targetReferPixelCoord, (coordValue: Int) => coordValue >> 1)
    geom = DouglasPeuckerSimplifier.simplify(geom, 5.0D) // 道格拉斯扑克简化，基于像素坐标
    referPixelCoordinate = (0, 0)
  }

  /**
   * relocate the feature's reference pixel coordinate to the target pixel coordinate
   *
   * @param targetPixelCoord target reference pixel coordinate
   */
  def relocate(targetPixelCoord: (Int, Int)): Unit = {
    if (targetPixelCoord._1 == referPixelCoordinate._1 &&
      targetPixelCoord._2 == referPixelCoordinate._2) return
    translateAndScroll(targetPixelCoord)
  }

  /**
   * relocate the reference pixel coordinate to (0,0) and scroll to a smaller zoom level
   */
  def scrollUp(): Unit = {
    translateAndScroll((0, 0), (coordValue: Int) => coordValue >> 1)
    geom = DouglasPeuckerSimplifier.simplify(geom, 5.0D) // 道格拉斯扑克简化，基于像素坐标
  }

  /**
   * First translate the coordinate in the same zoom level, and then scroll the the smaller zoom level
   *
   * @param targetPixelCoord target pixel coordinate
   * @param zoomTransform    function for scroll coordinates to the smaller zoom level
   */
  private def translateAndScroll(targetPixelCoord: (Int, Int),
                                 zoomTransform: Int => Int = (coordValue: Int) => coordValue): Unit = {

    val deltaPixelX = targetPixelCoord._1 - referPixelCoordinate._1
    val deltaPixelY = targetPixelCoord._2 - referPixelCoordinate._2
    geom.getCoordinates.foreach(pixelCoord => {
      pixelCoord.setX(zoomTransform(pixelCoord.getX.toInt - deltaPixelX))
      pixelCoord.setY(zoomTransform(pixelCoord.getY.toInt - deltaPixelY))
    })
    geom.geometryChanged()
    referPixelCoordinate = targetPixelCoord
  }
}
