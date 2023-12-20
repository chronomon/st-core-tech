package com.chronomo.services.controller;

import com.chronomo.services.service.WmsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * wms的getMap接口
     *
     * @param bbox   地理范围 minLon,minLat,maxLon,maxLat
     * @param width  图片宽度 256
     * @param height 图片高度 256
     * @param crs    bbox的坐标系EPSG:4326
     * @return 图片字节数组
     */
    @RequestMapping(value = "/wms/getMap", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getMap(@RequestParam(required = false, value = "BBOX") String bbox
            , @RequestParam(required = false, value = "WIDTH") Integer width
            , @RequestParam(required = false, value = "HEIGHT") Integer height
            , @RequestParam(required = false, value = "CRS", defaultValue = "EPSG:4326") String crs) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        byte[] pngBytes = wmsService.getMap(bbox, width, height, crs);
        return new ResponseEntity<>(pngBytes, headers, HttpStatus.OK);
    }
}
