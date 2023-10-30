package com.chronomon.coretech.analysis.trajectory;

import com.chronomon.coretech.analysis.trajectory.model.GpsPoint;
import com.chronomon.coretech.analysis.trajectory.model.Trajectory;
import com.chronomon.coretech.analysis.trajectory.staypoint.StayPoint;
import com.chronomon.coretech.analysis.trajectory.staypoint.TrajStayPointDetector;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TrajStayPointDetectorTest {

    @Test
    public void test() throws IOException, ParseException, URISyntaxException {
        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("bus_trajectory-raw.txt")).toURI();
        List<String> lines = Files.readAllLines(Paths.get(uri));
        String wkt = lines.get(0);
        LineString lineString = (LineString) new WKTReader().read(wkt);

        List<GpsPoint> gpsList = new ArrayList<>(lineString.getNumPoints());
        for (int i = 0; i < lineString.getNumPoints(); i++) {
            Point geom = lineString.getPointN(i);
            Timestamp time = new Timestamp((long) geom.getCoordinate().getM());
            gpsList.add(new GpsPoint("oid", lineString.getPointN(i), time));
        }

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

        Assert.assertTrue(count - subTrajectoryList.size() * 2 == rawTrajectory.getNumPoints());
    }
}