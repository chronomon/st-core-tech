package com.chronomo.services.controller;

import com.chronomo.services.model.ResponseResult;
import com.chronomo.services.service.SpFuncService;
import org.geotools.filter.text.cql2.CQLException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @Description
 * @Author Sui Yuan
 * @Date 2023/12/12 14:59
 * @Version 1.0
 */
@RestController
@RequestMapping("/spfunc")
public class SpFuncController {

    @Resource
    private SpFuncService spFuncService;

    @GetMapping("/hello")
    public ResponseResult<String> hello(@RequestParam("name") String name) {
        return ResponseResult.success("hello " + name);
    }

    @GetMapping("/query-by-plg")
    public ResponseResult<String> queryByPlg(@RequestParam("plgwkt") String plgwkt) {
        String result;
        try {
            result = spFuncService.queryByPlg(plgwkt);
        } catch (IOException | CQLException e) {
            return ResponseResult.fail(e.getMessage());
        }
        return ResponseResult.success(result);
    }

    @GetMapping("/query-by-bbox")
    public ResponseResult<String> queryByBbox(@RequestParam("minx") double minx, @RequestParam("miny") double miny, @RequestParam("maxx") double maxx, @RequestParam("maxy") double maxy) {
        String result;
        try {
            result = spFuncService.queryByBbox(minx, miny, maxx, maxy);
        } catch (IOException | CQLException e) {
            return ResponseResult.fail(e.getMessage());
        }
        return ResponseResult.success(result);
    }

    @GetMapping("/query-by-radius")
    public ResponseResult<String> queryByRadius(@RequestParam("x") double x, @RequestParam("y") double y, @RequestParam("radiusM") double radiusM) {
        String result;
        try {
            result = spFuncService.queryByRadius(x, y, radiusM);
        } catch (IOException | CQLException e) {
            return ResponseResult.fail(e.getMessage());
        }
        return ResponseResult.success(result);
    }

}
