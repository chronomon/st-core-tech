package com.chronomon.tile.vector.feature

case class TileCoord(rowNum: Int, columnNum: Int, zoomLevel: Int) {

  def parent: TileCoord = TileCoord(rowNum >> 1, columnNum >> 1, zoomLevel - 1)

  def child(childId: Int): TileCoord = {
    val upperRowNum = rowNum << 1
    val upperColumnNum = columnNum << 1
    childId match {
      case 0 => TileCoord(upperRowNum + 1, upperColumnNum, zoomLevel + 1)
      case 1 => TileCoord(upperRowNum + 1, upperColumnNum + 1, zoomLevel + 1)
      case 2 => TileCoord(upperRowNum, upperColumnNum, zoomLevel + 1)
      case 3 => TileCoord(upperRowNum, upperColumnNum + 1, zoomLevel + 1)
    }
  }

  override def toString: String = {
    s"$zoomLevel-$rowNum-$columnNum"
  }
}
