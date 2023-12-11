package com.chronomon.analysis.trajectory.segment;

import com.chronomon.analysis.trajectory.filter.TrajNoiseFilter;
import com.chronomon.analysis.trajectory.model.GpsPoint;
import com.chronomon.analysis.trajectory.model.Trajectory;
import com.chronomon.analysis.trajectory.staypoint.TrajStayPointDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * 轨迹分段器
 */
public class TrajSegmenter {

    public static List<Trajectory> sliceByStayPoint(Trajectory trajectory,
                                                    double maxStayDistInMeter,
                                                    long minStayTimeInSecond) {
        return new TrajStayPointDetector(maxStayDistInMeter, minStayTimeInSecond).
                sliceTrajectory(trajectory);
    }

    public static List<Trajectory> sliceByTimeInterval(Trajectory trajectory,
                                                       long maxTimeIntervalInSecond) {

        List<GpsPoint> gpsPointList = trajectory.getSortedGpsList();
        List<Trajectory> subTrajectoryList = new ArrayList<>();
        int startIndex = 0;
        for (int currIndex = 1; currIndex < gpsPointList.size(); currIndex++) {
            long timeInterval = gpsPointList.get(currIndex - 1).timeIntervalInSec(gpsPointList.get(currIndex));
            if (timeInterval > maxTimeIntervalInSecond && startIndex < currIndex - 1) {
                List<GpsPoint> subGpsPointList = gpsPointList.subList(startIndex, currIndex);
                subTrajectoryList.add(new Trajectory(trajectory.getOid(), subGpsPointList, true));
                startIndex = currIndex;
            }
        }

        if (startIndex < gpsPointList.size() - 1) {
            // 末尾的子轨迹
            subTrajectoryList.add(new Trajectory(trajectory.getOid(), gpsPointList.subList(startIndex, gpsPointList.size())));
        }
        return subTrajectoryList;
    }

    public static void main(String[] args) throws Exception {
        List<GpsPoint> gpsList = TrajNoiseFilter.readGpsPoint();
        Trajectory rawTrajectory = new Trajectory("oid", gpsList, true);
        List<Trajectory> subTrajectoryList = TrajSegmenter.sliceByTimeInterval(rawTrajectory, 300);
        System.out.println(subTrajectoryList.size());

        int count = 0;
        for (Trajectory subTrajectory : subTrajectoryList) {
            System.out.println(subTrajectory.getLineString().toText());
            count += subTrajectory.getNumPoints();
        }
        System.out.println(rawTrajectory.getNumPoints());
        System.out.println(count);
    }
}
