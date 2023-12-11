package com.chronomon.visualization.vector.service;


import com.chronomon.visualization.vector.model.TileCoord;

public interface ITileService {

    byte[] getTile(TileCoord tileCoord);
}
