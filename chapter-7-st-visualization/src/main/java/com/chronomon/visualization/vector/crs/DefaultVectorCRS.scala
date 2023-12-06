package com.chronomon.visualization.vector.crs

import org.locationtech.jts.geom.Envelope

object DefaultVectorCRS extends VectorCRS {
  /**
   * web mercator parameter
   */
  private val ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0

  /**
   * convert spatial lng to projection lng
   *
   * @param lng longitude
   * @return mercator projection x with unit 'meter'
   */
  override protected def toProjectX(lng: Double): Double = {
    lng * ORIGIN_SHIFT / 180.0
  }

  /**
   * convert spatial lat to projection lat
   *
   * @param lat latitude
   * @return mercator projection y with unit 'meter'
   */
  override protected def toProjectY(lat: Double): Double = {
    val projectLat = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0)
    projectLat * ORIGIN_SHIFT / 180
  }

  /**
   * Get tile boundary in the projection coordinate system
   */
  override def projectBound: Envelope = new Envelope(-20037508.3427892, 20037508.3427892, -20037508.3427892, 20037508.3427892)
}
