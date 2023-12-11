# What is this
《时空大数据系统核心技术》的配套代码，工程里每个模块和书中的章节相对应。这个代码库的目的是为了帮助读者更快的应用这些算法，因此我们没有过多地深入到算法的细节中，而是着重呈现它们最常用的使用方法。文章内提到的算法都提供了包含Main方法的可执行类，用户可以查看执行类内包含的java工具包，以及使用方法。

## 阅读指引
### 第4章：数据存储与索引
+ 4.1、空间数据模型
  + 4.1.1、空间数据模型 [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/model/GeometryDataType.java)
  + 4.1.2、空间数据特性 [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/model/GeometryValidation.java)
  + 4.1.3、空间拓扑关系 [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/model/GeometryTopologyRelation.java)
+ 4.2、时空索引算法
  + 4.2.2、基于树状结构的空间索引
    + KdTreeIndex [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/index/tree/KdTreeIndex.java)
    + QuadTreeIndex [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/index/tree/QuadTreeIndex.java)
    + RTreeIndex [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/index/tree/RTreeIndex.java)
  + 4.2.3、基于填充曲线的时空索引
    + Z曲线 [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/index/curve/ZOrderIndex.java)
    + GeoHash编码 [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/index/curve/GeoHash.java)
    + XZ曲线 [[代码链接]](chapter-4-st-storage/src/main/java/com/chronomon/storage/index/curve/XZOrderIndex.java)

### 第5章：时空大数据分析与挖掘
+ 5.2、几何分析
  + 5.2.1、凸包计算 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/convex/ConvexHull.java)
+ 5.3、空间聚类
  + 5.3.1、KMeans [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/cluster/KMeansPlusPlusCluster.java)
  + 5.3.2、DBSCAN [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/cluster/DBSCANCluster.java)
+ 5.4、轨迹分析
  + 5.4.1、轨迹预处理
    + 轨迹去噪 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/filter/TrajNoiseFilter.java)
    + 驻留点检测 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/staypoint/TrajStayPointDetector.java)
    + 轨迹分段 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/segment/TrajSegmenter.java)
    + 路网匹配 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/mapmatch/HmmMapMatcher.java)
  + 5.4.2、轨迹挖掘
    + 轨迹压缩 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/compress/TrajectoryCompress.java)
    + 轨迹相似性 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/similarity/TrajectorySimilarity.java)
    + 轨迹聚类 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/cluster/TrajectoryCluster.java)
    + 时空共现 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/cooccur/SocialStrengthInfer.java)
  + 5.4.3、实时轨迹分析
    + 实时GPS点排序 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/flink/GpsStreamSortFunction.java)
    + 实时车辆行驶状态检测 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/flink/TrajStayPointDetectFunction.java)
    + 实时轨迹地图匹配 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/trajectory/flink/TrajMapMatchFunction.java)
+ 5.5、路径规划
  + 5.5.1、DFS和BFS
    + 深度优先搜索DFS [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/path/DFSModel.java)
    + 广度优先搜索BFS [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/path/BFSModel.java)
  + 5.5.2、Dijkstra算法 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/path/DijkstraModel.java)
  + 5.5.3、GBFS算法 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/path/GBFSModel.java)
  + 5.5.4、A*算法 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/path/AStarModel.java)
+ 5.6、地址搜索
  + 5.6.1、正地理编码
    + 基于地理层级树的滑窗匹配算法 [[代码链接]](chapter-5-st-analysis/src/main/java/com/chronomon/analysis/address/AddressSearch.java)
### 第7章：数据可视化
+ 7.2、时空数据可视化
  + 7.2.5、 动态矢量切片服务[[代码链接]](chapter-7-st-visualization/src/main/java/com/chronomon/visualization/vector/controller/VectorTileController.java)

 
