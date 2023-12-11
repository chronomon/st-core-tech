package com.chronomon.visualization.common;

import com.chronomon.visualization.vector.model.MapFeature;
import com.chronomon.visualization.vector.model.MercatorCRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Component
public class DataStoreComponent {

    private STRtree rtree;

    @PostConstruct
    private void initData() {
        rtree = new STRtree();
        WKTReader reader = new WKTReader();
        List<String> lines = readResource("data/road.csv");
        for (String line : lines) {
            String[] attrs = line.split("\t");
            try {
                Geometry geom = reader.read(attrs[0]);
                // 地理坐标转投影坐标
                Arrays.stream(geom.getCoordinates()).forEach(coord -> {
                    coord.setX(MercatorCRS.geodetic2ProjectX(coord.getX()));
                    coord.setY(MercatorCRS.geodetic2ProjectY(coord.getY()));
                });
                geom.geometryChanged();
                // 构建地图要素并插入索引树，用于模拟数据库中的空间索引
                long id = Long.parseLong(attrs[1]);
                MapFeature feature = new MapFeature(id, geom, new HashMap<>());
                Envelope env = geom.getEnvelopeInternal();
                rtree.insert(env, feature);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        // 构建索引
        rtree.build();
    }

    private List<String> readResource(String relativePath) {
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(relativePath);
             Reader reader = new InputStreamReader(in);
             BufferedReader bReader = new BufferedReader(reader)) {
            String line;
            List<String> lineList = new ArrayList<>();
            while ((line = bReader.readLine()) != null) {
                lineList.add(line);
            }
            return lineList;
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败", e);
        }
    }

    public List<MapFeature> queryFeatures(Envelope projectBound) {
        FeatureVisitor visitor = new FeatureVisitor(projectBound);
        rtree.query(projectBound, visitor);
        return visitor.featureList;
    }

    private static class FeatureVisitor implements ItemVisitor {

        private final Geometry queryEnv;

        public final List<MapFeature> featureList = new ArrayList<>();

        public FeatureVisitor(Envelope queryEnv) {
            this.queryEnv = new GeometryFactory().toGeometry(queryEnv);
        }

        @Override
        public void visitItem(Object item) {
            if (((MapFeature) item).getGeom().intersects(queryEnv)) {
                featureList.add((MapFeature) item);
            }
        }
    }
}
