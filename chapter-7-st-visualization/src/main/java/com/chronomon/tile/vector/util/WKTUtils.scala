package com.chronomon.tile.vector.util

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.{WKTReader, WKTWriter}

object WKTUtils {
  private[this] val readerPool = new ThreadLocal[WKTReader] {
    override def initialValue: WKTReader = new WKTReader
  }
  private[this] val writerPool = new ThreadLocal[WKTWriter] {
    override def initialValue: WKTWriter = new WKTWriter
  }

  def read(s: String): Geometry = readerPool.get.read(s)

  def write(g: Geometry): String = writerPool.get.write(g)
}