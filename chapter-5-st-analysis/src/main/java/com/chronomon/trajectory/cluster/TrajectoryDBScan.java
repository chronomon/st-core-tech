package com.chronomon.trajectory.cluster;

import java.util.*;

/**
 * dbscan cluster for segments
 *
 * @author yuzisheng
 * @date 2023-11-07
 */
public class TrajectoryDBScan {
    /**
     * segments to be clustered
     */
    List<TrajectorySegment> segments;
    /**
     * eps
     */
    double eps;
    /**
     * minimum number
     */
    int minNum;
    /**
     * cluster ids
     */
    ArrayList<Integer> clusterIds;
    /**
     * unclassified point id
     */
    private final int UNCLASSIFIED_ID = -2;
    /**
     * noise point id
     */
    private final int NOISE_ID = -1;

    public TrajectoryDBScan(List<TrajectorySegment> segments, Double eps, int minNum) {
        this.segments = segments;
        this.eps = eps;
        this.minNum = minNum;
    }

    public ArrayList<Integer> cluster() {
        // initialize items with unclassified
        clusterIds = new ArrayList<>(Collections.nCopies(segments.size(), UNCLASSIFIED_ID));
        // dbscan
        int currentId = 0;
        for (int i = 0; i < segments.size(); i++) {
            if (clusterIds.get(i) == UNCLASSIFIED_ID && expandDense(i, currentId)) {
                currentId++;
            }
        }
        return clusterIds;
    }

    public int getClusterNum() throws Exception {
        if (clusterIds == null) {
            throw new Exception("clustering is not running yet");
        }
        if (clusterIds.contains(NOISE_ID)) {
            return (new HashSet<>(clusterIds)).size() - 1;
        } else {
            return (new HashSet<>(clusterIds)).size();
        }
    }

    private boolean expandDense(int segmentIndex, int currentId) {
        Set<Integer> neighborhoods1 = new HashSet<>();
        Set<Integer> neighborhoods2 = new HashSet<>();

        computeEpsNeighborhood(segmentIndex, neighborhoods1);
        if (neighborhoods1.size() < minNum) {
            clusterIds.set(segmentIndex, NOISE_ID);
            return false;
        }
        for (int seed : neighborhoods1) {
            clusterIds.set(seed, currentId);
        }
        neighborhoods1.remove(segmentIndex);
        int currIndex;
        while (!neighborhoods1.isEmpty()) {
            currIndex = (int) neighborhoods1.toArray()[0];
            computeEpsNeighborhood(currIndex, neighborhoods2);
            if (neighborhoods2.size() >= minNum) {
                for (int seed : neighborhoods2) {
                    int tempId = clusterIds.get(seed);
                    if (tempId == UNCLASSIFIED_ID || tempId == NOISE_ID) {
                        if (tempId == UNCLASSIFIED_ID) {
                            neighborhoods1.add(seed);
                        }
                        clusterIds.set(seed, currentId);
                    }
                }
            }
            neighborhoods1.remove(currIndex);
        }
        return true;
    }

    private void computeEpsNeighborhood(int i, Set<Integer> neighborhoods) {
        neighborhoods.clear();
        for (int j = 0; j < segments.size(); j++) {
            double distance = TrajectoryClusterUtil.computeSegmentToSegmentDistance(segments.get(i), segments.get(j));
            if (distance <= eps) neighborhoods.add(j);
        }
    }
}

