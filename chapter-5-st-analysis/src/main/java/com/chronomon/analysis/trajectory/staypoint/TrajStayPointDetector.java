package com.chronomon.analysis.trajectory.staypoint;

import com.chronomon.analysis.trajectory.filter.TrajNoiseFilter;
import com.chronomon.analysis.trajectory.model.DistanceUtil;
import com.chronomon.analysis.trajectory.model.GpsPoint;
import com.chronomon.analysis.trajectory.model.Trajectory;

import java.util.ArrayList;
import java.util.List;

public class TrajStayPointDetector {
    private final double maxStayDistInMeter;
    private final long minStayTimeInSecond;

    public TrajStayPointDetector(double maxStayDistInMeter, long minStayTimeInSecond) {
        this.maxStayDistInMeter = maxStayDistInMeter;
        this.minStayTimeInSecond = minStayTimeInSecond;
    }

    public List<StayPoint> detectStayPoint(Trajectory trajectory) {
        List<GpsPoint> gpsPointList = trajectory.getSortedGpsList();
        List<StayPointMark> stayPointMarks = this.calStayPointMarks(gpsPointList);

        List<StayPoint> stayPointList = new ArrayList<>(stayPointMarks.size());
        for (StayPointMark mark : stayPointMarks) {
            List<GpsPoint> stayPointCoordinates = gpsPointList.subList(mark.startIndex, mark.endIndex);
            stayPointList.add(new StayPoint(trajectory.getOid(), stayPointCoordinates));
        }

        return stayPointList;
    }

    public List<Trajectory> sliceTrajectory(Trajectory trajectory) {
        List<GpsPoint> gpsPointList = trajectory.getSortedGpsList();
        List<StayPointMark> stayPointMarks = this.calStayPointMarks(gpsPointList);

        int trajStartIndex = 0;
        List<Trajectory> subTrajectories = new ArrayList<>();
        for (StayPointMark mark : stayPointMarks) {
            if (trajStartIndex < mark.startIndex) {
                //当GPS点数量小于2，不能构成一个轨迹，直接丢弃
                List<GpsPoint> subGpsList = gpsPointList.subList(trajStartIndex, mark.startIndex + 1); // 轨迹中包含驻留点的第一个GPS点
                subTrajectories.add(new Trajectory(trajectory.getOid(), subGpsList, true));
            }
            trajStartIndex = mark.endIndex - 1; // 轨迹包含驻留点的最后一个GPS点
        }

        if (trajStartIndex < gpsPointList.size() - 1) {
            // 最后一段子轨迹不要忘记
            subTrajectories.add(new Trajectory(trajectory.getOid(), gpsPointList.subList(trajStartIndex, gpsPointList.size())));
        }

        return subTrajectories;
    }

    private List<StayPointMark> calStayPointMarks(List<GpsPoint> gpsPointList) {
        List<StayPointMark> stayPointMarks = new ArrayList<>();

        int currIndex = 0;
        while (currIndex < gpsPointList.size()) {
            int endIndex = getFirstExceedIndex(gpsPointList, currIndex);

            GpsPoint anchorPoint = gpsPointList.get(currIndex);
            GpsPoint lastPoint = gpsPointList.get(endIndex - 1);
            boolean isExceedTime = isExceedMinTimeThreshold(anchorPoint, lastPoint);
            if (isExceedTime) {
                stayPointMarks.add(new StayPointMark(currIndex, endIndex));
                currIndex = endIndex;
            } else {
                currIndex++;
            }
        }

        return stayPointMarks;
    }

    public int getFirstExceedIndex(List<GpsPoint> gpsPointList, int anchorIndex) {

        GpsPoint anchor = gpsPointList.get(anchorIndex);
        int currIndex = anchorIndex + 1;
        while (currIndex < gpsPointList.size()) {
            if (isExceedMaxDistThreshold(anchor, gpsPointList.get(currIndex))) {
                return currIndex;
            }
            currIndex++;
        }
        return currIndex;
    }

    public boolean isExceedMinTimeThreshold(GpsPoint first, GpsPoint last) {
        return first.getTime().toInstant().plusSeconds(minStayTimeInSecond).isBefore(last.getTime().toInstant());
    }

    public boolean isExceedMaxDistThreshold(GpsPoint from, GpsPoint to) {
        return DistanceUtil.distInMeter(from.getGeom(), to.getGeom()) > maxStayDistInMeter;
    }

    private static class StayPointMark {
        public final int startIndex;
        public final int endIndex;

        public StayPointMark(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    public static void main(String[] args) throws Exception {
        List<GpsPoint> gpsList = TrajNoiseFilter.readGpsPoint();
        Trajectory rawTrajectory = new Trajectory("oid", gpsList, true);
        TrajStayPointDetector stayPointDetector = new TrajStayPointDetector(50, 100);

        int count = 0;
        List<StayPoint> stayPointList = stayPointDetector.detectStayPoint(rawTrajectory);
        for (StayPoint stayPoint : stayPointList) {
            count += stayPoint.gpsPointList.size();
        }

        List<Trajectory> subTrajectoryList = stayPointDetector.sliceTrajectory(rawTrajectory);
        for (Trajectory subTrajectory : subTrajectoryList) {
            count += subTrajectory.getNumPoints();
        }

        assert (count - subTrajectoryList.size() * 2 == rawTrajectory.getNumPoints());
    }
}
