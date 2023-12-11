package com.chronomon.visualization.vector.model;

import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.*;

import java.util.*;

public class TileEncoder {

    private final Pyramid pyramid;

    public TileEncoder(Pyramid pyramid) {
        this.pyramid = pyramid;
    }

    public byte[] generateTile(List<MapFeature> featureList) {
        FixedVectorTileEncoder encoder = new FixedVectorTileEncoder(pyramid.tileExtent, 8, false, false);
        for (MapFeature feature : featureList) {
            encoder.addFeature("layer", feature.getProperties(), feature.getGeom(), feature.getId());
        }
        return encoder.encode();
    }

    private static class FixedVectorTileEncoder extends VectorTileEncoder {
        FixedVectorTileEncoder(int extent, int clipBuffer, boolean autoScale, boolean autoincrementIds) {
            super(extent, clipBuffer, autoScale, autoincrementIds, -1.0);
        }

        protected Geometry clipGeometry(Geometry geometry) {
            Geometry clippedGeometry = super.clipGeometry(geometry);
            if (clippedGeometry instanceof GeometryCollection) {
                if (clippedGeometry instanceof MultiPoint) {
                    return clippedGeometry;
                } else if (clippedGeometry instanceof MultiLineString) {
                    return clippedGeometry;
                } else if (clippedGeometry instanceof MultiPolygon) {
                    return clippedGeometry;
                } else {
                    List<Polygon> polygons = new ArrayList<>();
                    List<LineString> lineStrings = new ArrayList<>();
                    List<Point> points = new ArrayList<>();
                    for (int i = 0; i < clippedGeometry.getNumGeometries(); i++) {
                        Geometry subGeom = clippedGeometry.getGeometryN(i);
                        if (subGeom instanceof Point) {
                            points.add((Point) subGeom);
                        } else if (subGeom instanceof LineString) {
                            lineStrings.add((LineString) subGeom);
                        } else if (subGeom instanceof Polygon) {
                            polygons.add((Polygon) subGeom);
                        }
                    }
                    if (polygons.size() > 0) {
                        return clippedGeometry.getFactory().createMultiPolygon(polygons.toArray(new Polygon[0]));
                    } else if (lineStrings.size() > 0) {
                        return clippedGeometry.getFactory().createMultiLineString(lineStrings.toArray(new LineString[0]));
                    } else if (points.size() > 0) {
                        return clippedGeometry.getFactory().createMultiPoint(points.toArray(new Point[0]));
                    } else {
                        return clippedGeometry.getFactory().createEmpty(2);
                    }
                }
            } else {
                return clippedGeometry;
            }
        }
    }
}
