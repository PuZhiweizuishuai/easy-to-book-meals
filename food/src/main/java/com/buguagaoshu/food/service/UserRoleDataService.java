package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.repository.UserRoleDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户-角色关联服务（RBAC-D 域感知）
 * <p>
 * 统一管理系统级角色和门店级角色
 * storeId 为 null 时为系统级角色，不为 null 时为门店级角色
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@Service
public class UserRoleDataService {

    private final UserRoleDataRepository userRoleDataRepository;

    public UserRoleDataService(UserRoleDataRepository userRoleDataRepository) {
        this.userRoleDataRepository = userRoleDataRepository;
    }

    public List<UserRoleData> findAll() {
        List<UserRoleData> result = new ArrayList<>();
        userRoleDataRepository.findAll().forEach(result::add);
        return result;
    }

    public List<UserRoleData> findByUserId(Long userId) {
        return userRoleDataRepository.findByUserId(userId);
    }

    public List<UserRoleData> findByRoleId(Long roleId) {
        return userRoleDataRepository.findByRoleId(roleId);
    }

    public List<UserRoleData> findSystemRolesByUserId(Long userId) {
        return userRoleDataRepository.findByUserIdAndStoreIdIsNull(userId);
    }

    public List<UserRoleData> findStoreRolesByUserId(Long userId) {
        return userRoleDataRepository.findByUserIdAndStoreIdIsNotNull(userId);
    }

    public List<UserRoleData> findByStoreIdAndUserId(Long storeId, Long userId) {
        return userRoleDataRepository.findByStoreIdAndUserId(storeId, userId);
    }

    public List<UserRoleData> findByStoreId(Long storeId) {
        return userRoleDataRepository.findByStoreId(storeId);
    }

    public boolean existsByUserIdAndRoleIdAndStoreId(Long userId, Long roleId, Long storeId) {
        return userRoleDataRepository.existsByUserIdAndRoleIdAndStoreId(userId, roleId, storeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserRoleData save(UserRoleData userRoleData) {
        return userRoleDataRepository.save(userRoleData);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserIdAndRoleIdAndStoreId(Long userId, Long roleId, Long storeId) {
        userRoleDataRepository.deleteByUserIdAndRoleIdAndStoreId(userId, roleId, storeId);
    }
}
