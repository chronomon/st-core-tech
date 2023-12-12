package com.chronomo.services.util;

import org.geotools.data.*;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
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
public class SpatialiteUtil {

    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

    public static void main(String[] args) throws IOException, CQLException {
        String plg = "Polygon ((120.61045595303342282 31.29702097551869855, 120.61013599860429224 31.29362145970915066, 120.61017599290792646 31.29098183566879499, 120.61553522959592044 31.29142177300885308, 120.61665507009789167 31.29322151667273388, 120.61289560555556477 31.29690099260777458, 120.61289560555556477 31.29690099260777458, 120.61045595303342282 31.29702097551869855))";
        List<SimpleFeature> features = queryFeatures("poi", plg);
        for (SimpleFeature feature : features) {
            Object objGeom = feature.getDefaultGeometry();
            System.out.println(objGeom);
        }
    }

    /**
     * 基于多边形范围查询
     * @param sft
     * @param polygonWKT 多边形wkt
     * @return
     * @throws IOException
     * @throws CQLException
     */
    public static List<SimpleFeature> queryFeaturesByPlg(String sft, String polygonWKT) throws IOException, CQLException {
        String cql = String.format("INTERSECTS(geom,%s)", polygonWKT);
        return queryFeatures(sft, cql);
    }

    /**
     * 基于四至范围查询
     * @param sft
     * @param minx 最小经度
     * @param miny 最小纬度
     * @param maxx 最大经度
     * @param maxy 最大纬度
     * @return
     * @throws IOException
     * @throws CQLException
     */
    public static List<SimpleFeature> queryFeaturesByBBOX(String sft, double minx, double miny, double maxx, double maxy) throws IOException, CQLException {
        String cql = String.format("BBOX(geom,%s,%s,%s,%s)", minx, miny, maxx, maxy);
        return queryFeatures(sft, cql);
    }

    /**
     * 基于点和半径查询
     * @param sft
     * @param x 点经度
     * @param y 点纬度
     * @param radiusM 范围半径（单位米）
     * @return
     * @throws IOException
     * @throws CQLException
     */
    public static List<SimpleFeature> queryFeaturesByRange(String sft, double x, double y, double radiusM) throws IOException, CQLException {
        Point point = GEOMETRY_FACTORY.createPoint(new CoordinateXY(x, y));
        Geometry bufferGeom = point.buffer(GeometryUtil.meterToDegree(radiusM));
        String cql = String.format("WITHIN(geom,%s)", WKTUtil.toWKT(bufferGeom));
        return queryFeatures(sft, cql);
    }

    /**
     * cql查询
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
}
