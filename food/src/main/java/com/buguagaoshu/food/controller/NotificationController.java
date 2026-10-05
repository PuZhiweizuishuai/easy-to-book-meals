package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 站内消息通知控制器（当前登录用户视角）
 * <p>
 * 所有接口只作用于当前登录用户自己的通知，不接受前端传入 userId。
 * 员工邀请的「同意/拒绝」在此处理：同意后才会真正写入门店角色绑定。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-05
 */
@RestController
@RequestMapping("/api/notification")
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 分页查询我的通知（按时间倒序）
     */
    @SaCheckLogin
    @GetMapping("/list")
    public ResponseDetails list(
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseDetails.ok().put("data", notificationService.listMyNotifications(userId, page, size));
    }

    /**
     * 未读/待处理数量（顶部铃铛角标）
     */
    @SaCheckLogin
    @GetMapping("/unread-count")
    public ResponseDetails unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseDetails.ok().put("data", notificationService.countUnread(userId));
    }

    /**
     * 同意员工邀请：写入门店角色绑定，之后即可正常使用门店管理功能
     */
    @SaCheckLogin
    @PostMapping("/staff-invite/accept/{id}")
    public ResponseDetails acceptStaffInvite(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.acceptStaffInvite(id, userId);
        return ResponseDetails.ok().put("message", "已接受邀请，现在可以使用门店管理功能了");
    }

    /**
     * 拒绝员工邀请：不写入任何角色，店主可重新邀请
     */
    @SaCheckLogin
    @PostMapping("/staff-invite/reject/{id}")
    public ResponseDetails rejectStaffInvite(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.rejectStaffInvite(id, userId);
        return ResponseDetails.ok().put("message", "已拒绝邀请");
    }

    /**
     * 标记单条告知型通知为已读
     */
    @SaCheckLogin
    @PostMapping("/read/{id}")
    public ResponseDetails markRead(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.markRead(id, userId);
        return ResponseDetails.ok();
    }

    /**
     * 全部标记已读（待处理的员工邀请不受影响）
     */
    @SaCheckLogin
    @PostMapping("/read-all")
    public ResponseDetails markAllRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseDetails.ok().put("data", notificationService.markAllRead(userId));
    }

    /**
     * 删除已处理的通知
     */
    @SaCheckLogin
    @PostMapping("/delete/{id}")
    public ResponseDetails delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        notificationService.deleteNotification(id, userId);
        return ResponseDetails.ok().put("message", "已删除");
    }
}
