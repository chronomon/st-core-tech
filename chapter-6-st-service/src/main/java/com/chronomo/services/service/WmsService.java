package com.chronomo.services.service;

import com.chronomo.services.util.CrsUtil;
import com.chronomo.services.util.DataStoreCacheHelper;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @Description
 * @Author Sui Yuan
 * @Date 2023/12/20 16:18
 * @Version 1.0
 */
@Slf4j
@Service
public class WmsService {

    private static final String POI_SFT_MAME = "poi";

    /**
     * 获取地图图片字节数组
     *
     * @param bbox   地理范围
     * @param width  图片宽度
     * @param height 图片高度
     * @param crsStr 地理范围坐标系
     * @return
     * @throws FactoryException
     * @throws IOException
     */
    public byte[] getMap(String bbox, int width, int height, String crsStr) throws FactoryException, IOException, TransformException {
        byte[] wmsBytes = new byte[0];
        DataStore dataStore = DataStoreCacheHelper.getInstance().getDataStore();
        SimpleFeatureSource simpleFeatureSource = dataStore.getFeatureSource(POI_SFT_MAME);
        //1.计算切片请求地理范围
        String[] bboxStrArray = bbox.split(",");
        double tempLonMin = Double.parseDouble(bboxStrArray[0]), tempLatMin = Double.parseDouble(bboxStrArray[1]), tempLonMax = Double.parseDouble(bboxStrArray[2]), tempLatMax = Double.parseDouble(bboxStrArray[3]);
        ReferencedEnvelope srcBound = simpleFeatureSource.getBounds();
        ReferencedEnvelope bound = new ReferencedEnvelope(tempLonMin, tempLonMax, tempLatMin, tempLatMax, srcBound.getCoordinateReferenceSystem());
        ReferencedEnvelope queryBound = CrsUtil.envTransform(bound, crsStr, CrsUtil.WGS84);
        ReferencedEnvelope intersect = srcBound.intersection(queryBound);
        if (intersect != null && intersect.getArea() > 0) {
            MapContent mapContent = new MapContent();

            //默认样式
            Style style = SLD.createSimpleStyle(simpleFeatureSource.getSchema());
            FeatureLayer featureLayer = new FeatureLayer(simpleFeatureSource, style);
            //请求单个图层
            mapContent.addLayer(featureLayer);

            // 初始化渲染器
            StreamingRenderer sr = new StreamingRenderer();
            sr.setMapContent(mapContent);
            // 初始化输出图像
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.getGraphics();
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Rectangle rect = new Rectangle(0, 0, width, height);
            // 绘制地图
            log.info("bound ---------> " + bound);
            sr.paint((Graphics2D) g, rect, bound);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", out);
            wmsBytes = out.toByteArray();

            //清空所有图层
//            mapContent.layers().forEach(layer -> layer.getFeatureSource().getDataStore().dispose());
            mapContent.dispose();
        }

        return wmsBytes;
    }
}
