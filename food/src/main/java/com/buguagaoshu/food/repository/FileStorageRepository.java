package com.buguagaoshu.food.repository;


import com.buguagaoshu.food.data.FileData;
import com.buguagaoshu.food.dto.VditorFiles;
import com.buguagaoshu.food.enums.FileExternalTypeEnum;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

public interface FileStorageRepository {
    /**
     * 使用 Vditor 编辑器上传时返回的数据
     * TODO 待完成
     * */
    VditorFiles vditorFileSave(MultipartFile[] files, Long userId);

    /**
     * 文件上传接口
     * @param type 文件外部关联类型 0 店面
     *                             1 食品
     *             具体内容参考 FileExternalTypeEnum 类
     * @param userId 用户ID
     *
     * */
    List<FileData> save(MultipartFile[] files, FileExternalTypeEnum type, Long userId) throws FileNotFoundException;


    /**
     * 加载文件
     * */
    Path load(String filePath) throws FileNotFoundException;

    /**
     * 删除本地文件和数据库文件
     *
     */
    boolean deleteFileWithDatabase(FileData FileData);


    /**
     * 删除本地文件
     * */
    boolean deleteFile(FileData FileData);
}
