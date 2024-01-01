package com.chronomo.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

/**
 * @Description
 * @Author Sui Yuan
 * @Date 2023/12/20 16:18
 * @Version 1.0
 */
@Slf4j
@Service
public class WmtsService {

    private final static String path;

    static {
        URL url = WmtsService.class.getClassLoader().getResource("data/tile");
        assert url != null;
        path = url.getPath();
    }

    /**
     * 获取文件系统切片
     *
     * @param z 缩放级别
     * @param y 行号
     * @param x 列号
     */
    public byte[] getTile(int z, int y, int x) {
        byte[] tileData = new byte[0];
        String suffix = ".png";

        File tileFile = new File(path + File.separator + z + File.separator + y + File.separator + x + suffix);
        log.info("tile path : " + tileFile.getAbsolutePath());

        if (tileFile.exists()) {
            tileData = readFiletoBytes(tileFile);
        }
        return tileData;
    }

    /**
     * 将本地文件读取成byte数组
     */
    public byte[] readFiletoBytes(File file) {
        byte[] tileData = new byte[0];
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            tileData = bos.toByteArray();
            bos.close();
        } catch (Exception e) {
            log.error("读取" + file.getAbsolutePath() + "出错 " + e);
        }
        return tileData;
    }
}
