package com.chronomo.services.service;

import com.chronomo.services.util.QueryUtil;
import org.geotools.filter.text.cql2.CQLException;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @Description
 * @Author Sui Yuan
 * @Date 2023/12/12 14:59
 * @Version 1.0
 */
@Service
public class SpFuncService {

    private static final String POI_SFT_MAME = "poi";


    public String queryByPlg(String plgWkt) throws IOException, CQLException {
        return QueryUtil.queryFeaturesByPlg(POI_SFT_MAME, plgWkt);
    }

    public String queryByBbox(double minx, double miny, double maxx, double maxy) throws IOException, CQLException {
        return QueryUtil.queryFeaturesByBBOX(POI_SFT_MAME, minx, miny, maxx, maxy);
    }

    public String queryByRadius(double x, double y, double radiusM) throws IOException, CQLException {
        return QueryUtil.queryFeaturesByRadius(POI_SFT_MAME, x, y, radiusM);
    }
}
