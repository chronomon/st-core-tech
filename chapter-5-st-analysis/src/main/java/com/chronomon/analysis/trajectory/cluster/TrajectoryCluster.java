package com.chronomon.analysis.trajectory.cluster;

import com.chronomon.analysis.trajectory.model.GpsPoint;
import com.chronomon.analysis.trajectory.model.Trajectory;
import org.locationtech.jts.geom.LineString;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * trajectory cluster
 *
 * @author yuzisheng
 * @date 2023-11-07
 */
public class TrajectoryCluster {
    List<Trajectory> trajs;

    // trajectory partition parameters
    double partitionMinSegmentLengthInM;

    // segments dbscan cluster parameters
    double dbscanEpsInM;
    int dbscanMinNum;

    // compute representative trajectory parameters
    double repMinSmoothingLengthInM;
    int repMinTrajNumForCluster;
    int repMinSegmentNumForSweep;

    /**
     * trajectory cluster
     *
     * @param trajs                        trajectories to cluster
     * @param partitionMinSegmentLengthInM min segment length in meter for partition step
     * @param dbscanEpsInM                 eps in meter for dbscan step
     * @param dbscanMinNum                 min number for dbscan step
     * @param repMinSmoothingLengthInM     min smoothing length in meter for representative computing step
     * @param repMinTrajNumForCluster      min trajectory number during cluster for representative computing step
     * @param repMinSegmentNumForSweep     min segment number during sweep representative computing step
     */
    public TrajectoryCluster(List<Trajectory> trajs,
                             double partitionMinSegmentLengthInM,
                             double dbscanEpsInM,
                             int dbscanMinNum,
                             double repMinSmoothingLengthInM,
                             int repMinTrajNumForCluster,
                             int repMinSegmentNumForSweep) {
        this.trajs = trajs;
        this.partitionMinSegmentLengthInM = TrajectoryClusterUtil.getDegreeFromM(partitionMinSegmentLengthInM);
        this.dbscanEpsInM = TrajectoryClusterUtil.getDegreeFromM(dbscanEpsInM);
        this.dbscanMinNum = dbscanMinNum;
        this.repMinSmoothingLengthInM = TrajectoryClusterUtil.getDegreeFromM(repMinSmoothingLengthInM);
        this.repMinTrajNumForCluster = repMinTrajNumForCluster;
        this.repMinSegmentNumForSweep = repMinSegmentNumForSweep;
    }

    /**
     * do trajectory cluster
     *
     * @return list of representative spatial line
     */
    public List<LineString> doCluster() throws Exception {
        // first step: trajectory partition
        TrajectoryPartition trajectoryPartition = new TrajectoryPartition(trajs, partitionMinSegmentLengthInM);
        ArrayList<TrajectorySegment> segments = trajectoryPartition.partition();

        // second step: trajectory cluster including noise
        TrajectoryDBScan trajectoryDBScan = new TrajectoryDBScan(segments, dbscanEpsInM, dbscanMinNum);
        ArrayList<Integer> clusterIds = trajectoryDBScan.cluster();

        // third step: compute representative trajectory
        int clusterNum = trajectoryDBScan.getClusterNum();
        TrajectoryRepresentative trajectoryRepresentative = new TrajectoryRepresentative(segments, clusterIds, clusterNum,
                repMinSmoothingLengthInM, repMinTrajNumForCluster, repMinSegmentNumForSweep);

        return trajectoryRepresentative.compute();
    }

    public static void main(String[] args) throws Exception {
        String filePath = Objects.requireNonNull(TrajectoryCluster.class.getResource("/trajectory_cluster.txt")).getPath();
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<Trajectory> trajs = new ArrayList<>();
        String line;
        Timestamp fixedTimestamp = Timestamp.valueOf("2023-11-11 11:11:11");
        while ((line = bufferedReader.readLine()) != null) {
            String[] items = line.split(" ");
            String oid = items[0];
            List<GpsPoint> gpsPoints = new ArrayList<>();
            for (int i = 1; i < items.length; i += 2) {
                gpsPoints.add(new GpsPoint(String.valueOf(i), Double.parseDouble(items[i]), Double.parseDouble(items[i + 1]), fixedTimestamp));
            }
            Trajectory traj = new Trajectory(oid, gpsPoints);
            trajs.add(traj);
        }

        // trajectory partition parameters
        double PARTITION_MIN_SEGMENT_LENGTH_IN_M = 20.0;

        // segments dbscan cluster parameters
        double DBSCAN_EPS_IN_M = 1000.0;
        int DBSCAN_MIN_NUM = 2;

        // compute representative trajectory parameters
        double REP_MIN_SMOOTHING_LENGTH_IN_M = 30.0;
        int REP_MIN_TRAJ_NUM_FOR_CLUSTER = 1;
        int REP_MIN_SEGMENT_NUM_FOR_SWEEP = 4;

        TrajectoryCluster trajectoryCluster = new TrajectoryCluster(trajs,
                PARTITION_MIN_SEGMENT_LENGTH_IN_M, DBSCAN_EPS_IN_M, DBSCAN_MIN_NUM,
                REP_MIN_SMOOTHING_LENGTH_IN_M, REP_MIN_TRAJ_NUM_FOR_CLUSTER, REP_MIN_SEGMENT_NUM_FOR_SWEEP);
        List<LineString> representativeTrajs = trajectoryCluster.doCluster();
        System.out.println(representativeTrajs.size());  // 1
    }
}
