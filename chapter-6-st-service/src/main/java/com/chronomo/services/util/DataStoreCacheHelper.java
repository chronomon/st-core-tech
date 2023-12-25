package com.chronomo.services.util;

import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : suiyuan
 * @description : geomesa数据库连接缓存器
 * @date : Created in 2019-10-11 08:40
 * @modified by :
 **/
@Slf4j
public class DataStoreCacheHelper {
    /**
     * 单例
     */
    private static volatile DataStoreCacheHelper instance;

    /**
     * DataStore
     */
    private DataStore dataStore;

    public static DataStoreCacheHelper getInstance() throws IOException {
        if (instance == null) {
            synchronized (DataStoreCacheHelper.class) {
                if (instance == null) {
                    instance = new DataStoreCacheHelper();
                }
            }
        }
        return instance;
    }

    private DataStoreCacheHelper() throws IOException {
        log.info("开始连接DataStore...");
        dataStore = initDataStore();
        log.info("DataStore连接成功");
    }

    /**
     * 获取指定名称的datastore对象
     *
     * @return datastore对象
     * @throws IOException IOException
     */
    public DataStore getDataStore() {
        return dataStore;
    }

    /**
     * 初始化datastore
     *
     * @return datastore对象
     * @throws IOException IOException
     */
    private DataStore initDataStore() throws IOException {
        URL url = this.getClass().getClassLoader().getResource("data/service_data.gpkg");
        assert url != null;
        String path = url.getFile();
        Map<String, Object> params = new HashMap<>(3);
        params.put("dbtype", "geopkg");
        params.put("database", path);
        params.put("read-only", true);
        return DataStoreFinder.getDataStore(params);
    }
}
