package com.chronomon.tile.vector

import com.chronomon.tile.vector.consume.TileGenerator
import com.chronomon.tile.vector.consume.TileGenerator.EXTENT
import com.chronomon.tile.vector.feature.{TileInfo, WrapFeature}
import org.locationtech.jts.geom.Envelope

import scala.collection.mutable.ArrayBuffer

class UpperLocalSlicer(consumer: TileGenerator) {
  private val minZoomLevel = consumer.minLevel

  def middleSlice(tileFeatures: Array[(TileInfo, Array[WrapFeature])], scrollStep: Int): (TileInfo, Array[WrapFeature]) = {
    var tilesWithFeatures = tileFeatures.sortBy(_._1.tileId).map {
      case (tileInfo, features) => TileWithFeatures(tileInfo, features)
    }
    var step = scrollStep
    while (step > 0) {
      tilesWithFeatures = sliceAndScroll(tilesWithFeatures)
      step -= 1
    }
    tilesWithFeatures.map(tileFeatures => (tileFeatures.tileInfo, tileFeatures.features)).head
  }

  def upperSlice(tileFeatures: (TileInfo, Array[WrapFeature])): Unit = {
    var tmpTileFeatures = TileWithFeatures(tileFeatures._1, tileFeatures._2)
    while (tmpTileFeatures.tileInfo.tileCoord.zoomLevel > minZoomLevel) {
      tmpTileFeatures = sliceAndScroll(Array(tmpTileFeatures)).head
    }
  }

  private def sliceAndScroll(tilesWithFeatures: Array[TileWithFeatures]): Array[TileWithFeatures] = {
    val groupedTileFeatures = tilesWithFeatures.groupBy(_.tileInfo.tileId >> 2).values
    (for (childrenTileFeatures <- groupedTileFeatures) yield {
      //calculate parent tile boundary
      val tileEnv = new Envelope()
      for (childTileFeatures <- childrenTileFeatures) {
        tileEnv.expandToInclude(childTileFeatures.tileInfo.env)
      }
      //aggregate child features
      val features = new ArrayBuffer[WrapFeature]()
      for (childTileFeatures <- childrenTileFeatures) {
        features ++= childTileFeatures.scrollFeatures(tileEnv)
      }
      //generate tile
      val baseTile = childrenTileFeatures.head.tileInfo
      val tileInfo = baseTile.parent(tileEnv)

      val featureArray = features.toArray
      consumer.generateTile(tileInfo.tileCoord, featureArray)
      TileWithFeatures(tileInfo, consumer.reduceFeatures(tileInfo.tileCoord.zoomLevel, featureArray))
    }).toArray
  }

  private case class TileWithFeatures(tileInfo: TileInfo, features: Array[WrapFeature]) {
    def scrollFeatures(parentEnv: Envelope): Array[WrapFeature] = {
      val duplicatedFeatures = duplicate(parentEnv)
      tileInfo.tileId % 4 match {
        case 0 => duplicatedFeatures.foreach(_.scrollUp((0, -EXTENT)))
        case 1 => duplicatedFeatures.foreach(_.scrollUp((-EXTENT, -EXTENT)))
        case 2 => duplicatedFeatures.foreach(_.scrollUp((0, 0)))
        case 3 => duplicatedFeatures.foreach(_.scrollUp((-EXTENT, 0)))
      }
      duplicatedFeatures
    }

    private def duplicate(parentEnv: Envelope): Array[WrapFeature] = {
      features.flatMap(feature => {
        if (tileInfo.env.contains(feature.projectEnv)) Some(feature)
        else {
          val intersection = parentEnv.intersection(feature.projectEnv)
          if (tileInfo.env.contains(intersection.getMinX, intersection.getMinY)) Some(feature) else None
        }
      })
    }
  }

}
