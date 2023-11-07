package com.chronomon.trajectory.cluster;

import com.chronomon.trajectory.model.Trajectory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.ArrayList;
import java.util.List;

/**
 * trajectory partition
 *
 * @author yuzisheng
 * @date 2023-11-07
 */
public class TrajectoryPartition {
    /**
     * raw trajectories
     */
    private final List<Trajectory> trajs;
    /**
     * minimum length threshold to filter short segments
     */
    private final double minSegmentLength;

    public TrajectoryPartition(List<Trajectory> trajs, double minSegmentLength) {
        this.trajs = trajs;
        this.minSegmentLength = minSegmentLength;
    }

    public ArrayList<TrajectorySegment> partition() throws Exception {
        ArrayList<TrajectorySegment> partitionedSegments = new ArrayList<>();
        for (Trajectory traj : trajs) {
            partitionedSegments.addAll(partitionOneTraj(traj));
        }
        return partitionedSegments;
    }

    private ArrayList<TrajectorySegment> partitionOneTraj(Trajectory traj) throws Exception {
        LineString trajLine = traj.getLineString();
        int pointNumber = trajLine.getNumPoints();
        if (pointNumber < 2) {
            throw new Exception("trajectory to be partitioned shall contain at least two points");
        }

        ArrayList<Coordinate> characteristicPoints = new ArrayList<>();
        // first: add the start point
        characteristicPoints.add(trajLine.getCoordinateN(0));

        // second: check each point
        int startIndex = 0, length = 1, currIndex;
        int parMDLCost, noParMDLCost;
        do {
            currIndex = startIndex + length;
            // MDLCost = L(H) + L(D|H)
            parMDLCost = computeParModelCost(traj, startIndex, currIndex) + computeEncodingCost(traj, startIndex, currIndex);
            // L(D|H)=0 when there is no characteristic point between pi and pj
            noParMDLCost = computeNoParModelCost(traj, startIndex, currIndex);
            if (parMDLCost > noParMDLCost) {
                characteristicPoints.add(trajLine.getCoordinateN(currIndex - 1));
                startIndex = currIndex - 1;
                length = 1;
            } else {
                length += 1;
            }
        } while (startIndex + length < pointNumber);

        // third: add the end point
        characteristicPoints.add(trajLine.getCoordinateN(pointNumber - 1));

        ArrayList<TrajectorySegment> segments = new ArrayList<>();
        for (int i = 0; i < characteristicPoints.size() - 1; i++) {
            TrajectorySegment s = new TrajectorySegment(characteristicPoints.get(i), characteristicPoints.get(i + 1), traj.getOid());
            if (s.length() >= minSegmentLength) {
                segments.add(s);
            }
        }
        return segments;
    }

    /**
     * compute L(H) assuming pi and pj are only two characteristic points
     */
    private static int computeParModelCost(Trajectory traj, int i, int j) {
        double distance = TrajectoryClusterUtil.computePointToPointDistance(traj.getLineString().getCoordinateN(i), traj.getLineString().getCoordinateN(j));
        if (distance < 1.0) distance = 1.0;
        return (int) Math.ceil(TrajectoryClusterUtil.log2(distance));
    }

    /**
     * compute L(H) assuming no characteristic point between pi and pj
     */
    private static int computeNoParModelCost(Trajectory traj, int i, int j) {
        int modelCost = 0;
        double distance;
        for (int k = i; k < j; k++) {
            distance = TrajectoryClusterUtil.computePointToPointDistance(traj.getLineString().getCoordinateN(k), traj.getLineString().getCoordinateN(k + 1));
            if (distance < 1.0) distance = 1.0;
            modelCost += (int) Math.ceil(TrajectoryClusterUtil.log2(distance));
        }
        return modelCost;
    }

    /**
     * compute L(D|H) assuming pi and pj are only two characteristic points
     */
    private static int computeEncodingCost(Trajectory traj, int i, int j) {
        LineString trajLine = traj.getLineString();
        int encodingCost = 0;
        TrajectorySegment s1 = new TrajectorySegment(trajLine.getCoordinateN(i), trajLine.getCoordinateN(j));
        TrajectorySegment s2;
        double perDistance, angleDistance;
        for (int k = i; k < j; k++) {
            s2 = new TrajectorySegment(trajLine.getCoordinateN(k), trajLine.getCoordinateN(k + 1));
            perDistance = TrajectoryClusterUtil.computePerpendicularDistance(s1, s2);
            angleDistance = TrajectoryClusterUtil.computeAngleDistance(s1, s2);

            if (perDistance < 1.0) perDistance = 1.0;
            if (angleDistance < 1.0) angleDistance = 1.0;
            encodingCost += ((int) Math.ceil(TrajectoryClusterUtil.log2(perDistance)) + (int) Math.ceil(TrajectoryClusterUtil.log2(angleDistance)));
        }
        return encodingCost;
    }
}

