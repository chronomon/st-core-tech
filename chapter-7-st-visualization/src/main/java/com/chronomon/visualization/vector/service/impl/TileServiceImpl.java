package com.chronomon.visualization.vector.service.impl;

import com.chronomon.visualization.common.DataStoreComponent;
import com.chronomon.visualization.vector.model.MapFeature;
import com.chronomon.visualization.vector.model.Pyramid;
import com.chronomon.visualization.vector.model.TileCoord;
import com.chronomon.visualization.vector.model.TileEncoder;
import com.chronomon.visualization.vector.service.ITileService;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TileServiceImpl implements ITileService {

    @Resource
    private DataStoreComponent dataStoreComponent;

    @Override
    public byte[] getTile(TileCoord tileCoord) {
        // 根据瓦片坐标计算空间范围，并从数据存储中查询地图要素
        Envelope projectBound = Pyramid.getInstance().getProjectTileBound(tileCoord);
        List<MapFeature> featureList = dataStoreComponent.queryFeatures(projectBound);

        // 地图要素的投影坐标转地图的地图像素坐标, 地图像素坐标转瓦片像素坐标, 不破坏原始Feature
        Pyramid pyramid = Pyramid.getInstance();
        List<MapFeature> tileFeatureList = featureList.stream().map(projectFeature -> {
            Geometry tileGeom = pyramid.pixel2TilePixel(
                    pyramid.project2Pixel(projectFeature.getGeom(), true, tileCoord.zoomLevel),
                    tileCoord);
            return new MapFeature(projectFeature.getId(), tileGeom, projectFeature.getProperties());
        }).collect(Collectors.toList());

        // 矢量瓦片编码
        TileEncoder encoder = new TileEncoder(pyramid);
        return encoder.generateTile(tileFeatureList);
    }
}
