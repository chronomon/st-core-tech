package com.chronomon.visualization.vector.feature

import org.locationtech.jts.geom.Envelope

case class TileInfo(tileCoord: TileCoord, env: Envelope, tileId: Int) {
  private val centre = env.centre()

  def parent(parentEnv: Envelope): TileInfo = {
    val parentTileId = tileId >> 2
    TileInfo(tileCoord.parent, parentEnv, parentTileId)
  }

  def child(childId: Int): TileInfo = {
    TileInfo(tileCoord.child(childId), createSubEnv(childId), (tileId << 2) + childId)
  }

  /**
   * Get sub node's envelope of the specific index
   * 2  /  3
   * 0  /  1
   *
   * @param childId index of child
   * @return sub node's envelope
   */
  private def createSubEnv(childId: Int): Envelope = {
    var minx = 0.0
    var maxx = 0.0
    var miny = 0.0
    var maxy = 0.0

    childId match {
      case 0 =>
        minx = env.getMinX
        maxx = centre.getX
        miny = env.getMinY
        maxy = centre.getY
      case 1 =>
        minx = centre.getX
        maxx = env.getMaxX
        miny = env.getMinY
        maxy = centre.getY
      case 2 =>
        minx = env.getMinX
        maxx = centre.getX
        miny = centre.getY
        maxy = env.getMaxY
      case 3 =>
        minx = centre.getX
        maxx = env.getMaxX
        miny = centre.getY
        maxy = env.getMaxY
    }
    new Envelope(minx, maxx, miny, maxy)
  }
}