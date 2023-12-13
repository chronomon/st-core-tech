package com.chronomo.services.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author : suiyuan
 * @description : 集群配置帮助类
 * @date : Created in 2019-10-11 11:31
 * @modified by :
 **/
public class PropertiesHelper {

    /**
     * 属性容器
     */
    private Properties properties;

    /**
     * 单例对象
     */
    private static volatile PropertiesHelper instance;

    /**
     * 获取单例对象
     *
     * @return 单例对象
     */
    public static PropertiesHelper getInstance() {
        if (instance == null) {
            synchronized (PropertiesHelper.class) {
                if (instance == null) {
                    instance = new PropertiesHelper();
                }
            }
        }
        return instance;
    }

    private PropertiesHelper() {
        properties = new Properties();
    }

    public Properties getProperties(String property) {
        properties = new Properties();
        try (InputStream in = PropertiesHelper.class.getClassLoader().getResourceAsStream(property)) {
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException("can't load cluster config file in cluster.properties");
        }
        return properties;
    }

    public String getValue(String property, String key) {
        properties = new Properties();
        try (InputStream in = PropertiesHelper.class.getClassLoader().getResourceAsStream(property)) {
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException("can't load cluster config file in cluster.properties");
        }
        return properties.getProperty(key);
    }
}
