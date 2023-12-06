package com.chronomon.visualization.vector

import com.chronomon.visualization.vector.consume.{LayerInfo, TileGenerator, TilePersister}
import com.chronomon.visualization.vector.crs.VectorCRS
import com.chronomon.visualization.vector.feature.{TileInfo, WrapFeature}
import com.chronomon.visualization.vector.index.{QuadNode, QuadTree}
import org.apache.spark.Partitioner
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.locationtech.jts.geom.Envelope

class BalanceDistSlicer(crs: VectorCRS, persister: TilePersister) {
  def slice(layers: (LayerInfo, RDD[WrapFeature])*): Unit = {
    val layerPreparer = new LayerPreparer(crs, persister)
    val (combineProjectRdd, tileGenerator) = layerPreparer.combine(layers.toArray)

    val crsProjectBound = crs.projectBound
    val projectRdd = combineProjectRdd.filter(feature => {
      crsProjectBound.contains(feature.projectEnv)
    }).persist(StorageLevel.MEMORY_AND_DISK)

    //calculate global bound of rdd
    val globalBound = projectRdd.map(_.geom.getEnvelopeInternal)
      .reduce((env1, env2) => {
        env1.expandToInclude(env2)
        env1
      })

    //slicing for tiles whose zoom level equals or is lower than root's
    val root = QuadNode.tightRoot(globalBound, crsProjectBound)
    val samples = projectRdd.sample(withReplacement = false, 0.01).map(_.projectEnv).collect()
    val rootFeatures = balance(projectRdd, root, samples, tileGenerator)

    //slicing for tile whose zoom level is upper than root's
    if (tileGenerator.minLevel < root.tileInfo.tileCoord.zoomLevel) {
      val upperLocalSlicer = new UpperLocalSlicer(tileGenerator)
      upperLocalSlicer.upperSlice((root.tileInfo, rootFeatures))
    }
  }

  private def balance(projectRdd: RDD[WrapFeature], root: QuadNode,
                      samples: Array[Envelope], tileGenerator: TileGenerator): Array[WrapFeature] = {

    val sparkContext = projectRdd.context

    //build global index
    val reLabelRoot = new QuadNode(TileInfo(root.tileInfo.tileCoord, root.tileInfo.env, tileId = 0))
    val quadTree = new QuadTree(reLabelRoot)
    val partitionDepth = 5 //todo:用常量表示
    val bankedNodes = quadTree.build(samples, partitionDepth)
    val bcQuadTree = sparkContext.broadcast(quadTree)

    //recursively slice for banked leaf nodes
    val bankedTileFeatures = bankedNodes.map {
      case (subRoot, subSamples) =>
        val subEnv = subRoot.tileInfo.env
        val subRdd = projectRdd.filter(_.projectEnv.intersects(subEnv))
        (subRoot.tileInfo, balance(subRdd, subRoot, subSamples, tileGenerator))
    }

    //lower slice for balance leaf nodes
    var partitioner = quadTree.getPartitioner
    val partitionedRdd = projectRdd.mapPartitions(iter => {
      val tree = bcQuadTree.value
      iter.flatMap(feature => {
        tree.intersectBottomTiles(feature.projectEnv).map((_, feature))
      })
    }).partitionBy(partitioner).mapPartitions(iter => {
      if (iter.nonEmpty) {
        val features = iter.toArray
        Iterator((features.head._1, features.map(_._2)))
      } else Iterator.empty
    })
    var balanceRdd = lowerSlice(partitionedRdd, bcQuadTree, tileGenerator)

    val rootZoomLevel = root.tileInfo.tileCoord.zoomLevel
    var bottomZoomLevel = rootZoomLevel + partitionDepth
    if (bottomZoomLevel <= tileGenerator.minLevel) {
      balanceRdd.count()
      return null
    }

    //union feature or banked and balance leaf nodes and do upper slicing
    if (bankedTileFeatures.nonEmpty) {
      balanceRdd = balanceRdd.union(sparkContext.parallelize(bankedTileFeatures))
    }
    while (bottomZoomLevel > rootZoomLevel && bottomZoomLevel > tileGenerator.minLevel) {
      val scrollStep = Math.min(bottomZoomLevel - rootZoomLevel, 2) //todo: 常量表示
      val prePartitionNum = partitioner.numPartitions
      partitioner = new Partitioner {
        override def numPartitions: Int = Math.max(prePartitionNum >> (scrollStep * 2), 1)

        override def getPartition(key: Any): Int = key.asInstanceOf[TileInfo].tileId >> (scrollStep * 2)
      }
      balanceRdd = balanceRdd.partitionBy(partitioner).mapPartitions(iter => {
        if (iter.nonEmpty) {
          val upperLocalSlicer = new UpperLocalSlicer(tileGenerator)
          Iterator(upperLocalSlicer.middleSlice(iter.toArray, scrollStep))
        } else Iterator.empty
      })
      bottomZoomLevel = bottomZoomLevel - scrollStep
    }

    //collect features of root tile
    if (bottomZoomLevel == rootZoomLevel) {
      val rootTile = balanceRdd.collect()
      assert(rootTile.length == 1)
      rootTile.head._2
    } else {
      balanceRdd.count()
      null
    }
  }

  private def lowerSlice(partitionedRdd: RDD[(Int, Array[WrapFeature])],
                         bcQuadtree: Broadcast[QuadTree],
                         tileGenerator: TileGenerator): RDD[(TileInfo, Array[WrapFeature])] = {
    partitionedRdd.map {
      case (partitionId, features) =>
        val localRoot = bcQuadtree.value.leafNode(partitionId)
        assert(localRoot.tileInfo.tileId == partitionId)
        val localSlicer = new LowerLocalSlicer(localRoot, tileGenerator)
        localSlicer.slice(features)
    }
  }
}
