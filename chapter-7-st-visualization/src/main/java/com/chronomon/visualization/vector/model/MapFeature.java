package com.chronomon.visualization.vector.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

@Data
@AllArgsConstructor
public class MapFeature {

    private long id;


    private Geometry geom;

    private Map<String, Object> properties;
}
