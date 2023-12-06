package com.chronomon.analysis.trajectory.road;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoadNetworkHolder {

    private static final AtomicInteger keyCounter = new AtomicInteger();

    private static final long MAX_CACHE_ROAD_NETWORK_COUNT = 3;

    private static final Cache<String, RoadNetwork> roadNetworkCache =
            Caffeine.newBuilder().maximumSize(MAX_CACHE_ROAD_NETWORK_COUNT).build();

    public static String generateKey() {
        return Integer.toString(keyCounter.getAndIncrement());
    }

    public static synchronized RoadNetwork getRoadNetwork(List<RoadSegment> roadSegmentList, String cacheKey) {
        return roadNetworkCache.get(cacheKey, s -> {
            RoadNetwork cacheRoadNetwork = new RoadNetwork(roadSegmentList, false);
            cacheRoadNetwork.getRoadRtree();
            cacheRoadNetwork.getRoadGraph();
            return cacheRoadNetwork;
        });
    }
}
