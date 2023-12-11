package com.chronomon.visualization.vector.model;

public class TileCoord {

    public final int rowNum;

    public final int columnNum;

    public final int zoomLevel;

    public TileCoord(int rowNum, int columnNum, int zoomLevel) {
        this.rowNum = rowNum;
        this.columnNum = columnNum;
        this.zoomLevel = zoomLevel;
    }

    public String toString() {
        return String.format("%s-%s-%s", zoomLevel, rowNum, columnNum);
    }
}
