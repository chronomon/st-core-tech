package com.chronomon.tile.vector.crs

import org.locationtech.jts.geom.{Envelope, Geometry}

trait VectorCRS extends Serializable {
  /**
   * convert spatial lng to projection lng
   *
   * @param lng longitude
   * @return projection x
   */
  protected def toProjectX(lng: Double): Double

  /**
   * convert spatial lat to projection lat
   *
   * @param lat latitude
   * @return projection y
   */
  protected def toProjectY(lat: Double): Double

  /**
   * Get tile boundary in the projection coordinate system
   */
  def projectBound: Envelope

  /**
   * convert geometry's geographic coordinates to projection coordinates
   *
   * @param geom geometry
   */
  def asProject(geom: Geometry): Unit = {
    geom.getCoordinates.foreach(coord => {
      coord.setX(toProjectX(coord.getX))
      coord.setY(toProjectY(coord.getY))
    })
    geom.geometryChanged()
  }
}
