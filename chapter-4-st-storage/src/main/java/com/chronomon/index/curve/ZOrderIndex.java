package com.chronomon.index.curve;

import org.locationtech.geomesa.curve.NormalizedDimension;
import org.locationtech.geomesa.curve.Z2SFC;

import java.lang.reflect.Field;

/**
 * ZOrderIndex
 *
 * @author yuzisheng
 * @date 2023-11-05
 */
public class ZOrderIndex {
    /**
     * @see Z2SFC
     */
    private final Z2SFC z2SFC;

    ZOrderIndex(int precision) {
        z2SFC = new Z2SFC(precision);
    }

    ZOrderIndex(double minX, double maxX, double minY, double maxY, int precision) throws Exception {
        z2SFC = new Z2SFC(precision);
        try {
            Field lon = z2SFC.getClass().getDeclaredField("lon");
            lon.setAccessible(true);
            lon.set(z2SFC, new NormalizedDimension.BitNormalizedDimension(minX, maxX, precision));

            Field lat = z2SFC.getClass().getDeclaredField("lat");
            lat.setAccessible(true);
            lat.set(z2SFC, new NormalizedDimension.BitNormalizedDimension(minY, maxY, precision));
        } catch (Exception ignored) {
            throw new Exception("Fail to change the ranges of x and y by reflection");
        }
    }

    /**
     * @see Z2SFC#index(double, double, boolean)
     */
    public long index(double x, double y) {
        return z2SFC.index(x, y, false);
    }

    public static void main(String[] args) throws Exception {
        ZOrderIndex z1 = new ZOrderIndex(1);
        System.out.println(z1.index(-180, -90));  // 0
        System.out.println(z1.index(180, 90));  // 3

        ZOrderIndex z2 = new ZOrderIndex(2);
        System.out.println(z2.index(-180, -90));  // 0
        System.out.println(z2.index(180, 90));  // 15
    }
}
