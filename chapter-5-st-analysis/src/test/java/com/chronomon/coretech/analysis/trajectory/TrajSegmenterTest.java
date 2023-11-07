package com.chronomon.coretech.analysis.trajectory;

import com.chronomon.trajectory.model.GpsPoint;
import com.chronomon.trajectory.model.Trajectory;
import com.chronomon.trajectory.segment.TrajSegmenter;
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

public class TrajSegmenterTest {

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
        List<Trajectory> subTrajectoryList = TrajSegmenter.sliceByTimeInterval(rawTrajectory, 300);
        System.out.println(subTrajectoryList.size());

        int count = 0;
        for (Trajectory subTrajectory : subTrajectoryList) {
            System.out.println(subTrajectory.getLineString().toText());
            count += subTrajectory.getNumPoints();
        }
//        System.out.println(rawTrajectory.getNumPoints());
//        System.out.println(count);
    }
}