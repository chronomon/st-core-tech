package com.chronomo.services.controller;

import com.chronomo.services.service.WmsService;
import com.chronomo.services.service.WmtsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @Description
 * @Author Sui Yuan
 * @Date 2023/12/20 16:45
 * @Version 1.0
 */
@RestController
@RequestMapping("/ows")
public class OWSController {

    @Resource
    WmsService wmsService;

    @Resource
    WmtsService wmtsService;

    /**
     * wms的getMap接口
     *
     * @param bbox   地理范围 minLon,minLat,maxLon,maxLat
     * @param width  图片宽度 256
     * @param height 图片高度 256
     * @param crs    bbox的坐标系EPSG:4326
     * @return 图片字节数组
     */
    @RequestMapping(value = "/wms/get-map", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getMap(@RequestParam(required = false, value = "BBOX") String bbox
            , @RequestParam(required = false, value = "WIDTH", defaultValue = "256") Integer width
            , @RequestParam(required = false, value = "HEIGHT", defaultValue = "256") Integer height
            , @RequestParam(required = false, value = "CRS", defaultValue = "EPSG:4326") String crs) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        byte[] pngBytes = wmsService.getMap(bbox, width, height, crs);
        return new ResponseEntity<>(pngBytes, headers, HttpStatus.OK);
    }


    @RequestMapping(value = "/wmts/get-tile/{z}/{y}/{x}.png", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getTile(@PathVariable Integer z
            , @PathVariable Integer y, @PathVariable Integer x) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        byte[] pngBytes = wmtsService.getTile(z, y, x);
        return new ResponseEntity<>(pngBytes, headers, HttpStatus.OK);
    }
}
