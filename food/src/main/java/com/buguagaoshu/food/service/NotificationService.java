package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.NotificationData;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 站内消息通知服务
 * <p>
 * 目前承载「员工邀请」流程：店主按手机号邀请 → 生成待处理通知 → 员工确认后才真正写入角色绑定。
 * 未确认的邀请不会赋予任何权限，仅在店主员工列表中显示「待确认」。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-05
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserDataService userDataService;
    private final RoleDataService roleDataService;
    private final StoreService storeService;
    private final UserRoleDataService userRoleDataService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserDataService userDataService,
                               RoleDataService roleDataService,
                               StoreService storeService,
                               UserRoleDataService userRoleDataService) {
        this.notificationRepository = notificationRepository;
        this.userDataService = userDataService;
        this.roleDataService = roleDataService;
        this.storeService = storeService;
        this.userRoleDataService = userRoleDataService;
    }

    /**
     * 店主按手机号邀请员工加入门店（可一次邀请多个角色）
     * <p>
     * 邀请不会立即赋权，而是生成一条待处理通知，员工确认后才写入角色绑定。
     * 校验：手机号必须已注册；角色必须是门店域且未拥有/未在待确认中。
     *
     * @param storeId    门店ID
     * @param phone      被邀请人手机号
     * @param roleIds    要分配的角色ID列表
     * @param operatorId 操作人（店主）ID
     */
    @Transactional(rollbackFor = Exception.class)
    public NotificationData inviteStaff(Long storeId, String phone, List<Long> roleIds, Long operatorId) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(400, "请至少选择一个角色");
        }
        userDataService.validateStoreOwnerOrSuperAdmin(storeId, operatorId);
        Store store = storeService.getStore(storeId);

        UserData target = userDataService.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(404, "该手机号尚未注册，请让员工先用手机号注册账号"));
        if (target.getStatus() != null && target.getStatus() == 1) {
            throw new BusinessException(400, "该用户账号已被禁用，无法邀请");
        }
        if (target.getId().equals(operatorId)) {
            throw new BusinessException(400, "不能邀请自己");
        }

        // 去重并保持顺序
        Set<Long> distinctRoleIds = new LinkedHashSet<>(roleIds);
        List<RoleData> roles = new ArrayList<>();
        for (Long roleId : distinctRoleIds) {
            RoleData role = roleDataService.findById(roleId)
                    .orElseThrow(() -> new BusinessException(404, "角色不存在: " + roleId));
            if (!role.isStore()) {
                throw new BusinessException(400, "只能分配门店域角色，「" + role.getRoleName() + "」属于" + role.getDomain() + "域");
            }
            if (userRoleDataService.existsByUserIdAndRoleIdAndStoreId(target.getId(), roleId, storeId)) {
                throw new BusinessException(400, "该员工在本店已拥有「" + role.getRoleName() + "」角色");
            }
            roles.add(role);
        }

        // 同一门店已有待确认邀请时，直接覆盖为最新的角色组合，避免堆积多条待处理记录
        List<NotificationData> existing = notificationRepository.findByUserIdAndStoreIdAndTypeAndStatus(
                target.getId(), storeId, NotificationData.TYPE_STAFF_INVITE, NotificationData.STATUS_PENDING);
        NotificationData notification = existing.isEmpty() ? new NotificationData() : existing.get(0);

        String roleNames = String.join("、", roles.stream().map(RoleData::getRoleName).toList());
        notification.setUserId(target.getId());
        notification.setType(NotificationData.TYPE_STAFF_INVITE);
        notification.setTitle("门店员工邀请");
        notification.setContent("「" + store.getStoreName() + "」邀请你担任：" + roleNames);
        notification.setStatus(NotificationData.STATUS_PENDING);
        notification.setStoreId(storeId);
        notification.setRoleIds(joinIds(roles.stream().map(RoleData::getId).toList()));
        notification.setInviterId(operatorId);
        notification.setInviterName(userDataService.findById(operatorId)
                .map(UserData::getNickname).orElse(null));
        if (notification.getCreateTime() == null) {
            notification.setCreateTime(System.currentTimeMillis());
        }
        NotificationData saved = notificationRepository.save(notification);
        logger.info("员工邀请: storeId={}, phone={}, roles={}, operator={}",
                storeId, phone, roleNames, operatorId);
        return saved;
    }

    /**
     * 店主撤销待确认的邀请
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelStaffInvite(Long notificationId, Long storeId, Long operatorId) {
        userDataService.validateStoreOwnerOrSuperAdmin(storeId, operatorId);
        NotificationData notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(404, "邀请记录不存在"));
        if (!storeId.equals(notification.getStoreId())) {
            throw new BusinessException(403, "该邀请不属于本门店");
        }
        if (!NotificationData.TYPE_STAFF_INVITE.equals(notification.getType())) {
            throw new BusinessException(400, "该通知不是员工邀请");
        }
        if (notification.getStatus() != NotificationData.STATUS_PENDING) {
            throw new BusinessException(400, "该邀请已被处理，无法撤销");
        }
        notificationRepository.delete(notification);
        logger.info("撤销员工邀请: notificationId={}, storeId={}, operator={}", notificationId, storeId, operatorId);
    }

    /**
     * 员工同意邀请：此时才真正写入角色绑定，员工获得门店权限
     */
    @Transactional(rollbackFor = Exception.class)
    public NotificationData acceptStaffInvite(Long notificationId, Long userId) {
        NotificationData notification = loadActionableInvite(notificationId, userId);
        Store store = storeService.getStore(notification.getStoreId());

        List<Long> roleIds = parseIds(notification.getRoleIds());
        if (roleIds.isEmpty()) {
            throw new BusinessException(400, "邀请数据异常：未包含任何角色");
        }
        int assigned = 0;
        for (Long roleId : roleIds) {
            RoleData role = roleDataService.findById(roleId)
                    .orElseThrow(() -> new BusinessException(404, "角色不存在: " + roleId));
            if (!role.isStore()) {
                throw new BusinessException(400, "角色「" + role.getRoleName() + "」不是门店域角色");
            }
            // 幂等：已拥有则跳过（可能店主在邀请后又单独分配过）
            if (userRoleDataService.existsByUserIdAndRoleIdAndStoreId(userId, roleId, store.getId())) {
                continue;
            }
            UserRoleData binding = new UserRoleData();
            binding.setUserId(userId);
            binding.setRoleId(roleId);
            binding.setStoreId(store.getId());
            binding.setCreateUser(notification.getInviterId());
            userRoleDataService.save(binding);
            assigned++;
        }

        notification.setStatus(NotificationData.STATUS_ACCEPTED);
        notification.setHandleTime(System.currentTimeMillis());
        notificationRepository.save(notification);
        logger.info("员工接受邀请: notificationId={}, userId={}, storeId={}, assigned={}",
                notificationId, userId, store.getId(), assigned);
        return notification;
    }

    /**
     * 员工拒绝邀请：不写入任何角色，店主可重新邀请
     */
    @Transactional(rollbackFor = Exception.class)
    public NotificationData rejectStaffInvite(Long notificationId, Long userId) {
        NotificationData notification = loadActionableInvite(notificationId, userId);
        notification.setStatus(NotificationData.STATUS_REJECTED);
        notification.setHandleTime(System.currentTimeMillis());
        notificationRepository.save(notification);
        logger.info("员工拒绝邀请: notificationId={}, userId={}", notificationId, userId);
        return notification;
    }

    /**
     * 分页查询当前用户的通知
     */
    public Page<NotificationData> listMyNotifications(Long userId, Integer page, Integer size) {
        return notificationRepository.findByUserIdOrderByCreateTimeDesc(
                userId, PageRequest.of(page - 1, size));
    }

    /**
     * 未读/待处理通知数量（前端角标）
     */
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationData.STATUS_PENDING);
    }

    /**
     * 标记单条告知型通知为已读
     * <p>
     * 需处理型通知（员工邀请）不能通过「已读」消除，必须同意或拒绝。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long notificationId, Long userId) {
        NotificationData notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(404, "通知不存在"));
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该通知");
        }
        if (NotificationData.TYPE_STAFF_INVITE.equals(notification.getType())) {
            throw new BusinessException(400, "该通知需要你同意或拒绝，不能仅标记已读");
        }
        if (notification.getStatus() == NotificationData.STATUS_PENDING) {
            notification.setStatus(NotificationData.STATUS_ACCEPTED);
            notification.setHandleTime(System.currentTimeMillis());
            notificationRepository.save(notification);
        }
    }

    /**
     * 全部标记已读（不影响待处理的员工邀请）
     */
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId, System.currentTimeMillis(),
                NotificationData.TYPE_STAFF_INVITE);
    }

    /**
     * 删除通知（仅已处理的可删，待处理邀请需先同意/拒绝或由店主撤销）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        NotificationData notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(404, "通知不存在"));
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该通知");
        }
        if (notification.getStatus() == NotificationData.STATUS_PENDING) {
            throw new BusinessException(400, "待处理的通知不能删除，请先处理");
        }
        notificationRepository.delete(notification);
    }

    /**
     * 查询某门店待确认的员工邀请（店主员工列表用）
     */
    public List<NotificationData> listPendingStaffInvites(Long storeId) {
        return notificationRepository.findByStoreIdAndTypeAndStatus(
                storeId, NotificationData.TYPE_STAFF_INVITE, NotificationData.STATUS_PENDING);
    }

    private NotificationData loadActionableInvite(Long notificationId, Long userId) {
        NotificationData notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(404, "通知不存在"));
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该通知");
        }
        if (!NotificationData.TYPE_STAFF_INVITE.equals(notification.getType())) {
            throw new BusinessException(400, "该通知不是员工邀请");
        }
        if (notification.getStatus() != NotificationData.STATUS_PENDING) {
            throw new BusinessException(400, "该邀请已处理，请勿重复操作");
        }
        return notification;
    }

    private static String joinIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }

    private static List<Long> parseIds(String raw) {
        List<Long> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(Long.valueOf(trimmed));
            }
        }
        return result;
    }
}
