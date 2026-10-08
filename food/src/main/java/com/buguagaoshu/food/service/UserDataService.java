package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.*;
import com.buguagaoshu.food.enums.RoleDomainEnum;
import com.buguagaoshu.food.enums.RbacRoleCode;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.NotificationRepository;
import com.buguagaoshu.food.repository.UserDataRepository;
import com.buguagaoshu.food.utils.PasswordUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

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

    /** 中国大陆手机号校验规则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /** 邮箱校验规则 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserDataRepository userDataRepository;
    private final UserRoleDataService userRoleDataService;
    private final RoleDataService roleDataService;
    private final StoreService storeService;
    private final NotificationRepository notificationRepository;
    private final ClientAuditService clientAuditService;

    public UserDataService(UserDataRepository userDataRepository,
                           UserRoleDataService userRoleDataService,
                           RoleDataService roleDataService,
                           StoreService storeService,
                           NotificationRepository notificationRepository,
                           ClientAuditService clientAuditService) {
        this.userDataRepository = userDataRepository;
        this.userRoleDataService = userRoleDataService;
        this.roleDataService = roleDataService;
        this.storeService = storeService;
        this.notificationRepository = notificationRepository;
        this.clientAuditService = clientAuditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserData register(UserData userData) {
        // 未填写的唯一字段统一归一化为 null：
        // MySQL 唯一索引中空字符串 '' 会参与唯一性比较（多个 '' 冲突，多个 NULL 可共存），
        // 开发环境 H2 会把 '' 当作 NULL，故该问题只在 MySQL 部署时暴露。
        normalizeUniqueFields(userData);
        validateRegistration(userData);

        if (StringUtils.hasLength(userData.getPassword())) {
            userData.setPassword(PasswordUtil.encode(userData.getPassword()));
        }

        long time = System.currentTimeMillis();
        userData.setCreateTime(time);
        userData.setUpdateTime(time);
        userData.setStatus(0);
        userData.setGender(userData.getGender() != null ? userData.getGender() : 0);
        // 注册来源留痕（IP/UA/位置）：一律由服务端按当前请求覆盖，不信任请求体中的同名字段
        clientAuditService.fill(userData);

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
        // 手机/邮箱为可空唯一字段，未填写（空白串）时落 NULL，避免 MySQL 唯一索引冲突
        existing.setPhone(blankToNull(updateData.getPhone()));
        existing.setEmail(blankToNull(updateData.getEmail()));
        existing.setGender(updateData.getGender());
        existing.setAddress(updateData.getAddress());
        existing.setUpdateTime(System.currentTimeMillis());

        if (StringUtils.hasLength(updateData.getPassword())) {
            existing.setPassword(PasswordUtil.encode(updateData.getPassword()));
        }

        return userDataRepository.save(existing);
    }

    /**
     * 将历史 MD5 密文透明升级为 BCrypt（登录校验通过后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void upgradePasswordToBCrypt(Long userId, String rawPassword) {
        findById(userId).ifPresent(u -> {
            u.setPassword(PasswordUtil.encode(rawPassword));
            u.setUpdateTime(System.currentTimeMillis());
            userDataRepository.save(u);
        });
    }

    /**
     * 修改（重置）当前登录用户自己的密码
     * <p>
     * 用户身份来自登录会话（userId），不信任前端传参；需校验原密码。
     *
     * @param userId   当前登录用户ID
     * @param oldRaw   原密码明文
     * @param newRaw   新密码明文
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldRaw, String newRaw) {
        UserData user = getExistingUser(userId);
        if (!PasswordUtil.matches(oldRaw, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (PasswordUtil.matches(newRaw, user.getPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        user.setPassword(PasswordUtil.encode(newRaw));
        user.setUpdateTime(System.currentTimeMillis());
        userDataRepository.save(user);
    }

    /**
     * 更新当前登录用户的头像与昵称
     * <p>
     * 仅更新非 null 字段，避免误清空其它信息。
     *
     * @param userId    当前登录用户ID
     * @param nickname  昵称（为 null 表示不修改）
     * @param avatarUrl 头像URL（为 null 表示不修改）
     * @return 已脱敏的最新用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserData updateProfile(Long userId, String nickname, String avatarUrl) {
        UserData user = getExistingUser(userId);
        if (nickname != null) {
            String trimmed = nickname.trim();
            if (trimmed.isEmpty()) {
                throw new BusinessException(400, "昵称不能为空");
            }
            user.setNickname(trimmed);
        }
        if (avatarUrl != null) {
            String trimmed = avatarUrl.trim();
            user.setAvatarUrl(trimmed.isEmpty() ? null : trimmed);
        }
        user.setUpdateTime(System.currentTimeMillis());
        UserData saved = userDataRepository.save(user);
        saved.setPassword(null);
        return saved;
    }

    /**
     * 校验「新手机号」是否可用于绑定/更换
     * <p>
     * 规则：格式正确 → 不与当前手机号相同 → 未被其它账号占用。
     * 在发送新手机验证码、以及最终提交修改时各调用一次（后者为并发兜底）。
     *
     * @param userId 当前登录用户ID
     * @param phone  待校验的新手机号
     * @return 校验通过的手机号
     */
    public String validateNewPhoneAvailable(Long userId, String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new BusinessException(400, "请输入正确的 11 位手机号");
        }
        String normalized = phone.trim();
        UserData current = getExistingUser(userId);
        if (normalized.equals(current.getPhone())) {
            throw new BusinessException(400, "新手机号不能与当前手机号相同");
        }
        userDataRepository.findByPhone(normalized).ifPresent(u -> {
            throw new BusinessException(400, "该手机号已被其他账号绑定");
        });
        return normalized;
    }

    /**
     * 校验注册目标（手机号或邮箱）格式合法且尚未被注册
     * <p>
     * 在「发送注册验证码」时调用：防止向已注册的手机号/邮箱下发验证码。
     *
     * @param target 手机号或邮箱
     * @return 归一化（去空白）后的目标
     */
    public String validateRegisterTarget(String target) {
        if (target == null || target.isBlank()) {
            throw new BusinessException(400, "请输入手机号或邮箱");
        }
        String normalized = target.trim();
        if (normalized.contains("@")) {
            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                throw new BusinessException(400, "请输入正确的邮箱地址");
            }
            userDataRepository.findByEmail(normalized).ifPresent(u -> {
                throw new BusinessException(400, "该邮箱已注册");
            });
        } else {
            if (!PHONE_PATTERN.matcher(normalized).matches()) {
                throw new BusinessException(400, "请输入正确的 11 位手机号");
            }
            userDataRepository.findByPhone(normalized).ifPresent(u -> {
                throw new BusinessException(400, "该手机号已注册");
            });
        }
        return normalized;
    }

    /**
     * 校验「忘记密码」目标（手机号或邮箱）格式合法且已注册
     * <p>
     * 与 {@link #validateRegisterTarget(String)} 相反：重置密码要求目标已绑定某个账号，
     * 否则无人可收到验证码。在发送重置验证码与最终提交重置时各调用一次。
     *
     * @param target 手机号或邮箱
     * @return 归一化（去空白）后的目标
     */
    public String validateResetTarget(String target) {
        if (target == null || target.isBlank()) {
            throw new BusinessException(400, "请输入手机号或邮箱");
        }
        String normalized = target.trim();
        if (normalized.contains("@")) {
            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                throw new BusinessException(400, "请输入正确的邮箱地址");
            }
            userDataRepository.findByEmail(normalized)
                    .orElseThrow(() -> new BusinessException(404, "该邮箱未注册"));
        } else {
            if (!PHONE_PATTERN.matcher(normalized).matches()) {
                throw new BusinessException(400, "请输入正确的 11 位手机号");
            }
            userDataRepository.findByPhone(normalized)
                    .orElseThrow(() -> new BusinessException(404, "该手机号未注册"));
        }
        return normalized;
    }

    /**
     * 忘记密码：凭已校验的手机号/邮箱验证码重置密码（无需原密码）
     * <p>
     * 仅更新密码字段，不动其它资料；重置成功后注销该账号的登录会话，
     * 使「密码已泄露」的用户不会因旧 token 仍有效而继续处于风险中。
     *
     * @param target 手机号或邮箱（验证码已由其所属账号收到并校验通过）
     * @param newRaw 新密码明文
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByTarget(String target, String newRaw) {
        if (newRaw == null || newRaw.length() < 6 || newRaw.length() > 32) {
            throw new BusinessException(400, "新密码长度需在 6-32 位之间");
        }
        String normalized = validateResetTarget(target);
        UserData user = (normalized.contains("@")
                ? userDataRepository.findByEmail(normalized)
                : userDataRepository.findByPhone(normalized))
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        user.setPassword(PasswordUtil.encode(newRaw));
        user.setUpdateTime(System.currentTimeMillis());
        userDataRepository.save(user);
        StpUtil.logout(user.getId());
    }

    /**
     * 普通用户自助注册
     * <p>
     * 验证码已由 Controller 层通过 VerificationCodeService 校验通过，此处按目标类型
     * 回填 phone/email 后复用 {@link #register(UserData)} 完成建号与默认角色分配。
     *
     * @param target 已校验的手机号或邮箱
     * @param password 登录密码明文
     * @param nickname 昵称（可空）
     * @return 已脱敏的新用户
     */
    @Transactional(rollbackFor = Exception.class)
    public UserData registerByTarget(String target, String password, String nickname) {
        String normalized = validateRegisterTarget(target);
        UserData userData = new UserData();
        if (normalized.contains("@")) {
            userData.setEmail(normalized);
        } else {
            userData.setPhone(normalized);
        }
        userData.setPassword(password);
        if (StringUtils.hasText(nickname)) {
            String name = nickname.trim();
            if (!name.isEmpty()) {
                userData.setNickname(name);
            }
        }
        UserData saved = register(userData);
        saved.setPassword(null);
        return saved;
    }

    /**
     * 更换当前登录用户的手机号
     * <p>
     * 新旧手机号验证码已在 Controller 层通过 VerificationCodeService 校验通过，
     * 此处再次确认新号码未被占用后落库（数据库唯一约束为最终兜底）。
     *
     * @param userId   当前登录用户ID
     * @param newPhone 已校验可用的新手机号
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePhone(Long userId, String newPhone) {
        UserData user = getExistingUser(userId);
        userDataRepository.findByPhone(newPhone).ifPresent(u -> {
            if (!u.getId().equals(userId)) {
                throw new BusinessException(400, "该手机号已被其他账号绑定");
            }
        });
        user.setPhone(newPhone);
        user.setUpdateTime(System.currentTimeMillis());
        userDataRepository.save(user);
    }

    /**
     * 管理员重置指定用户密码（无需原密码）
     * <p>
     * 仅更新密码字段，不触碰昵称、头像、手机号、邮箱等其它资料，
     * 从根本上避免「重置密码却把用户资料清空」的问题。
     *
     * @param userId 被重置密码的目标用户ID
     * @param newRaw 新密码明文
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminResetPassword(Long userId, String newRaw) {
        if (newRaw == null || newRaw.length() < 6 || newRaw.length() > 32) {
            throw new BusinessException(400, "新密码长度需在 6-32 位之间");
        }
        UserData user = getExistingUser(userId);
        user.setPassword(PasswordUtil.encode(newRaw));
        user.setUpdateTime(System.currentTimeMillis());
        userDataRepository.save(user);
    }

    /** 按ID加载用户，不存在抛 404 */
    private UserData getExistingUser(Long userId) {
        return findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
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
                List<PermissionData> permCodes = RolePermissionCache.getPermissionDetailsByRoleId(ur.getRoleId());
                role.setPermissionList(permCodes);
                roleDataList.add(role);
            }
        }
        user.setRoleDataList(roleDataList);
    }

    /**
     * 修改用户状态（启用/禁用）
     *
     * 校验规则：
     * - 超级管理员不能禁用自己，需先移除超管角色后再禁用
     * - 系统中仅剩一个超级管理员时，不能禁用该用户
     */
    @Transactional(rollbackFor = Exception.class)
    public UserData updateStatus(Long userId, Integer status) {
        UserData user = findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        
        // 禁用时校验：超级管理员不能被直接禁用
        if (status == WebConstant.ENABLE && isSuperAdmin(userId)) {
            throw new BusinessException(400, "该用户拥有超级管理员角色，无法直接禁用，请先移除超管角色");
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
     *
     * 校验规则：
     * 1. 系统中仅剩一个超级管理员时，不允许移除自己的超级管理员角色
     * 2. 移除后若用户无任何角色，自动分配 CUSTOMER 角色保底
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long userId, Long roleId, Long storeId) {
        RoleData role = roleDataService.findById(roleId)
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));

        // 规则1：最后一个超级管理员不能移除自己的角色
        if (RbacRoleCode.SUPER_ADMIN.getCode().equals(role.getRoleCode()) && storeId == null) {
            List<UserRoleData> superAdminBindings = userRoleDataService.findByRoleId(roleId).stream()
                    .filter(ur -> ur.getStoreId() == null)
                    .toList();
            if (superAdminBindings.size() <= 1) {
                throw new BusinessException(400, "系统仅剩一个超级管理员，不能移除该角色");
            }
        }

        // 执行移除
        userRoleDataService.deleteByUserIdAndRoleIdAndStoreId(userId, roleId, storeId);

        // 规则2：移除后若用户无任何角色，自动分配 CUSTOMER 角色保底
        List<UserRoleData> remainingRoles = userRoleDataService.findByUserId(userId);
        if (remainingRoles.isEmpty()) {
            assignDefaultRole(userId);
        }
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
        RoleData superAdminRole = roleDataService.findByRoleCode(RbacRoleCode.SUPER_ADMIN.getCode()).orElse(null);
        if (superAdminRole == null) {
            return false;
        }
        List<UserRoleData> sysRoles = userRoleDataService.findSystemRolesByUserId(userId);
        return sysRoles.stream().anyMatch(ur -> ur.getRoleId().equals(superAdminRole.getId()));
    }

    public boolean isStoreOwner(Long storeId, Long userId) {
        RoleData storeOwnerRole = roleDataService.findByRoleCode(RbacRoleCode.STORE_OWNER.getCode()).orElse(null);
        if (storeOwnerRole == null) {
            return false;
        }
        List<UserRoleData> storeRoles = userRoleDataService.findByStoreIdAndUserId(storeId, userId);
        return storeRoles.stream().anyMatch(ur -> ur.getRoleId().equals(storeOwnerRole.getId()));
    }

    /**
     * 查询门店员工列表（按员工聚合，含已生效角色与待确认邀请）
     * <p>
     * 用于店主端员工管理页面。每个员工一行，包含：
     * <ul>
     *   <li>{@code roles}：已生效的角色（员工已确认，可正常使用门店功能）</li>
     *   <li>{@code pendingRoles}：待确认的角色（邀请已发出但员工尚未同意，不具备任何权限）</li>
     *   <li>{@code pending}：是否存在待确认邀请，前端据此展示「待确认」标记与撤销按钮</li>
     * </ul>
     * 支持一个员工拥有多个角色（如同时是服务员与收银员）。
     */
    public List<Map<String, Object>> listStoreEmployees(Long storeId) {
        List<UserRoleData> storeRoles = userRoleDataService.findByStoreId(storeId);
        List<NotificationData> pendingInvites = notificationRepository.findByStoreIdAndTypeAndStatus(
                storeId, NotificationData.TYPE_STAFF_INVITE, NotificationData.STATUS_PENDING);
        if (storeRoles.isEmpty() && pendingInvites.isEmpty()) {
            return new ArrayList<>();
        }

        // 已生效角色按员工分组
        Map<Long, List<UserRoleData>> rolesByUser = new LinkedHashMap<>();
        for (UserRoleData ur : storeRoles) {
            rolesByUser.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(ur);
        }
        // 待确认邀请按员工索引（同店同人最多一条待处理邀请）
        Map<Long, NotificationData> inviteByUser = new LinkedHashMap<>();
        for (NotificationData n : pendingInvites) {
            inviteByUser.putIfAbsent(n.getUserId(), n);
        }

        // 批量查询用户信息，避免 N+1
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.addAll(rolesByUser.keySet());
        userIds.addAll(inviteByUser.keySet());
        Map<Long, UserData> userMap = new HashMap<>();
        for (UserData u : userDataRepository.findAllById(userIds)) {
            u.setPassword(null);
            userMap.put(u.getId(), u);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long userId : userIds) {
            UserData user = userMap.get(userId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", userId);
            item.put("storeId", storeId);
            if (user != null) {
                item.put("nickname", user.getNickname());
                item.put("phone", user.getPhone());
                item.put("avatarUrl", user.getAvatarUrl());
                item.put("userStatus", user.getStatus());
            }

            // 已生效角色
            List<Map<String, Object>> roles = new ArrayList<>();
            for (UserRoleData ur : rolesByUser.getOrDefault(userId, List.of())) {
                Map<String, Object> roleVo = new LinkedHashMap<>();
                roleVo.put("userRoleId", ur.getId());
                roleVo.put("roleId", ur.getRoleId());
                RoleData role = RolePermissionCache.ROLE_MAP.get(ur.getRoleId());
                if (role != null) {
                    roleVo.put("roleName", role.getRoleName());
                    roleVo.put("roleCode", role.getRoleCode());
                }
                roles.add(roleVo);
            }
            item.put("roles", roles);

            // 待确认邀请
            NotificationData invite = inviteByUser.get(userId);
            List<Map<String, Object>> pendingRoles = new ArrayList<>();
            if (invite != null) {
                for (String raw : (invite.getRoleIds() == null ? "" : invite.getRoleIds()).split(",")) {
                    if (raw.isBlank()) {
                        continue;
                    }
                    Long roleId = Long.valueOf(raw.trim());
                    RoleData role = RolePermissionCache.ROLE_MAP.get(roleId);
                    if (role == null) {
                        continue;
                    }
                    Map<String, Object> roleVo = new LinkedHashMap<>();
                    roleVo.put("roleId", roleId);
                    roleVo.put("roleName", role.getRoleName());
                    roleVo.put("roleCode", role.getRoleCode());
                    pendingRoles.add(roleVo);
                }
                item.put("notificationId", invite.getId());
                item.put("inviteTime", invite.getCreateTime());
                item.put("inviterName", invite.getInviterName());
            }
            item.put("pendingRoles", pendingRoles);
            item.put("pending", invite != null);
            result.add(item);
        }
        // 待确认的排前面，便于店主关注未完成的邀请
        result.sort(Comparator.comparing(m -> !(Boolean) m.get("pending")));
        return result;
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

    /**
     * 创建匿名（扫码点餐）顾客用户
     * <p>
     * 用于餐桌二维码免登录下单场景：不要求顾客注册/登录，
     * 由系统创建一条无登录凭证的用户记录，仅用于订单归属与展示。
     * phone/email/wxOpenid 均为空，不会被 @NotBlank 校验（直接落库，不走注册校验）。
     *
     * @param nickname 展示昵称（如"扫码顾客-A1"）
     * @return 已创建并分配 CUSTOMER 角色的用户
     */
    @Transactional(rollbackFor = Exception.class)
    public UserData createGuestUser(String nickname) {
        UserData guest = new UserData();
        guest.setNickname(nickname);
        guest.setGender(0);
        guest.setStatus(0);
        long now = System.currentTimeMillis();
        guest.setCreateTime(now);
        guest.setUpdateTime(now);
        // 扫码下单来源留痕，与注册账号同口径便于审计
        clientAuditService.fill(guest);
        UserData saved = userDataRepository.save(guest);
        assignDefaultRole(saved.getId());
        return saved;
    }

    /**
     * 为用户绑定手机号（用于免登录顾客结算时升级为可识别会员）
     * <p>
     * 规则：
     * - 若用户已绑定相同手机号，直接返回 true（幂等）
     * - 若手机号已被其他用户占用，返回 false（不绑定，由调用方决定仅累积积分）
     * - 否则回填手机号并保存
     *
     * @param userId 用户ID
     * @param phone  手机号
     * @return true 绑定成功或已绑定相同号码；false 手机号已被占用
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean bindPhone(Long userId, String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        UserData user = findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (phone.equals(user.getPhone())) {
            return true;
        }
        if (userDataRepository.findByPhone(phone).isPresent()) {
            return false;
        }
        user.setPhone(phone);
        user.setUpdateTime(System.currentTimeMillis());
        userDataRepository.save(user);
        return true;
    }

    private void assignDefaultRole(Long userId) {
        RoleData customerRole = roleDataService.findByRoleCode(RbacRoleCode.CUSTOMER.getCode())
                .orElseThrow(() -> new BusinessException(500, "默认角色CUSTOMER未初始化"));

        UserRoleData userRoleData = new UserRoleData();
        userRoleData.setUserId(userId);
        userRoleData.setRoleId(customerRole.getId());
        userRoleData.setStoreId(null);
        userRoleData.setCreateUser(null);
        userRoleDataService.save(userRoleData);
    }

    /**
     * 将用户记录中「可空唯一字段」的空白值归一化为 null。
     * <p>
     * phone/email 及三方 openid 均允许不填，但在 MySQL 中唯一索引把空字符串 ''
     * 视为一个普通值（只允许出现一次），第二条未填写记录就会报
     * 1062 Duplicate entry ''；NULL 在唯一索引中可重复出现，因此统一落 NULL。
     */
    private void normalizeUniqueFields(UserData userData) {
        userData.setPhone(blankToNull(userData.getPhone()));
        userData.setEmail(blankToNull(userData.getEmail()));
        userData.setWxOpenid(blankToNull(userData.getWxOpenid()));
        userData.setZfbOpenid(blankToNull(userData.getZfbOpenid()));
        userData.setDyOpenid(blankToNull(userData.getDyOpenid()));
    }

    /** 空白字符串（含纯空白）转为 null，其余值原样返回 */
    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
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
