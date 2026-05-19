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

    public List<FileData> findFileByExternalId(Long externalId, Integer status, FileExternalTypeEnum type) {
        return fileDataRepository.findByExternalIdAndStatusAndType(externalId, status, type.getCode());
    }


    /**
     * 更新文件关联信息，更新逻辑如下
     * 1、从数据库查询已有文件关联信息，通过 findFileByExternalId 查询
     * 2、将从数据库查询到的文件信息与用户修改后的新文件信息 files 对比，可以直接对比文件ID
     * 3、批量更新数据，新增文件进行新的关联，已移除的文件进行删除，处理删除数据库记录外，还需要删除本地文件
     * 4、返回更新后的数据
     * */
    @Transactional(rollbackFor = Exception.class)
    public List<FileData> updateAssociateFiles(List<FileData> files, Long externalId, Integer status, FileExternalTypeEnum type) {
        // 1、从数据库查询已有文件关联信息
        List<FileData> existingFiles = findFileByExternalId(externalId, status, type);

        // 2、获取新文件的ID集合
        List<Long> newFileIds = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            newFileIds = files.stream().map(FileData::getId).collect(Collectors.toList());
        }

        // 3、找出需要删除的文件（数据库中有但新文件列表中没有）
        List<FileData> filesToDelete = new ArrayList<>();
        for (FileData existingFile : existingFiles) {
            if (!newFileIds.contains(existingFile.getId())) {
                filesToDelete.add(existingFile);
            }
        }

        // 4、删除需要移除的文件（数据库记录和本地文件）
        if (!filesToDelete.isEmpty()) {
            // 删除数据库记录
            fileDataRepository.deleteAll(filesToDelete);
            // 删除本地物理文件
            for (FileData fileToDelete : filesToDelete) {
                fileStorageRepository.deleteFile(fileToDelete);
            }
            log.info("删除文件关联，数量: {}", filesToDelete.size());
        }

        // 5、找出需要新增关联的文件（新文件列表中有但数据库中没有）
        List<FileData> filesToAdd = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            List<Long> existingFileIds = existingFiles.stream().map(FileData::getId).collect(Collectors.toList());
            for (FileData file : files) {
                if (!existingFileIds.contains(file.getId())) {
                    filesToAdd.add(file);
                }
            }
        }

        // 6、对需要新增关联的文件进行更新
        List<FileData> resultFiles = new ArrayList<>();
        if (!filesToAdd.isEmpty()) {
            resultFiles = associateFiles(filesToAdd, externalId, status, type);
            log.info("新增文件关联，数量: {}", resultFiles.size());
        }

        // 7、保留已存在且未被删除的文件关联
        for (FileData existingFile : existingFiles) {
            if (newFileIds.contains(existingFile.getId())) {
                resultFiles.add(existingFile);
            }
        }

        return resultFiles;
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
