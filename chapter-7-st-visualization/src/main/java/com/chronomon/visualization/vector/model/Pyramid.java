package com.chronomon.visualization.vector.model;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class Pyramid {

    public final int maxZoomLevel;

    public final int tileExtent;

    private Pyramid(int maxZoomLevel, int tileExtent) {
        this.maxZoomLevel = maxZoomLevel;
        this.tileExtent = tileExtent;
    }

    public static final Pyramid PYRAMID;

    static {
        PYRAMID = new Pyramid(18, 256);
    }

    public static Pyramid getInstance() {
        return PYRAMID;
    }

    /**
     * 瓦片对应投影坐标系中的空间范围
     *
     * @param tileCoord 瓦片坐标
     * @return 投影坐标系中的空间范围
     */
    public Envelope getProjectTileBound(TileCoord tileCoord) {
        int minX = tileCoord.columnNum * tileExtent;
        int minY = tileCoord.rowNum * tileExtent;
        int maxX = minX + tileExtent;
        int maxY = minY + tileExtent;
        return new Envelope(
                pixelX2Project(minX, tileCoord.zoomLevel),
                pixelX2Project(maxX, tileCoord.zoomLevel),
                pixelY2Project(minY, tileCoord.zoomLevel),
                pixelY2Project(maxY, tileCoord.zoomLevel));
    }

    private double pixelX2Project(double pixelX, int zoomLevel) {
        return MercatorCRS.MIN_VALUE + pixelX * getResolution(zoomLevel);
    }

    private double pixelY2Project(double pixelY, int zoomLevel) {
        return MercatorCRS.MAX_VALUE - pixelY * getResolution(zoomLevel);
    }

    /**
     * 投影坐标转像素坐标
     *
     * @param projectGeom 投影标系下的空间几何
     */
    public Geometry project2Pixel(Geometry projectGeom, boolean reserveProject, int zoomLevel) {
        Geometry geom = reserveProject ? projectGeom.copy() : projectGeom;
        Arrays.stream(geom.getCoordinates()).forEach(coord -> {
            coord.setX(projectX2Pixel(coord.getX(), zoomLevel));
            coord.setY(projectY2Pixel(coord.getY(), zoomLevel));
        });
        geom.geometryChanged();
        return geom;
    }

    private double projectX2Pixel(double projectX, int zoomLevel) {
        return Math.floor((projectX - MercatorCRS.MIN_VALUE) / getResolution(zoomLevel));
    }

    private double projectY2Pixel(double projectY, int zoomLevel) {
        return Math.floor((MercatorCRS.MAX_VALUE - projectY) / getResolution(zoomLevel));
    }

    public Geometry pixel2TilePixel(Geometry pixelGeom, TileCoord tileCoord) {
        long tileMinPixelX = (long) tileCoord.columnNum * this.tileExtent;
        long tileMinPixelY = (long) tileCoord.rowNum * this.tileExtent;
        Arrays.stream(pixelGeom.getCoordinates()).forEach(coord -> {
            coord.setX(coord.getX() - tileMinPixelX);
            coord.setY(coord.getY() - tileMinPixelY);
        });
        pixelGeom.geometryChanged();
        return pixelGeom;
    }


    private final ConcurrentHashMap<Integer, Double> resolutionCache = new ConcurrentHashMap<>();

    /**
     * 计算地图分辨率(米/像素)
     *
     * @param zoomLevel 地图层级
     * @return 地图分辨率
     */
    private double getResolution(int zoomLevel) {
        if (resolutionCache.containsKey(zoomLevel)) {
            return resolutionCache.get(zoomLevel);
        } else {
            double resolution = MercatorCRS.EXTENT / ((1 << zoomLevel) * this.tileExtent);
            resolutionCache.putIfAbsent(zoomLevel, resolution);
            return resolution;
        }
    }
}
