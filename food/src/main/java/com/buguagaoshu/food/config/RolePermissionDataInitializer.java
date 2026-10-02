package com.buguagaoshu.food.config;

import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.RolePermissionData;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.enums.RbacPermissionCode;
import com.buguagaoshu.food.enums.RbacRoleCode;
import com.buguagaoshu.food.repository.PermissionDataRepository;
import com.buguagaoshu.food.repository.RoleDataRepository;
import com.buguagaoshu.food.repository.RolePermissionDataRepository;
import com.buguagaoshu.food.repository.UserDataRepository;
import com.buguagaoshu.food.service.RoleDataService;
import com.buguagaoshu.food.service.RolePermissionDataService;
import com.buguagaoshu.food.service.UserRoleDataService;
import com.buguagaoshu.food.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色与权限数据初始化（RBAC-D 双域隔离）
 * <p>
 * 系统启动时：
 * 1. 初始化全局域角色和权限（平台管理相关）
 * 2. 初始化门店域角色和权限（门店运营相关）
 * 3. 建立角色与权限的默认关联关系（域内绑定，禁止跨域）
 * 4. 将角色和权限数据加载到内存缓存
 * <p>
 * 权限架构：
 * ┌─────────────────────────────────────────────────────┐
 * │                    全局域 (GLOBAL)                    │
 * │  超级管理员 → 平台运营 → 平台审核                      │
 * │  管理用户、审核门店、系统配置、平台统计                   │
 * └─────────────────────────────────────────────────────┘
 * ┌─────────────────────────────────────────────────────┐
 * │                    门店域 (STORE)                     │
 * │  店铺老板 → 收银员 / 后厨 / 服务员 / 库存管理员         │
 * │  管理菜品、订单、餐桌、配送、评价、库存、门店统计          │
 * └─────────────────────────────────────────────────────┘
 * ┌─────────────────────────────────────────────────────┐
 * │                  普通用户 (CUSTOMER)                   │
 * │  浏览店面、菜品，下单、评价                              │
 * └─────────────────────────────────────────────────────┘
 * <p>
 * 角色与权限的定义由 {@link RbacDefinition} 集中维护，
 * 角色编码见 {@link RbacRoleCode}，权限编码见 {@link RbacPermissionCode}
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Slf4j
@Component
@Order(1)
public class RolePermissionDataInitializer implements CommandLineRunner {

    /**
     * 首次启动初始化超级管理员的默认密码（仅用于本地/开发首启，登录后应立即修改）
     */
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private final RoleDataService roleDataService;
    private final RolePermissionDataService rolePermissionDataService;
    private final RoleDataRepository roleDataRepository;
    private final PermissionDataRepository permissionDataRepository;
    private final RolePermissionDataRepository rolePermissionDataRepository;
    private final UserDataRepository userDataRepository;
    private final UserRoleDataService userRoleDataService;

    public RolePermissionDataInitializer(RoleDataService roleDataService,
                                         RolePermissionDataService rolePermissionDataService,
                                         RoleDataRepository roleDataRepository,
                                         PermissionDataRepository permissionDataRepository,
                                         RolePermissionDataRepository rolePermissionDataRepository,
                                         UserDataRepository userDataRepository,
                                         UserRoleDataService userRoleDataService) {
        this.roleDataService = roleDataService;
        this.rolePermissionDataService = rolePermissionDataService;
        this.roleDataRepository = roleDataRepository;
        this.permissionDataRepository = permissionDataRepository;
        this.rolePermissionDataRepository = rolePermissionDataRepository;
        this.userDataRepository = userDataRepository;
        this.userRoleDataService = userRoleDataService;
    }

    @Override
    public void run(String... args) {
        log.info("开始初始化角色与权限数据（RBAC-D 双域隔离）...");
        initRoles();
        initPermissions();
        initRolePermissionRelations();
        loadCache();
        initSuperAdmin();
        log.info("角色与权限数据初始化完成 - 全局角色: {}, 门店角色: {}, 全局权限: {}, 门店权限: {}",
                RolePermissionCache.getGlobalRoles().size(),
                RolePermissionCache.getStoreRoles().size(),
                RolePermissionCache.getGlobalPermissions().size(),
                RolePermissionCache.getStorePermissions().size());
    }

