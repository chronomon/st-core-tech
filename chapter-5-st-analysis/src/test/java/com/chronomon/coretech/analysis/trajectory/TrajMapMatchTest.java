package com.chronomon.coretech.analysis.trajectory;

import com.chronomon.analysis.trajectory.mapmatch.HmmMapMatcher;
import com.chronomon.analysis.trajectory.mapmatch.MapMatchTrajectory;
import com.chronomon.analysis.trajectory.model.GpsPoint;
import com.chronomon.analysis.trajectory.model.Trajectory;
import com.chronomon.analysis.trajectory.road.DirectionEnum;
import com.chronomon.analysis.trajectory.road.RoadNetwork;
import com.chronomon.analysis.trajectory.road.RoadSegment;
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

public class TrajMapMatchTest {

    @Test
    public void test() throws IOException, ParseException, URISyntaxException {
        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("bus_trajectory.txt")).toURI();
        List<String> lines = Files.readAllLines(Paths.get(uri));
        String wkt = lines.get(0);
        LineString lineString = (LineString) new WKTReader().read(wkt);

        // 构造轨迹
        List<GpsPoint> gpsList = new ArrayList<>(lineString.getNumPoints());
        for (int i = 0; i < lineString.getNumPoints(); i++) {
            Point geom = lineString.getPointN(i);
            Timestamp time = new Timestamp((long) geom.getCoordinate().getM());
            gpsList.add(new GpsPoint("oid", lineString.getPointN(i), time));
        }
        Trajectory trajectory = new Trajectory("oid", gpsList, true);

        // 构造路网
        RoadNetwork rn = new RoadNetwork(getRoadSegments(), true);

        // 轨迹地图匹配
        HmmMapMatcher mapMatcher = new HmmMapMatcher(rn, 50.0);
        List<MapMatchTrajectory> matchLines = mapMatcher.mapMatch(trajectory);

        // 结果输出
        for (MapMatchTrajectory matchTrajectory : matchLines) {
            //System.out.println(matchTrajectory.startTime);
            //System.out.println(matchTrajectory.endTime);
            System.out.println(matchTrajectory.matchedPath.toText());
        }
    }

    private List<RoadSegment> getRoadSegments() throws IOException, URISyntaxException, ParseException {
        // 文件参数
        boolean hasHeader = true;
        String separator = "\t";
        int roadLineIndex = 0;
        int directionIndex = 4;

        // 构造路网
        List<RoadSegment> roadSegmentList = new ArrayList<>();

        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("road.csv")).toURI();
        List<String> lines = Files.readAllLines(Paths.get(uri));
        int count = 0;
        for (String line : lines) {
            if (hasHeader && count == 0) {
                count++;
                continue;
            }
            String[] attrs = line.split(separator);
            LineString roadLine = (LineString) (new WKTReader().read(attrs[roadLineIndex]));
            DirectionEnum directionEnum = DirectionEnum.UN_KNOWN;
            try {
                int code = Integer.parseInt(attrs[directionIndex]);
                directionEnum = DirectionEnum.getByCode(code);
            } catch (Exception ignore) {
            }
            RoadSegment segment = new RoadSegment(roadLine, directionEnum);

            roadSegmentList.add(segment);
        }

        return roadSegmentList;
    }
}