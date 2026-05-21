package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.*;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.UserDataRepository;
import com.buguagaoshu.food.utils.MD5Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 用户服务
 * 提供用户注册、信息修改、查询、角色分配等功能
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@Service
public class UserDataService {

    private final UserDataRepository userDataRepository;
    private final UserRoleDataService userRoleDataService;
    private final RoleDataService roleDataService;
    private final StoreService storeService;

    public UserDataService(UserDataRepository userDataRepository,
                           UserRoleDataService userRoleDataService,
                           RoleDataService roleDataService,
                           StoreService storeService) {
        this.userDataRepository = userDataRepository;
        this.userRoleDataService = userRoleDataService;
        this.roleDataService = roleDataService;
        this.storeService = storeService;
    }

    /**
     * 用户注册
     * 注册成功后自动分配默认角色"顾客"(CUSTOMER)，storeId 为 null 表示系统级角色
     */
    @Transactional(rollbackFor = Exception.class)
    public UserData register(UserData userData) {
        validateRegistration(userData);

        if (StringUtils.hasLength(userData.getPassword())) {
            userData.setPassword(MD5Util.getMD5(userData.getPassword()));
        }

        long time = System.currentTimeMillis();
        userData.setCreateTime(time);
        userData.setUpdateTime(time);
        userData.setStatus(0);
        userData.setGender(userData.getGender() != null ? userData.getGender() : 0);

        UserData savedUser = userDataRepository.save(userData);

        assignDefaultRole(savedUser.getId());

        return savedUser;
    }

    /**
     * 修改用户信息
     * 禁止修改系统级参数：status、角色信息、createTime
     */
    @Transactional(rollbackFor = Exception.class)
    public UserData updateUserInfo(UserData updateData) {
        UserData existing = findById(updateData.getId())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        existing.setNickname(updateData.getNickname());
        existing.setAvatarUrl(updateData.getAvatarUrl());
        existing.setPhone(updateData.getPhone());
        existing.setEmail(updateData.getEmail());
        existing.setGender(updateData.getGender());
        existing.setAddress(updateData.getAddress());
        existing.setUpdateTime(System.currentTimeMillis());

        if (StringUtils.hasLength(updateData.getPassword())) {
            existing.setPassword(MD5Util.getMD5(updateData.getPassword()));
        }

        return userDataRepository.save(existing);
    }

    public Optional<UserData> findById(Long id) {
        return userDataRepository.findById(id);
    }

    public Optional<UserData> findByWxOpenid(String wxOpenid) {
        return userDataRepository.findByWxOpenid(wxOpenid);
    }

    public Optional<UserData> findByPhone(String phone) {
        return userDataRepository.findByPhone(phone);
    }

    public Optional<UserData> findByEmail(String email) {
        return userDataRepository.findByEmail(email);
    }

    public List<UserData> findAll() {
        List<UserData> result = new ArrayList<>();
        userDataRepository.findAll().forEach(result::add);
        return result;
    }

    /**
     * 查询用户信息并关联角色与权限
     */
    public UserData findUserWithRolesAndPermissions(Long userId) {
        UserData user = findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        fillUserRolesAndPermissions(user);
        return user;
    }

    /**
     * 为用户填充角色与权限信息
     */
    public void fillUserRolesAndPermissions(UserData user) {
        List<UserRoleData> userRoles = userRoleDataService.findByUserId(user.getId());

        List<RoleData> roleDataList = new ArrayList<>();
        for (UserRoleData ur : userRoles) {
            RoleData role = RolePermissionCache.ROLE_MAP.get(ur.getRoleId());
            if (role != null && role.getStatus() == 0) {
                List<String> permCodes = RolePermissionCache.ROLE_PERMISSIONS_MAP.get(ur.getRoleId());
                role.setPermissions(permCodes != null ? permCodes : Collections.emptyList());
                roleDataList.add(role);
            }
        }
        user.setRoleDataList(roleDataList);
    }

