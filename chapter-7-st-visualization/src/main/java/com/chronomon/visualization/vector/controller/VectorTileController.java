package com.chronomon.visualization.vector.controller;

import com.chronomon.visualization.vector.service.ITileService;
import com.chronomon.visualization.vector.model.TileCoord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/vector-tile")
public class VectorTileController {

    @Resource
    private ITileService tileService;

    /**
     * 1. 本地启动地图服务
     * 2. 在QGIS的的Vector Tiles中添加配置：http://localhost:8080/st-visualization/vector-tile/dynamic/{z}/{y}/{x}.pbf
     * 3. 然后在QGIS中便可动态请求并渲染矢量瓦片
     */

    @GetMapping("/dynamic/{z}/{y}/{x}.pbf")
    public void pixelTile(@PathVariable Integer z,
                          @PathVariable Integer y,
                          @PathVariable Integer x,
                          HttpServletResponse response) {

        try {
            response.setContentType("application/x-protobuf");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode(x.toString(), StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=utf-8''" + encodedFileName + ".pbf");

            TileCoord tileCoord = new TileCoord(y, x, z);
            byte[] vectorTile = tileService.getTile(tileCoord);
            response.getOutputStream().write(vectorTile);
        } catch (Exception e) {
            // 重置response
            log.error("文件下载失败" + e.getMessage());
            throw new RuntimeException("下载文件失败", e);
        }
    }
}
