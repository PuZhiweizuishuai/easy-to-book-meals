package com.buguagaoshu.food.config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class FileConfig {
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");

    public final static String ROOT = "files";

    public final static String OSS = "oss";

    public static int FILE_SAVE_LOCATION = 0;

    /**
     * 允许上传的文件扩展名白名单（仅图片，防止上传 .jsp/.html 等可执行或可存储型 XSS 的文件）
     */
    private final static Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    );

    /**
     * 校验文件扩展名是否在白名单内
     */
    public static boolean isAllowedExtension(String suffix) {
        return suffix != null && ALLOWED_EXTENSIONS.contains(suffix.toLowerCase());
    }

    public static String filePath() {
        return ROOT + "/" + DATE_FORMAT.format(new Date());
    }

    public static String ossFilePath() {
        return DATE_FORMAT.format(new Date());
    }

    public static String ossFileURL(int saveLocation, String path, String fileName) {
        return  "/api/files" + saveLocation + "/oss/" + path + "/" + fileName;
    }

    public static String newFilename(String suffix) {
        return UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    /**
     * 获取文件后缀名
     */
    public static String getSuffix(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
