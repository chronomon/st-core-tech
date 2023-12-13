package com.chronomo.services.util;

import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @Description
 * @Author Sui Yuan
 * @Date 2023/12/12 15:08
 * @Version 1.0
 */
public class QueryUtil {

    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

    private static final FeatureJSON featureJSON = new FeatureJSON(new GeometryJSON(7));
    /**
     * 基于多边形范围查询
     *
     * @param sft
     * @param polygonWKT 多边形wkt
     * @return
     * @throws IOException
     * @throws CQLException
     */
    public static String queryFeaturesByPlg(String sft, String polygonWKT) throws IOException, CQLException {
        String cql = String.format("INTERSECTS(geom,%s)", polygonWKT);
        return featureJSON.toString(queryFeatureCol(sft, cql));
    }

    /**
     * 基于四至范围查询
     *
     * @param sft
     * @param minx 最小经度
     * @param miny 最小纬度
     * @param maxx 最大经度
     * @param maxy 最大纬度
     * @return
     * @throws IOException
     * @throws CQLException
     */
    public static String queryFeaturesByBBOX(String sft, double minx, double miny, double maxx, double maxy) throws IOException, CQLException {
        String cql = String.format("BBOX(geom,%s,%s,%s,%s)", minx, miny, maxx, maxy);
        return featureJSON.toString(queryFeatureCol(sft, cql));
    }

    /**
     * 基于点和半径查询
     *
     * @param sft
     * @param x       点经度
     * @param y       点纬度
     * @param radiusM 范围半径（单位米）
     * @return
     * @throws IOException
     * @throws CQLException
     */
    public static String queryFeaturesByRadius(String sft, double x, double y, double radiusM) throws IOException, CQLException {
        Point point = GEOMETRY_FACTORY.createPoint(new CoordinateXY(x, y));
        Geometry bufferGeom = point.buffer(GeometryUtil.meterToDegree(radiusM));
        String cql = String.format("WITHIN(geom,%s)", WKTUtil.toWKT(bufferGeom));
        return featureJSON.toString(queryFeatureCol(sft, cql));
    }

    /**
     * cql查询
     *
     * @param sft
     * @param cql cql语句
     * @return
     * @throws CQLException
     * @throws IOException
     */
    private static List<SimpleFeature> queryFeatures(String sft, String cql) throws CQLException, IOException {
        DataStore dataStore = DataStoreCacheHelper.getInstance().getDataStore();
        if (dataStore == null) {
            return null;
        }
        Query query = new Query(sft, ECQL.toFilter(cql));
        List<SimpleFeature> simpleFeatures = new LinkedList<>();
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader = dataStore.getFeatureReader(query, Transaction.AUTO_COMMIT)) {
            while (reader.hasNext()) {
                simpleFeatures.add(reader.next());
            }
        }
        return simpleFeatures;
    }

    private static SimpleFeatureCollection queryFeatureCol(String sft, String cql) throws CQLException, IOException {
        DataStore dataStore = DataStoreCacheHelper.getInstance().getDataStore();
        if (dataStore == null) {
            return null;
        }
        Query query = new Query(sft, ECQL.toFilter(cql));
        SimpleFeatureSource source = dataStore.getFeatureSource(sft);
        SimpleFeatureCollection collection = source.getFeatures(query);
        return collection;
    }
}
