package com.chronomon.visualization.vector.source

import no.ecc.vectortile.VectorTileEncoder
import org.locationtech.jts.geom.{Geometry, GeometryCollection, LineString, MultiLineString, MultiPoint, MultiPolygon, Point, Polygon}

import scala.collection.mutable.ArrayBuffer

class FixedVectorTileEncoder(extent: Int, clipBuffer: Int, autoScale: Boolean, autoincrementIds: Boolean)
  extends VectorTileEncoder(extent, clipBuffer, autoScale, autoincrementIds) {

  override protected def clipGeometry(geometry: Geometry): Geometry = {
    val clippedGeometry = super.clipGeometry(geometry)
    if (clippedGeometry.isInstanceOf[GeometryCollection]) {
      clippedGeometry match {
        case _: MultiPoint =>
          clippedGeometry
        case _: MultiLineString =>
          clippedGeometry
        case _: MultiPolygon =>
          clippedGeometry
        case _ =>
          val polygons = new ArrayBuffer[Polygon]()
          val lineStrings = new ArrayBuffer[LineString]()
          val points = new ArrayBuffer[Point]()
          for (i <- 0 until clippedGeometry.getNumGeometries) {
            clippedGeometry.getGeometryN(i) match {
              case point: Point =>
                points += point
              case lineString: LineString =>
                lineStrings += lineString
              case polygon: Polygon =>
                polygons += polygon
            }
          }
          if (polygons.nonEmpty) {
            clippedGeometry.getFactory.createMultiPolygon(polygons.toArray)
          } else if (lineStrings.nonEmpty) {
            clippedGeometry.getFactory.createMultiLineString(lineStrings.toArray)
          } else if (points.nonEmpty) {
            clippedGeometry.getFactory.createMultiPoint(points.toArray)
          } else {
            clippedGeometry.getFactory.createEmpty(2)
          }
      }
    } else {
      clippedGeometry
    }
  }
}
