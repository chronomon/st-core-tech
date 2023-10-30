package com.chronomon.tile.vector.app

import com.chronomon.tile.vector.BalanceDistSlicer
import com.chronomon.tile.vector.consume.{FilePersister, LayerInfo, ZoomBound}
import com.chronomon.tile.vector.crs.DefaultVectorCRS
import com.chronomon.tile.vector.feature.WrapFeature
import com.chronomon.tile.vector.util.WKTUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object VectorTileApp {

  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf()
      .setAppName("VectorTileApp")
      .setMaster("local[*]")
    val spark = new SparkContext(sparkConf)

    val start = System.currentTimeMillis()

    val persister = new FilePersister("E:/tile")
    val distributeSlicer = new BalanceDistSlicer(DefaultVectorCRS, persister)
    // 可以将多个图层数据切到同一张瓦片中，此处只切POI图层
    distributeSlicer.slice(readPoi(spark))

    val total = (System.currentTimeMillis() - start) / 1E3
    println("共计耗时：" + total + "秒")
  }

  def readPoi(spark: SparkContext): (LayerInfo, RDD[WrapFeature]) = {
    val path = "E:\\data\\poi.csv"
    val layerName = "poi"
    val attrNames = Array("id", "name", "type_code", "type_name")

    // 根据字段属性控制要素的显隐层级
    val gradientIndex = attrNames.indexOf("type_code")
    val gradientFunc = (feature: WrapFeature) => {
      val classField = feature.attributes(gradientIndex)
      if (classField.equals("15050000")) {
        ZoomBound(0, 7)
      } else if (classField.equals("12010000")) {
        ZoomBound(8, 10)
      } else if (classField.equals("16010000")) {
        ZoomBound(10, 16)
      } else {
        ZoomBound(10, 16)
      }
    }
    val layerInfo = LayerInfo(layerName, ZoomBound(5, 16), attrNames, gradientFunc)

    (layerInfo, buildRdd(spark, path))
  }

  private def buildRdd(spark: SparkContext, path: String): RDD[WrapFeature] = {
    spark.textFile(path, 10).map(line => {
      val fields = line.split("\t")
      val geom = WKTUtils.read(fields(0))
      val attrs: Array[String] = fields.drop(1)

      WrapFeature(geom, attrs)
    })
  }
}
