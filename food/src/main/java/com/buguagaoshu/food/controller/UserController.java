package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.Membership;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.dto.ChangePasswordDto;
import com.buguagaoshu.food.dto.ChangePhoneDto;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.RegisterDto;
import com.buguagaoshu.food.dto.SendRegisterCodeDto;
import com.buguagaoshu.food.dto.SendVerifyCodeDto;
import com.buguagaoshu.food.dto.UpdateProfileDto;
import com.buguagaoshu.food.enums.VerificationCodeScene;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.MembershipService;
import com.buguagaoshu.food.service.OrderService;
import com.buguagaoshu.food.service.SystemSettingsService;
import com.buguagaoshu.food.service.UserDataService;
import com.buguagaoshu.food.service.VerificationCodeService;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 提供用户注册、信息修改、查询等面向普通用户的功能
 * 系统级管理功能（角色分配、状态管理）在 controller.sys 包下
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserDataService userDataService;
    private final MembershipService membershipService;
    private final OrderService orderService;
    private final VerificationCodeService verificationCodeService;
    private final SystemSettingsService systemSettingsService;

    public UserController(UserDataService userDataService,
                          MembershipService membershipService,
                          OrderService orderService,
                          VerificationCodeService verificationCodeService,
                          SystemSettingsService systemSettingsService) {
        this.userDataService = userDataService;
        this.membershipService = membershipService;
        this.orderService = orderService;
        this.verificationCodeService = verificationCodeService;
        this.systemSettingsService = systemSettingsService;
    }

    /**
     * 用户注册
     * <p>
     * 受「系统设置 - 是否开放注册」开关控制：关闭时匿名注册一律拒绝。
     * 但平台管理员（已登录且拥有 {@code user:manage} 权限）通过超级后台「新增用户」
     * 复用本接口时不受开关限制，关闭注册期间仍可建号。
     * 注册成功后自动分配默认角色"顾客"(CUSTOMER)
     */
    @Valid
    @PostMapping("/register")
    public ResponseDetails register(@RequestBody UserData userData) {
        // 平台管理员新增用户放行；其余情况（匿名注册等）仍受注册开关控制
        boolean isAdminCreate = StpUtil.isLogin() && StpUtil.hasPermission("user:manage");
        if (!isAdminCreate && !systemSettingsService.isRegistrationEnabled()) {
            throw new BusinessException(400, "注册功能暂未开放");
        }
        UserData savedUser = userDataService.register(userData);
        savedUser.setPassword(null);
        return ResponseDetails.ok().put("data", savedUser);
    }

    /**
     * 发送注册验证码（公开接口，无需登录）
     * <p>
     * 普通用户在注册页输入手机号或邮箱后调用：先校验目标格式合法且未被注册，
     * 再下发验证码。目标为邮箱走邮件通道，为手机号走短信通道。
     * 受注册开关控制：关闭注册时不允许发送。
     */
    @PostMapping("/register/verify-code/send")
    public ResponseDetails sendRegisterCode(@Valid @RequestBody SendRegisterCodeDto dto) {
        if (!systemSettingsService.isRegistrationEnabled()) {
            throw new BusinessException(400, "注册功能暂未开放");
        }
        String target = userDataService.validateRegisterTarget(dto.getTarget());
        verificationCodeService.sendCode(target, VerificationCodeScene.REGISTER);
        return ResponseDetails.ok("验证码已发送，请注意查收");
    }

    /**
     * 普通用户自助注册（公开接口，无需登录）
     * <p>
     * 与管理员建号接口 {@link #register(UserData)} 区分：本接口必须携带手机号/邮箱验证码，
     * 校验通过后才建号，保证联系方式真实可达。受注册开关控制。
     */
    @PostMapping("/register/self")
    public ResponseDetails selfRegister(@Valid @RequestBody RegisterDto dto) {
        if (!systemSettingsService.isRegistrationEnabled()) {
            throw new BusinessException(400, "注册功能暂未开放");
        }
        String target = userDataService.validateRegisterTarget(dto.getTarget());
        // 校验验证码（仅校验，暂不作废）
        verificationCodeService.verifyCode(target, dto.getCode(), VerificationCodeScene.REGISTER);
        UserData savedUser = userDataService.registerByTarget(target, dto.getPassword(), dto.getNickname());
        // 注册成功后作废验证码，保证一次性使用
        verificationCodeService.consumeCode(target, VerificationCodeScene.REGISTER);
        return ResponseDetails.ok("注册成功").put("data", savedUser);
    }

    /**
     * 修改用户信息
     * 禁止修改系统级参数：status、角色信息、createTime
     */
    @SaCheckLogin
    @PostMapping("/update")
    public ResponseDetails updateUserInfo(@RequestBody UserData userData) {
        UserData updatedUser = userDataService.updateUserInfo(userData);
        updatedUser.setPassword(null);
        return ResponseDetails.ok().put("data", updatedUser);
    }

    /**
     * 根据ID查询用户信息（含角色与权限）
     */
    @SaCheckLogin
    @GetMapping("/{id}")
    public ResponseDetails getUserById(@PathVariable Long id) {
        UserData user = userDataService.findUserWithRolesAndPermissions(id);
        user.setPassword(null);
        return ResponseDetails.ok().put("data", user);
    }

    /**
     * 查询当前登录用户在指定门店的会员信息（顾客端，无需 member:manage 权限）
     * <p>
     * 用于顾客下单/结算页自动同步会员信息：
     * <ul>
     *   <li>优先按当前用户档案手机号在本店查会员记录</li>
     *   <li>手机号为空时按 userId 关联（早期数据可能用 userId 关联）</li>
     *   <li>查无记录返回 membership=null，前端按「非会员」展示，下单后自动建档</li>
     * </ul>
     * 返回内容已脱敏：手机号中间四位用 **** 替换；不含任何敏感备注字段。
     *
     * @param storeId 门店ID
     */
    @SaCheckLogin
    @GetMapping("/me/membership")
    public ResponseDetails myMembership(@RequestParam Long storeId) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserData user = userDataService.findById(userId).orElse(null);
        String phone = user != null ? user.getPhone() : null;

        Membership m = null;
        if (phone != null && !phone.isBlank()) {
            m = membershipService.findByStoreAndPhone(storeId, phone);
        }
        if (m == null) {
            // 手机号未关联会员时，按 userId 兜底查（历史数据可能用 userId 直接关联）
            var list = membershipService.findByStoreIdAndUserId(storeId, userId);
            if (!list.isEmpty()) {
                m = list.get(0);
            }
        }

        Map<String, Object> result = new HashMap<>();
        // 用户档案手机号（脱敏），供前端展示与下单时自动回填
        result.put("phone", phone);
        result.put("maskedPhone", maskPhone(phone));

        if (m != null) {
            Map<String, Object> membership = new HashMap<>();
            membership.put("id", m.getId());
            membership.put("nickname", m.getNickname());
            membership.put("num", m.getNum() != null ? m.getNum() : 0L);
            // 用餐次数按手机号在本店的订单数实时统计（与店主端口径一致）
            if (phone != null && !phone.isBlank()) {
                membership.put("count", orderService.countStoreOrdersByPhone(storeId, phone));
            } else {
                membership.put("count", m.getCount() != null ? m.getCount() : 0L);
            }
            result.put("membership", membership);
        } else {
            result.put("membership", null);
        }
        return ResponseDetails.ok().put("data", result);
    }

    // ==================== 当前用户账户自助管理 ====================

    /**
     * 修改（重置）自己的登录密码
     * <p>
     * 需登录并校验原密码，用户身份取自登录会话，不接受前端传入 userId。
     */
    @SaCheckLogin
    @PostMapping("/account/password")
    public ResponseDetails changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        userDataService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return ResponseDetails.ok("密码修改成功");
    }

    /**
     * 修改自己的头像、昵称
     */
    @SaCheckLogin
    @PostMapping("/account/profile")
    public ResponseDetails updateProfile(@Valid @RequestBody UpdateProfileDto dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserData user = userDataService.updateProfile(userId, dto.getNickname(), dto.getAvatarUrl());
        return ResponseDetails.ok().put("data", user);
    }

    /**
     * 发送手机验证码
     * <p>
     * 场景为「校验原手机号」时，验证码只能发往当前账号已绑定的手机号（忽略前端传值）；
     * 场景为「校验新手机号」时，需通过格式 / 占用校验后才发送。
     */
    @SaCheckLogin
    @PostMapping("/verify-code/send")
    public ResponseDetails sendVerifyCode(@Valid @RequestBody SendVerifyCodeDto dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        VerificationCodeScene scene = VerificationCodeScene.fromCode(dto.getScene());

        String targetPhone;
        if (scene == VerificationCodeScene.CHANGE_PHONE_OLD) {
            UserData current = userDataService.findById(userId)
                    .orElseThrow(() -> new BusinessException(404, "用户不存在"));
            if (!StringUtils.hasText(current.getPhone())) {
                throw new BusinessException(400, "当前账号未绑定手机号，无需校验原手机号");
            }
            targetPhone = current.getPhone();
        } else {
            targetPhone = userDataService.validateNewPhoneAvailable(userId, dto.getPhone());
        }

        verificationCodeService.sendCode(targetPhone, scene);
        return ResponseDetails.ok("验证码已发送，请注意查收");
    }

    /**
     * 修改（绑定）手机号
     * <p>
     * 当前账号已绑定手机号：需同时校验原手机验证码与新手机验证码；
     * 当前账号未绑定手机号（如邮箱注册）：仅校验新手机验证码，属于首次绑定。
     * 采用「先全部校验通过、再作废验证码、最后落库」的顺序，
     * 避免其中一个验证码被提前作废而另一个校验失败。
     */
    @SaCheckLogin
    @PostMapping("/account/phone")
    public ResponseDetails changePhone(@Valid @RequestBody ChangePhoneDto dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserData current = userDataService.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 新手机号可用性校验（格式 / 与当前不同 / 未被占用）
        String newPhone = userDataService.validateNewPhoneAvailable(userId, dto.getNewPhone());

        if (StringUtils.hasText(current.getPhone())) {
            if (!StringUtils.hasText(dto.getOldCode())) {
                throw new BusinessException(400, "请输入原手机号验证码");
            }
            // 校验两个验证码（仅校验，暂不作废）
            verificationCodeService.verifyCode(current.getPhone(), dto.getOldCode(),
                    VerificationCodeScene.CHANGE_PHONE_OLD);
            verificationCodeService.verifyCode(newPhone, dto.getNewCode(),
                    VerificationCodeScene.CHANGE_PHONE_NEW);
            // 全部通过后落库
            userDataService.changePhone(userId, newPhone);
            // 作废两个验证码，保证一次性使用
            verificationCodeService.consumeCode(current.getPhone(), VerificationCodeScene.CHANGE_PHONE_OLD);
            verificationCodeService.consumeCode(newPhone, VerificationCodeScene.CHANGE_PHONE_NEW);
        } else {
            // 首次绑定：仅校验新手机号验证码
            verificationCodeService.verifyCode(newPhone, dto.getNewCode(),
                    VerificationCodeScene.CHANGE_PHONE_NEW);
            userDataService.changePhone(userId, newPhone);
            verificationCodeService.consumeCode(newPhone, VerificationCodeScene.CHANGE_PHONE_NEW);
        }

        return ResponseDetails.ok(StringUtils.hasText(current.getPhone()) ? "手机号修改成功" : "手机号绑定成功");
    }

    /** 手机号脱敏：138****8888，长度非 11 位则原样返回 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
