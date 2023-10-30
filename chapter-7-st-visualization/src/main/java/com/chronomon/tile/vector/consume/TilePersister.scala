package com.chronomon.tile.vector.consume

import com.chronomon.tile.vector.feature.TileCoord

trait TilePersister extends Serializable {
  def persist(tileCoord: TileCoord, tileBytes: Array[Byte]): Unit
}