    /**
     * 修改用户状态（启用/禁用）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserData updateStatus(Long userId, Integer status) {
        UserData user = findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (status != 0 && status != 1) {
            throw new BusinessException(400, "无效的状态值");
        }

        user.setStatus(status);
        user.setUpdateTime(System.currentTimeMillis());
        return userDataRepository.save(user);
    }

    /**
     * 为用户分配系统级角色（storeId 为 null）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserRoleData assignRole(Long userId, Long roleId, Long operatorId) {
        findById(userId).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        roleDataService.findById(roleId).orElseThrow(() -> new BusinessException(404, "角色不存在"));

        List<UserRoleData> existing = userRoleDataService.findByUserId(userId);
        boolean alreadyAssigned = existing.stream()
                .anyMatch(ur -> ur.getRoleId().equals(roleId) && ur.getStoreId() == null);
        if (alreadyAssigned) {
            throw new BusinessException(400, "该系统角色已分配给此用户");
        }

        UserRoleData userRoleData = new UserRoleData();
        userRoleData.setUserId(userId);
        userRoleData.setRoleId(roleId);
        userRoleData.setStoreId(null);
        userRoleData.setCreateUser(operatorId);
        return userRoleDataService.save(userRoleData);
    }

    /**
     * 为用户分配店面级角色
     * 必须由该店面的店长(STORE_ADMIN)操作，否则拒绝
     *
     * @param storeId    店面ID
     * @param userId     被分配角色的用户ID
     * @param roleId     角色ID
     * @param operatorId 操作人ID（必须是该店面店长）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserRoleData assignStoreRole(Long storeId, Long userId, Long roleId, Long operatorId) {
        findById(userId).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        roleDataService.findById(roleId).orElseThrow(() -> new BusinessException(404, "角色不存在"));
        storeService.getStore(storeId);

        validateStoreAdmin(storeId, operatorId);

        List<UserRoleData> existing = userRoleDataService.findByStoreIdAndUserId(storeId, userId);
        boolean alreadyAssigned = existing.stream()
                .anyMatch(ur -> ur.getRoleId().equals(roleId));
        if (alreadyAssigned) {
            throw new BusinessException(400, "该用户在此店面已拥有此角色");
        }

        UserRoleData userRoleData = new UserRoleData();
        userRoleData.setUserId(userId);
        userRoleData.setRoleId(roleId);
        userRoleData.setStoreId(storeId);
        userRoleData.setCreateUser(operatorId);
        return userRoleDataService.save(userRoleData);
    }

    /**
     * 移除用户角色
     * storeId 为 null 时移除系统级角色，不为 null 时移除店面级角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long userId, Long roleId, Long storeId) {
        userRoleDataService.deleteByUserIdAndRoleIdAndStoreId(userId, roleId, storeId);
    }

    /**
     * 校验操作人是否为指定店面的店长
     * 操作人在该店面必须拥有 STORE_ADMIN 角色才被视为店长
     */
    private void validateStoreAdmin(Long storeId, Long operatorId) {
        if (operatorId == null) {
            throw new BusinessException(403, "缺少操作人信息");
        }

        RoleData storeAdminRole = roleDataService.findByRoleCode("STORE_ADMIN")
                .orElseThrow(() -> new BusinessException(500, "STORE_ADMIN角色未初始化"));

        List<UserRoleData> operatorRoles = userRoleDataService.findByStoreIdAndUserId(storeId, operatorId);
        boolean isStoreAdmin = operatorRoles.stream()
                .anyMatch(ur -> ur.getRoleId().equals(storeAdminRole.getId()));

        if (!isStoreAdmin) {
            RoleData superAdminRole = roleDataService.findByRoleCode("SUPER_ADMIN")
                    .orElse(null);
            if (superAdminRole != null) {
                List<UserRoleData> sysRoles = userRoleDataService.findSystemRolesByUserId(operatorId);
                boolean isSuperAdmin = sysRoles.stream()
                        .anyMatch(ur -> ur.getRoleId().equals(superAdminRole.getId()));
                if (isSuperAdmin) {
                    return;
                }
            }
            throw new BusinessException(403, "只有该店面店长或超级管理员才能分配店面角色");
        }
    }

    private void assignDefaultRole(Long userId) {
        RoleData customerRole = roleDataService.findByRoleCode("CUSTOMER")
                .orElseThrow(() -> new BusinessException(500, "默认角色CUSTOMER未初始化"));

        UserRoleData userRoleData = new UserRoleData();
        userRoleData.setUserId(userId);
        userRoleData.setRoleId(customerRole.getId());
        userRoleData.setStoreId(null);
        userRoleData.setCreateUser(null);
        userRoleDataService.save(userRoleData);
    }

    private void validateRegistration(UserData userData) {
        if (StringUtils.hasLength(userData.getPhone())) {
            userDataRepository.findByPhone(userData.getPhone()).ifPresent(u -> {
                throw new BusinessException(400, "该手机号已注册");
            });
        }

        if (StringUtils.hasLength(userData.getEmail())) {
            userDataRepository.findByEmail(userData.getEmail()).ifPresent(u -> {
                throw new BusinessException(400, "该邮箱已注册");
            });
        }

        if (StringUtils.hasLength(userData.getWxOpenid())) {
            userDataRepository.findByWxOpenid(userData.getWxOpenid()).ifPresent(u -> {
                throw new BusinessException(400, "该微信账号已注册");
            });
        }
    }
}
