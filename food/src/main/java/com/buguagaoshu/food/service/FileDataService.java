package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.FileData;
import com.buguagaoshu.food.dto.VditorFiles;
import com.buguagaoshu.food.enums.FileExternalTypeEnum;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.FileDataRepository;
import com.buguagaoshu.food.repository.FileStorageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件数据服务层
 * 
 * 架构分层说明：
 * - Service层（本类）：负责业务逻辑编排，协调文件存储和数据库操作
 * - Repository层（FileStorageRepository）：负责物理文件存储操作
 * - Repository层（FileDataRepository）：负责数据库CRUD操作
 * 
 * 设计原则：
 * 1. 文件存储与数据库操作分离
 * 2. 事务管理确保数据一致性
 * 3. 异常处理确保文件和数据库状态一致
 */
@Slf4j
@Service
public class FileDataService {

    private final FileDataRepository fileDataRepository;
    private final FileStorageRepository fileStorageRepository;

    public FileDataService(FileDataRepository fileDataRepository, 
                          FileStorageRepository fileStorageRepository) {
        this.fileDataRepository = fileDataRepository;
        this.fileStorageRepository = fileStorageRepository;
    }

    // ==================== 文件上传 ====================

    /**
     * Vditor编辑器文件上传
     * 仅保存文件到本地，不写入数据库（适用于富文本编辑器的临时上传）
     *
     * @param files  上传的文件数组
     * @param userId 用户ID
     * @return VditorFiles 响应对象
     */
    public VditorFiles uploadForVditor(MultipartFile[] files, Long userId) {
        if (files == null || files.length == 0) {
            VditorFiles result = new VditorFiles();
            result.setCode(1);
            result.setMsg("上传文件不能为空");
            return result;
        }
        return fileStorageRepository.vditorFileSave(files, userId);
    }

    /**
     * 上传文件并保存到数据库
     * 先保存物理文件，成功后写入数据库记录
     *
     * @param files  上传的文件数组
     * @param type   文件类型（0-店面关联，1-食品关联）
     * @param userId 用户ID
     * @return 保存成功的文件数据列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<FileData> uploadAndSave(MultipartFile[] files, FileExternalTypeEnum type, Long userId) {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        try {
            // 第一步：保存物理文件
            List<FileData> fileDataList = fileStorageRepository.save(files, type, userId);
            
            if (fileDataList.isEmpty()) {
                return new ArrayList<>();
            }

            // 第二步：保存到数据库
            List<FileData> savedList = new ArrayList<>();
            for (FileData fileData : fileDataList) {
                FileData saved = fileDataRepository.save(fileData);
                savedList.add(saved);
            }

            log.info("文件上传并保存成功，数量: {}", savedList.size());
            return savedList;

        } catch (FileNotFoundException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
    }



    // ==================== 文件更新 ====================

    /**
     * 关联文件到外部实体
     * 将临时文件关联到具体的店面或食品
     *
     * @param files    文件列表
     * @param externalId 外部实体ID
     * @param status     状态（1-已发布）
     * @return 更新成功的文件数量
     */
    @Transactional(rollbackFor = Exception.class)
    public List<FileData> associateFiles(List<FileData> files, Long externalId, Integer status, FileExternalTypeEnum type) {
        // TODO 数据校验，仅允许修改自己店面的图片
        List<FileData> fileDataList = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return fileDataList;
        }
        List<Long> fileIds = files.stream().map(FileData::getId).toList();
        // 获取数据库文件
        List<FileData> dbfiles = fileDataRepository.findAllByIdIn(fileIds);
        for (FileData fileData : dbfiles) {
            fileData.setExternalId(externalId);
            fileData.setStatus(status);
            fileData.setType(type.getCode());
        }
        fileDataRepository.saveAll(dbfiles);

