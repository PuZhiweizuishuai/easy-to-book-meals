package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.*;
import com.buguagaoshu.food.enums.RoleDomainEnum;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.UserDataRepository;
import com.buguagaoshu.food.utils.MD5Util;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 用户服务（RBAC-D 域感知）
 * <p>
 * 提供用户注册、信息修改、查询、角色分配等功能
 * 角色分配遵循域隔离规则：
 * - 全局角色(STORE域以外的GLOBAL域角色)由超级管理员分配
 * - 门店角色仅由本店店主(STORE_OWNER)分配，且仅限本店范围
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

    public Page<UserData> findUsersForAdmin(Integer page, Integer size, String key) {
        Page<UserData> users;
        if (StringUtils.hasLength(key)) {
            users = userDataRepository.findByNicknameLike(
                    "%" + key + "%",
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")))
            );
        } else {
            users = userDataRepository.findAll(
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")))
            );
        }
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public UserData findUserWithRolesAndPermissions(Long userId) {
        UserData user = findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        fillUserRolesAndPermissions(user);
        return user;
    }

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
     * 分配全局角色（仅超级管理员可操作）
     * 全局角色: domain=GLOBAL, storeId=null
     */
    @Transactional(rollbackFor = Exception.class)
    public UserRoleData assignGlobalRole(Long userId, Long roleId, Long operatorId) {
        findById(userId).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        RoleData role = roleDataService.findById(roleId)
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));

        if (!role.isGlobal()) {
            throw new BusinessException(400, "只能分配全局域角色，该角色属于" + role.getDomain() + "域");
        }

        if (userRoleDataService.existsByUserIdAndRoleIdAndStoreId(userId, roleId, null)) {
            throw new BusinessException(400, "该全局角色已分配给此用户");
        }

        UserRoleData userRoleData = new UserRoleData();
        userRoleData.setUserId(userId);
        userRoleData.setRoleId(roleId);
        userRoleData.setStoreId(null);
        userRoleData.setCreateUser(operatorId);
        return userRoleDataService.save(userRoleData);
    }

    /**
     * 分配门店角色（仅本店店主或超级管理员可操作）
     * 门店角色: domain=STORE, storeId=具体门店ID
     */
    @Transactional(rollbackFor = Exception.class)
    public UserRoleData assignStoreRole(Long storeId, Long userId, Long roleId, Long operatorId) {
        findById(userId).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        RoleData role = roleDataService.findById(roleId)
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));
        storeService.getStore(storeId);

        if (!role.isStore()) {
            throw new BusinessException(400, "只能分配门店域角色，该角色属于" + role.getDomain() + "域");
        }

        validateStoreOwnerOrSuperAdmin(storeId, operatorId);

        if (userRoleDataService.existsByUserIdAndRoleIdAndStoreId(userId, roleId, storeId)) {
            throw new BusinessException(400, "该用户在此门店已拥有此角色");
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
     * storeId 为 null 时移除全局角色，不为 null 时移除门店角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long userId, Long roleId, Long storeId) {
        userRoleDataService.deleteByUserIdAndRoleIdAndStoreId(userId, roleId, storeId);
    }

    /**
     * 校验操作人是否为指定门店的店主或超级管理员
     * 店主: 在该门店拥有 STORE_OWNER 角色
     * 超级管理员: 系统级拥有 SUPER_ADMIN 角色
     */
    public void validateStoreOwnerOrSuperAdmin(Long storeId, Long operatorId) {
        if (operatorId == null) {
            throw new BusinessException(403, "缺少操作人信息");
        }

        if (isSuperAdmin(operatorId)) {
            return;
        }

        if (isStoreOwner(storeId, operatorId)) {
            return;
        }

        throw new BusinessException(403, "只有本店店主或超级管理员才能执行此操作");
    }

    public boolean isSuperAdmin(Long userId) {
        RoleData superAdminRole = roleDataService.findByRoleCode("SUPER_ADMIN").orElse(null);
        if (superAdminRole == null) {
            return false;
        }
        List<UserRoleData> sysRoles = userRoleDataService.findSystemRolesByUserId(userId);
        return sysRoles.stream().anyMatch(ur -> ur.getRoleId().equals(superAdminRole.getId()));
    }

    public boolean isStoreOwner(Long storeId, Long userId) {
        RoleData storeOwnerRole = roleDataService.findByRoleCode("STORE_OWNER").orElse(null);
        if (storeOwnerRole == null) {
            return false;
        }
        List<UserRoleData> storeRoles = userRoleDataService.findByStoreIdAndUserId(storeId, userId);
        return storeRoles.stream().anyMatch(ur -> ur.getRoleId().equals(storeOwnerRole.getId()));
    }

    /**
     * 获取用户在指定门店拥有的权限编码列表
     */
    public List<String> getUserStorePermissionCodes(Long userId, Long storeId) {
        List<UserRoleData> storeRoles = userRoleDataService.findByStoreIdAndUserId(storeId, userId);
        Set<String> permCodes = new LinkedHashSet<>();
        for (UserRoleData ur : storeRoles) {
            List<String> codes = RolePermissionCache.getPermissionCodesByRoleId(ur.getRoleId());
            permCodes.addAll(codes);
        }
        return new ArrayList<>(permCodes);
    }

    /**
     * 获取用户的全局权限编码列表
     */
    public List<String> getUserGlobalPermissionCodes(Long userId) {
        List<UserRoleData> sysRoles = userRoleDataService.findSystemRolesByUserId(userId);
        Set<String> permCodes = new LinkedHashSet<>();
        for (UserRoleData ur : sysRoles) {
            List<String> codes = RolePermissionCache.getPermissionCodesByRoleId(ur.getRoleId());
            permCodes.addAll(codes);
        }
        return new ArrayList<>(permCodes);
    }

    /**
     * 获取用户所有角色编码列表（全局+门店）
     */
    public List<String> getUserAllRoleCodes(Long userId) {
        List<UserRoleData> allRoles = userRoleDataService.findByUserId(userId);
        List<String> roleCodes = new ArrayList<>();
        for (UserRoleData ur : allRoles) {
            RoleData role = RolePermissionCache.ROLE_MAP.get(ur.getRoleId());
            if (role != null && role.getStatus() == 0) {
                roleCodes.add(role.getRoleCode());
            }
        }
        return roleCodes;
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
