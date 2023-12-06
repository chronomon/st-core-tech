package com.chronomon.visualization.vector.consume

import com.chronomon.visualization.vector.feature.TileCoord

trait TilePersister extends Serializable {
  def persist(tileCoord: TileCoord, tileBytes: Array[Byte]): Unit
}