    private void initRoles() {
        Set<String> existingRoleCodes = new HashSet<>();
        roleDataRepository.findAll().forEach(r -> existingRoleCodes.add(r.getRoleCode()));

        List<RoleData> roleDataList = new ArrayList<>();
        for (RbacRoleCode roleDef : RbacDefinition.getDefaultRoles()) {
            if (!existingRoleCodes.contains(roleDef.getCode())) {
                RoleData role = new RoleData();
                role.setRoleCode(roleDef.getCode());
                role.setRoleName(roleDef.getName());
                role.setDomain(roleDef.getDomain().getCode());
                role.setDescription(roleDef.getDescription());
                role.setSort(roleDef.getSort());
                role.setStatus(0);
                role.setStoreId(null);
                roleDataList.add(role);
                log.info("初始化角色: [{}] {} - {}", roleDef.getDomain().getCode(), roleDef.getCode(), roleDef.getName());
            }
        }
        if (!roleDataList.isEmpty()) {
            roleDataService.saveAll(roleDataList);
        }
    }

    private void initPermissions() {
        Set<String> existingPermKeys = new HashSet<>();
        permissionDataRepository.findAll().forEach(p -> existingPermKeys.add(p.getPermissionCode() + "_" + p.getDomain()));

        List<PermissionData> permissionDataList = new ArrayList<>();
        for (RbacPermissionCode permDef : RbacDefinition.getDefaultPermissions()) {
            String permKey = permDef.getCode() + "_" + permDef.getDomain().getCode();
            if (!existingPermKeys.contains(permKey)) {
                PermissionData perm = new PermissionData();
                perm.setPermissionCode(permDef.getCode());
                perm.setPermissionName(permDef.getName());
                perm.setModule(permDef.getModule());
                perm.setDomain(permDef.getDomain().getCode());
                perm.setDescription(permDef.getDescription());
                perm.setSort(permDef.getSort());
                perm.setStatus(0);
                permissionDataList.add(perm);
                log.info("初始化权限: [{}] {} - {}", permDef.getDomain().getCode(), permDef.getCode(), permDef.getName());
            }
        }
        if (!permissionDataList.isEmpty()) {
            permissionDataRepository.saveAll(permissionDataList);
        }
    }

    private void initRolePermissionRelations() {
        List<RoleData> allRoles = new ArrayList<>();
        roleDataRepository.findAll().forEach(allRoles::add);
        Map<String, Long> roleCodeToIdMap = allRoles.stream()
                .collect(Collectors.toMap(RoleData::getRoleCode, RoleData::getId));

        List<PermissionData> allPermissions = new ArrayList<>();
        permissionDataRepository.findAll().forEach(allPermissions::add);
        Map<String, Long> permCodeToIdMap = allPermissions.stream()
                .collect(Collectors.toMap(
                        p -> p.getPermissionCode() + "_" + p.getDomain(),
                        PermissionData::getId
                ));

        Set<String> existingRelations = new HashSet<>();
        rolePermissionDataRepository.findAll().forEach(rp ->
                existingRelations.add(rp.getRoleId() + "_" + rp.getPermissionId()));

        List<RolePermissionData> rolePermissionDataList = new ArrayList<>();
        for (var entry : RbacDefinition.getDefaultRolePermissions().entrySet()) {
            RbacRoleCode roleCode = entry.getKey();
            List<RbacPermissionCode> permissionCodes = entry.getValue();

            Long roleId = roleCodeToIdMap.get(roleCode.getCode());
            if (roleId == null) {
                log.warn("角色 {} 未找到，跳过权限关联", roleCode.getCode());
                continue;
            }

            RoleData role = allRoles.stream()
                    .filter(r -> r.getRoleCode().equals(roleCode.getCode()))
                    .findFirst().orElse(null);
            if (role == null) {
                continue;
            }

            for (RbacPermissionCode permCode : permissionCodes) {
                String permKey = permCode.getCode() + "_" + roleCode.getDomain().getCode();
                Long permId = permCodeToIdMap.get(permKey);
                if (permId == null) {
                    log.warn("权限 {} 在 {} 域未找到，跳过关联", permCode.getCode(), roleCode.getDomain().getCode());
                    continue;
                }

                String relationKey = roleId + "_" + permId;
                if (!existingRelations.contains(relationKey)) {
                    RolePermissionData rp = new RolePermissionData();
                    rp.setRoleId(roleId);
                    rp.setPermissionId(permId);
                    rolePermissionDataList.add(rp);
                }
            }
        }

        if (!rolePermissionDataList.isEmpty()) {
            rolePermissionDataService.saveAll(rolePermissionDataList);
        }
    }

