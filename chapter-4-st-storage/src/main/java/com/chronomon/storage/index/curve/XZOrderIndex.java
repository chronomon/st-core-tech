package com.chronomon.storage.index.curve;

import org.locationtech.geomesa.curve.XZ2SFC;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import scala.Tuple2;
import scala.Tuple4;

/**
 * XZOrderIndex
 *
 * @author yuzisheng
 * @date 2023-11-05
 */
public class XZOrderIndex {

    /**
     * @see XZ2SFC
     */
    private final XZ2SFC xz2SFC;

    XZOrderIndex(int precision) {
        xz2SFC = new XZ2SFC((short) precision, new Tuple2<>(-180.0, 180.0), new Tuple2<>(-90.0, 90.0));
    }

    XZOrderIndex(double minX, double maxX, double minY, double maxY, int precision) {
        xz2SFC = new XZ2SFC((short) precision, new Tuple2<>(minX, maxX), new Tuple2<>(minY, maxY));
    }

    /**
     * @see XZ2SFC#index(Tuple4)
     */
    public long index(Geometry geometry) {
        Envelope envelope = geometry.getEnvelopeInternal();
        return xz2SFC.index(new Tuple4<>(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()));
    }

    public static void main(String[] args) throws Exception {
        WKTReader wktReader = new WKTReader();

        XZOrderIndex xz1 = new XZOrderIndex(1);
        System.out.println(xz1.index(wktReader.read("POINT (-180.0 -90.0)")));  // 1
        System.out.println(xz1.index(wktReader.read("POINT (180.0 90.0)")));  // 4

        XZOrderIndex xz2 = new XZOrderIndex(2);
        System.out.println(xz2.index(wktReader.read("POINT (-180.0 -90.0)")));  // 2
        System.out.println(xz2.index(wktReader.read("POINT (180.0 90.0)")));  // 20
    }
}
