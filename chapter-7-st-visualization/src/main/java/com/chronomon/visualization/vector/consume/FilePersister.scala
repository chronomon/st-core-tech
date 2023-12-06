package com.chronomon.visualization.vector.consume

import com.chronomon.visualization.vector.feature.TileCoord

import java.io.{File, FileOutputStream}

class FilePersister(basePath: String) extends TilePersister {

  override def persist(tileCoord: TileCoord, tileBytes: Array[Byte]): Unit = {
    val filePath = basePath + File.separator + tileCoord.zoomLevel + File.separator + tileCoord.rowNum + File.separator + tileCoord.columnNum + ".pbf"
    val file = new File(filePath)
    if (!file.getParentFile.exists) file.getParentFile.mkdirs
    if (!file.exists) file.createNewFile
    val outputStream = new FileOutputStream(file)
    outputStream.write(tileBytes)
    outputStream.close()
    println("save tile:" + tileCoord.toString)
  }
}
