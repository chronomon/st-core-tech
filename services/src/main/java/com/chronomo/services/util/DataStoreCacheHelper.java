package com.chronomo.services.util;

import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;

import java.io.IOException;
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
        Map params = new HashMap();
        params.put("dbtype", "geopkg");
        params.put("database", "/D:/2-data/2-personal/4-data/st-core-tech/service_data.gpkg");
        params.put("read-only", true);
//
//        String catalog = PropertiesHelper.getInstance().getValue("cluster.config", "catalog");
//        String zookeepers = PropertiesHelper.getInstance().getValue("cluster.config", "hbase.zookeepers");
//        Map<String, String> params = new HashMap<>(2);
//        params.put("hbase.zookeepers", zookeepers);
//        params.put("hbase.catalog", catalog);
        return DataStoreFinder.getDataStore(params);
    }
}
