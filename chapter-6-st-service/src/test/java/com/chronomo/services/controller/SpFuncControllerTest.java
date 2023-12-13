package com.chronomo.services.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @Description
 * @Author Sui Yuan
 * @Date 2023/12/13 10:48
 * @Version 1.0
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@WebAppConfiguration
class SpFuncControllerTest {

    MockMvc mvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void queryByPlg() throws Exception {
        String content = mvc.perform(get("/spfunc/query-by-plg?plgwkt=Polygon ((120.61045595303342282 31.29702097551869855, 120.61013599860429224 31.29362145970915066, 120.61017599290792646 31.29098183566879499, 120.61553522959592044 31.29142177300885308, 120.61665507009789167 31.29322151667273388, 120.61289560555556477 31.29690099260777458, 120.61289560555556477 31.29690099260777458, 120.61045595303342282 31.29702097551869855))"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        System.out.println(content);
    }

    @Test
    void queryByBbox() throws Exception {
        String content = mvc.perform(get("/spfunc/query-by-bbox?minx=120.564&miny=31.2792&maxx=120.6024&maxy=31.3234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        System.out.println(content);
    }

    @Test
    void queryByRadius() throws Exception {
        String content = mvc.perform(get("/spfunc/query-by-radius?x=120.5834751999999952&y=31.28725229999999868&radiusM=600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        System.out.println(content);
    }
}