    private void loadCache() {
        List<RoleData> allRoles = new ArrayList<>();
        roleDataRepository.findAll().forEach(allRoles::add);
        allRoles.forEach(role -> {
            RolePermissionCache.ROLE_MAP.put(role.getId(), role);
            RolePermissionCache.ROLE_CODE_MAP.put(role.getRoleCode(), role);
        });

        List<PermissionData> allPermissions = new ArrayList<>();
        permissionDataRepository.findAll().forEach(allPermissions::add);
        allPermissions.forEach(perm -> {
            RolePermissionCache.PERMISSION_MAP.put(perm.getId(), perm);
        });

        List<RolePermissionData> allRolePermissions = new ArrayList<>();
        rolePermissionDataRepository.findAll().forEach(allRolePermissions::add);
        Map<Long, List<RolePermissionData>> rolePermissionMap = allRolePermissions.stream()
                .collect(Collectors.groupingBy(RolePermissionData::getRoleId));

        RolePermissionCache.ROLE_MAP.keySet().forEach(roleId -> {
            List<String> permCodes = new ArrayList<>();
            List<RolePermissionData> relations = rolePermissionMap.getOrDefault(roleId, Collections.emptyList());
            for (RolePermissionData rp : relations) {
                PermissionData perm = RolePermissionCache.PERMISSION_MAP.get(rp.getPermissionId());
                if (perm != null && perm.getStatus() == 0) {
                    permCodes.add(perm.getPermissionCode());
                }
            }
            RolePermissionCache.ROLE_PERMISSIONS_MAP.put(roleId, permCodes);
        });

        log.info("角色权限缓存加载完成 - 全局角色: {}, 门店角色: {}, 全局权限: {}, 门店权限: {}",
                RolePermissionCache.getGlobalRoles().size(),
                RolePermissionCache.getStoreRoles().size(),
                RolePermissionCache.getGlobalPermissions().size(),
                RolePermissionCache.getStorePermissions().size());
    }

    /**
     * 初始化超级管理员账号
     * 当系统中不存在任何用户时，自动创建超级管理员账号
     * 默认账号: admin，密码: 123456
     * 仅在开发环境或首次启动时生效，已有用户则跳过
     */
    private void initSuperAdmin() {
        long userCount = userDataRepository.count();
        if (userCount > 0) {
            log.info("系统已有 {} 个用户，跳过超级管理员初始化", userCount);
            return;
        }

        log.info("系统中无用户，开始初始化超级管理员账号...");

        UserData admin = new UserData();
        admin.setNickname("admin");
        admin.setPhone("13800000000");
        admin.setPassword(PasswordUtil.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setGender(0);
        admin.setStatus(0);
        long time = System.currentTimeMillis();
        admin.setCreateTime(time);
        admin.setUpdateTime(time);

        UserData savedAdmin = userDataRepository.save(admin);
        log.warn("超级管理员账号创建成功 - ID: {}, 昵称: admin, 手机号: 13800000000，" +
                "已使用初始密码，请登录后立即修改！", savedAdmin.getId());

        RoleData superAdminRole = roleDataService.findByRoleCode(RbacRoleCode.SUPER_ADMIN.getCode())
                .orElseThrow(() -> new RuntimeException("超级管理员角色未初始化"));

        UserRoleData userRoleData = new UserRoleData();
        userRoleData.setUserId(savedAdmin.getId());
        userRoleData.setRoleId(superAdminRole.getId());
        userRoleData.setStoreId(null);
        userRoleData.setCreateUser(null);
        userRoleDataService.save(userRoleData);

        log.info("超级管理员角色分配成功 - 用户ID: {}, 角色ID: {}", savedAdmin.getId(), superAdminRole.getId());
    }
}