        return dbfiles;
    }

    /**
     * 更新文件状态
     *
     * @param id     文件ID
     * @param status 新状态
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        Optional<FileData> optional = fileDataRepository.findById(id);
        if (optional.isPresent()) {
            FileData fileData = optional.get();
            fileData.setStatus(status);
            fileDataRepository.save(fileData);
            return true;
        }
        return false;
    }

    /**
     * 批量更新文件状态
     *
     * @param fileIds 文件ID列表
     * @param status  新状态
     * @return 更新成功的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(List<Long> fileIds, Integer status) {
        if (fileIds == null || fileIds.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Long fileId : fileIds) {
            if (updateStatus(fileId, status)) {
                count++;
            }
        }
        return count;
    }

    // ==================== 文件删除 ====================

    /**
     * 删除单个文件（同时删除物理文件和数据库记录）
     *
     * @param id 文件ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        Optional<FileData> optional = fileDataRepository.findById(id);
        if (optional.isEmpty()) {
            log.warn("文件不存在，ID: {}", id);
            return false;
        }

        FileData fileData = optional.get();
        
        // 第一步：删除物理文件
        boolean fileDeleted = fileStorageRepository.deleteFile(fileData);
        if (!fileDeleted) {
            log.warn("物理文件删除失败或文件不存在: {}", fileData.getFileUrl());
            // 继续删除数据库记录
        }

        // 第二步：删除数据库记录
        fileDataRepository.deleteById(id);
        log.info("文件删除成功，ID: {}", id);
        return true;
    }

    /**
     * 批量删除文件
     *
     * @param ids 文件ID列表
     * @return 删除成功的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Long id : ids) {
            if (deleteById(id)) {
                count++;
            }
        }
        log.info("批量删除文件完成，成功: {}/总数: {}", count, ids.size());
        return count;
    }

    /**
     * 根据外部关联ID删除文件
     *
     * @param externalId 外部实体ID
     * @param type       文件类型
     * @return 删除成功的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteByExternalId(Long externalId, Integer type) {
//        List<FileData> files = findByExternalIdAndType(externalId, type);
//        if (files.isEmpty()) {
//            return 0;
//        }
//
//        int count = 0;
//        for (FileData fileData : files) {
//            if (deleteById(fileData.getId())) {
//                count++;
//            }
//        }
//        return count;
        // TODO 需重新实现
        return 0;
    }

    /**
     * 清理临时文件（未关联的文件）
     * 建议定时任务调用此方法清理过期临时文件
     *
     * @param beforeTime 上传时间在此之前（毫秒时间戳）
     * @return 清理的文件数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanTempFiles(Long beforeTime) {
//        List<FileData> tempFiles = findByStatus(0);
//        if (tempFiles.isEmpty()) {
//            return 0;
//        }
//
//        int count = 0;
//        for (FileData fileData : tempFiles) {
//            if (fileData.getUploadTime() != null && fileData.getUploadTime() < beforeTime) {
//                if (deleteById(fileData.getId())) {
//                    count++;
//                }
//            }
//        }
//        log.info("清理临时文件完成，数量: {}", count);
        // TODO 需重新实现
        return 0;
    }

    // ==================== 文件加载 ====================

    /**
     * 加载文件路径（用于文件下载或预览）
     *
     * @param fileId 文件ID
     * @return 文件路径
     * @throws FileNotFoundException 文件不存在时抛出
     */
    public Path loadFile(Long fileId) throws FileNotFoundException {
        Optional<FileData> optional = fileDataRepository.findById(fileId);
        if (optional.isEmpty()) {
            throw new FileNotFoundException("文件记录不存在，ID: " + fileId);
        }

        FileData fileData = optional.get();
        return fileStorageRepository.load(fileData.getFileUrl());
    }

    /**
     * 根据文件路径加载文件
     *
     * @param fileUrl 文件URL路径
     * @return 文件路径
     * @throws FileNotFoundException 文件不存在时抛出
     */
    public Path loadFileByUrl(String fileUrl) throws FileNotFoundException {
        return fileStorageRepository.load(fileUrl);
    }

}
