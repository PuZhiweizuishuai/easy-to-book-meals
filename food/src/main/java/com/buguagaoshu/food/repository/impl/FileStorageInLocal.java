package com.buguagaoshu.food.repository.impl;

import com.buguagaoshu.food.config.FileConfig;
import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.FileData;
import com.buguagaoshu.food.dto.VditorFiles;
import com.buguagaoshu.food.enums.FileExternalTypeEnum;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.FileStorageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地文件存储实现
 * 职责：负责文件的物理存储操作，不涉及数据库操作
 * */
@Slf4j
@Repository
public class FileStorageInLocal implements FileStorageRepository {
    // 定义最大文件大小为20MB（字节单位）
    private static final long MAX_SIZE = 20 * 1024 * 1024; // 20MB

    private final Path location;

    public FileStorageInLocal() {
        this.location = Paths.get(FileConfig.ROOT);
    }

    @Override
    public VditorFiles vditorFileSave(MultipartFile[] files, Long userId) {
        VditorFiles vditorFiles = new VditorFiles();
        Map<String, Object> succMap = new HashMap<>(files.length);
        List<String> errFiles = new ArrayList<>();

        // 先检查所有文件大小
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_SIZE) {
                errFiles.add(file.getOriginalFilename());
                Map<String, Object> data = new HashMap<>(2);
                vditorFiles.setCode(0);
                vditorFiles.setMsg("上传失败，文件大小超过 20M");
                data.put("succMap", succMap);
                data.put("errFiles", errFiles);
                vditorFiles.setData(data);
                return vditorFiles;
            }
        }

        for (MultipartFile file : files) {
            String path = FileConfig.filePath();
            String originalFilename = file.getOriginalFilename();
            String suffix = FileConfig.getSuffix(originalFilename);
            if (!FileConfig.isAllowedExtension(suffix)) {
                errFiles.add(originalFilename);
                continue;
            }
            String filename = FileConfig.newFilename(suffix);
            File dest = new File(path);
            if (!dest.exists() && !dest.mkdirs()) {
                log.error("创建目录失败: {}", path);
                errFiles.add(originalFilename);
                continue;
            }
            try {
                Path targetPath = Paths.get(path, filename);
                Files.copy(file.getInputStream(), targetPath);
                // 返回文件的访问路径
                String fileUrl = "/" + path + "/" + filename;
                succMap.put(originalFilename, fileUrl);
            } catch (Exception e) {
                log.error("文件保存失败: {}", originalFilename, e);
                errFiles.add(originalFilename);
            }
        }

        Map<String, Object> data = new HashMap<>(2);
        vditorFiles.setCode(0);
        vditorFiles.setMsg(errFiles.isEmpty() ? "上传成功" : "部分文件上传失败");
        data.put("succMap", succMap);
        data.put("errFiles", errFiles);
        vditorFiles.setData(data);
        return vditorFiles;
    }

    @Override
    public List<FileData> save(MultipartFile[] files, FileExternalTypeEnum type, Long userId) throws FileNotFoundException {
        List<FileData> fileDataList = new ArrayList<>();
        
        if (files == null || files.length == 0) {
            return fileDataList;
        }

        String path = FileConfig.filePath();
        File dest = new File(path);
        if (!dest.exists() && !dest.mkdirs()) {
            throw new FileNotFoundException("创建文件目录失败: " + path);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String originalFilename = file.getOriginalFilename();
            String suffix = FileConfig.getSuffix(originalFilename);
            if (!FileConfig.isAllowedExtension(suffix)) {
                throw new BusinessException("不支持的文件类型，仅允许上传图片文件");
            }
            String newFilename = FileConfig.newFilename(suffix);
            
            try {
                Path targetPath = Paths.get(path, newFilename);
                Files.copy(file.getInputStream(), targetPath);
                
                // 构建 FileData 对象（不包含数据库ID，由上层服务处理）
                FileData fileData = buildFileData(file, type, userId, path, originalFilename, newFilename, suffix);
                fileDataList.add(fileData);
                
                log.info("文件保存成功: {}", newFilename);
            } catch (IOException e) {
                log.error("文件保存失败: {}", originalFilename, e);
                throw new FileNotFoundException("文件保存失败: " + originalFilename);
            }
        }

        return fileDataList;
    }

    @Override
    public Path load(String filePath) throws FileNotFoundException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        return path;
    }

    @Override
    public boolean deleteFileWithDatabase(FileData fileData) {
        // 本层只负责物理文件删除，数据库删除由上层服务处理
        return deleteFile(fileData);
    }

    @Override
    public boolean deleteFile(FileData fileData) {
        if (fileData == null || fileData.getFileUrl() == null) {
            return false;
        }
        
        try {
            // 移除开头的 "/" 如果存在
            String filePath = fileData.getFileUrl();
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }
            
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("文件删除成功: {}", filePath);
            } else {
                log.warn("文件不存在或删除失败: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("文件删除异常: {}", fileData.getFileUrl(), e);
            return false;
        }
    }

    /**
     * 批量删除物理文件
     * @param fileDataList 文件数据列表
     * @return 删除成功的数量
     */
    public int deleteFiles(List<FileData> fileDataList) {
        if (fileDataList == null || fileDataList.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        for (FileData fileData : fileDataList) {
            if (deleteFile(fileData)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 构建 FileData 对象
     */
    private FileData buildFileData(MultipartFile file, FileExternalTypeEnum type, Long userId,
                                   String path, String originalFilename, 
                                   String newFilename, String suffix) {
        FileData fileData = new FileData();
        fileData.setFileUrl("/api/" + path + "/" + newFilename);
        fileData.setFileOriginalName(originalFilename);
        fileData.setFileNewName(newFilename);
        fileData.setSize(file.getSize());
        fileData.setUploadTime(System.currentTimeMillis());
        fileData.setUploadUserId(userId);
        fileData.setType(type.getCode());
        fileData.setSuffixName(suffix);
        fileData.setStatus(WebConstant.TEMP_FILE); // 默认为临时文件状态
        return fileData;
    }
}
