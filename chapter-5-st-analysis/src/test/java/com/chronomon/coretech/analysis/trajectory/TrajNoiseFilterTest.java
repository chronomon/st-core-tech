package com.chronomon.coretech.analysis.trajectory;

import com.chronomon.coretech.analysis.trajectory.filter.TrajNoiseFilter;
import com.chronomon.coretech.analysis.trajectory.model.GpsPoint;
import com.chronomon.coretech.analysis.trajectory.model.Trajectory;
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

public class TrajNoiseFilterTest {

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

        TrajNoiseFilter noiseFilter = new TrajNoiseFilter(10.0);
        List<Trajectory> subTrajectoryList = noiseFilter.filter(new Trajectory("oid", gpsList, true));
        System.out.println(subTrajectoryList.size());

        int count = 0;
        for (Trajectory sub : subTrajectoryList) {
            count += sub.getNumPoints();
        }

        System.out.println("raw:" + gpsList.size());
        System.out.println("clean:" + count);

        if (subTrajectoryList.size() == 1) {
            System.out.println("没有检测到噪点");
        } else {
            if (count == gpsList.size()) {
                System.out.println("去噪轨迹速度设置偏小，GPS点被误认为是噪点");
            } else {
                System.out.println("检测到噪点，数量为：" + (gpsList.size() - count));
            }
        }
    }
}