package com.chronomon.visualization.vector.model;

public class MercatorCRS {
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;

    public static final double MIN_VALUE = -20037508.3427892;

    public static final double MAX_VALUE = 20037508.3427892;

    public static final double EXTENT = MAX_VALUE - MIN_VALUE;

    /**
     * convert geodetic lng to projection x
     *
     * @param lng longitude
     * @return mercator projection x with unit 'meter'
     */
    public static double geodetic2ProjectX(double lng) {
        return lng * ORIGIN_SHIFT / 180.0;
    }

    /**
     * convert geodetic lat to projection y
     *
     * @param lat latitude
     * @return mercator projection y with unit 'meter'
     */
    public static double geodetic2ProjectY(double lat) {
        double projectLat = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        return projectLat * ORIGIN_SHIFT / 180;
    }
}
