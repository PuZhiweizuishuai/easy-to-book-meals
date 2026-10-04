package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.Coupon;
import com.buguagaoshu.food.data.UserCoupon;
import com.buguagaoshu.food.dto.CouponCheckResult;
import com.buguagaoshu.food.dto.CreateCouponDto;
import com.buguagaoshu.food.enums.CouponType;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.CouponRepository;
import com.buguagaoshu.food.repository.UserCouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 优惠券服务
 * <p>
 * 设计要点（业界通行做法）：
 * 1. 模板 + 券码两段式：模板描述规则，券码是唯一可核销凭证（32 位无横线 UUID，可生成二维码）
 * 2. 券码可未登录展示：顾客出示二维码/券码即可核销；登录后可领取关联到账户
 * 3. 三种券模式：满减券、折扣券（可限最高优惠）、无门槛券
 * 4. 校验与核销分离：结算前先试算优惠金额，确认结算时才真正核销，保证事务一致
 * 5. 门店隔离：所有查询均带 storeId，券不可跨店使用
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@Service
public class CouponService {
    private static final Logger logger = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponService(CouponRepository couponRepository,
                         UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    // ==================== 门店管理端 ====================

    /**
     * 创建券模板并批量生成券码
     * <p>
     * 券码生成规则：UUID 去掉横线（32 位），冲突时重试，保证全局唯一。
     *
     * @return 已创建的券模板
     */
    @Transactional(rollbackFor = Exception.class)
    public Coupon createCoupon(CreateCouponDto dto) {
        CouponType type = CouponType.fromCode(dto.getType());
        if (type == null) {
            throw new BusinessException(400, "不支持的优惠券类型");
        }
        validateCouponRule(type, dto);

        Coupon coupon = new Coupon();
        coupon.setStoreId(dto.getStoreId());
        coupon.setName(dto.getName().trim());
        coupon.setType(type.getCode());
        coupon.setDescription(dto.getDescription());
        coupon.setTotalCount(dto.getTotalCount());
        coupon.setPerLimit(dto.getPerLimit() == null ? 1 : Math.max(dto.getPerLimit(), 0));
        coupon.setStartTime(dto.getStartTime());
        coupon.setEndTime(dto.getEndTime());
        coupon.setStatus(Coupon.STATUS_ENABLED);

        // 按券类型填充对应规则字段，其余字段归零避免脏数据
        if (type == CouponType.FULL_REDUCE) {
            coupon.setThresholdAmount(nvl(dto.getThresholdAmount()));
            coupon.setReduceAmount(nvl(dto.getReduceAmount()));
            coupon.setDiscountRate(null);
            coupon.setMaxDiscountAmount(null);
        } else if (type == CouponType.DISCOUNT) {
            coupon.setThresholdAmount(nvl(dto.getThresholdAmount()));
            coupon.setDiscountRate(dto.getDiscountRate());
            coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());
            coupon.setReduceAmount(BigDecimal.ZERO);
        } else {
            coupon.setThresholdAmount(BigDecimal.ZERO);
            coupon.setReduceAmount(nvl(dto.getReduceAmount()));
            coupon.setDiscountRate(null);
            coupon.setMaxDiscountAmount(null);
        }
        if (dto.getEndTime() != null && dto.getEndTime() > 0
                && dto.getStartTime() != null && dto.getEndTime() <= dto.getStartTime()) {
            throw new BusinessException(400, "失效时间必须晚于生效时间");
        }

        Coupon saved = couponRepository.save(coupon);
        generateCodes(saved, dto.getTotalCount());
        logger.info("优惠券创建成功: couponId={}, name={}, type={}, count={}",
                saved.getId(), saved.getName(), type.getName(), dto.getTotalCount());
        return saved;
    }

    /**
     * 批量生成券码
     */
    private void generateCodes(Coupon coupon, int count) {
        List<UserCoupon> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UserCoupon uc = new UserCoupon();
            uc.setStoreId(coupon.getStoreId());
            uc.setCouponId(coupon.getId());
            uc.setCode(generateCode());
            uc.setStatus(UserCoupon.STATUS_UNUSED);
            codes.add(uc);
        }
        userCouponRepository.saveAll(codes);
    }

    /**
     * 生成券码：UUID 去掉横线的 32 位字符串（冲突重试）
     */
    private String generateCode() {
        for (int i = 0; i < 5; i++) {
            String code = UUID.randomUUID().toString().replace("-", "");
            if (userCouponRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new BusinessException(500, "券码生成失败，请重试");
    }

    /**
     * 校验券规则参数完整性
     */
    private void validateCouponRule(CouponType type, CreateCouponDto dto) {
        if (type == CouponType.FULL_REDUCE) {
            BigDecimal threshold = nvl(dto.getThresholdAmount());
            BigDecimal reduce = nvl(dto.getReduceAmount());
            if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "满减券需要设置大于 0 的使用门槛");
            }
            if (reduce.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "满减券需要设置大于 0 的减免金额");
            }
            if (reduce.compareTo(threshold) > 0) {
                throw new BusinessException(400, "减免金额不能大于使用门槛");
            }
        } else if (type == CouponType.DISCOUNT) {
            if (nvl(dto.getThresholdAmount()).compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "折扣券需要设置大于 0 的使用门槛");
            }
            if (dto.getDiscountRate() == null) {
                throw new BusinessException(400, "折扣券需要设置折扣率（如 0.85 表示 85 折）");
            }
            if (dto.getMaxDiscountAmount() != null
                    && dto.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "最高优惠金额必须大于 0");
            }
        } else {
            if (nvl(dto.getReduceAmount()).compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "无门槛券需要设置大于 0 的减免金额");
            }
        }
    }

    /**
     * 门店券模板分页列表（附带领取/核销统计）
     */
    public Page<Coupon> listCoupons(Long storeId, String key, int page, int size) {
        String likeKey = (key == null || key.isBlank()) ? null : key.trim();
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Coupon> coupons = couponRepository.findByStore(storeId, likeKey, pageable);
        coupons.forEach(c -> {
            c.setClaimCount(Math.toIntExact(userCouponRepository.countClaimedByCouponId(c.getId())));
            c.setUsedCount(Math.toIntExact(
                    userCouponRepository.countByCouponIdAndStatus(c.getId(), UserCoupon.STATUS_USED)));
        });
        return coupons;
    }

    /**
     * 券模板详情
     */
    public Coupon getCoupon(Long couponId, Long storeId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(404, "优惠券不存在"));
        if (!coupon.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "优惠券不属于该门店");
        }
        return coupon;
    }

    /**
     * 修改券模板状态（启用/停用），停用时同步作废未使用的券码
     */
    @Transactional(rollbackFor = Exception.class)
    public Coupon updateStatus(Long couponId, Long storeId, Integer status) {
        Coupon coupon = getCoupon(couponId, storeId);
        if (status == null || (status != Coupon.STATUS_ENABLED && status != Coupon.STATUS_DISABLED)) {
            throw new BusinessException(400, "非法的券状态");
        }
        coupon.setStatus(status);
        if (status == Coupon.STATUS_DISABLED) {
            List<UserCoupon> unused = userCouponRepository.findByCouponId(couponId).stream()
                    .filter(u -> u.getStatus() == UserCoupon.STATUS_UNUSED)
                    .toList();
            unused.forEach(u -> u.setStatus(UserCoupon.STATUS_INVALID));
            userCouponRepository.saveAll(unused);
        }
        return couponRepository.save(coupon);
    }

    /**
     * 券码分页列表（附带券模板快照，便于前端展示与生成二维码）
     */
    public Page<UserCoupon> listCodes(Long couponId, Long storeId, Integer status, int page, int size) {
        Coupon coupon = getCoupon(couponId, storeId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "id"));
        Page<UserCoupon> codes = userCouponRepository.findByCoupon(couponId, status, pageable);
        codes.forEach(u -> u.setCoupon(coupon));
        return codes;
    }

    // ==================== 顾客端 ====================

    /**
     * 按券码查询券详情（公开接口，未登录也可展示）
     */
    public Map<String, Object> findByCode(String code) {
        UserCoupon uc = findCode(code);
        Coupon coupon = couponRepository.findById(uc.getCouponId())
                .orElseThrow(() -> new BusinessException(404, "优惠券不存在"));
        uc.setCoupon(coupon);
        Map<String, Object> result = new HashMap<>();
        result.put("userCoupon", uc);
        result.put("coupon", coupon);
        return result;
    }

    /**
     * 领取优惠券：把券码关联到当前登录用户账户
     * <p>
     * 校验：券未被使用、未被他人领取、在有效期内、未超过每人限领数量。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon claim(String code, Long userId, String phone) {
        UserCoupon uc = findCode(code);
        if (uc.getStatus() == UserCoupon.STATUS_USED) {
            throw new BusinessException(400, "该优惠券已被使用");
        }
        if (uc.getStatus() == UserCoupon.STATUS_INVALID) {
            throw new BusinessException(400, "该优惠券已作废");
        }
        if (uc.getUserId() != null && !uc.getUserId().equals(userId)) {
            throw new BusinessException(400, "该优惠券已被其他用户领取");
        }
        Coupon coupon = couponRepository.findById(uc.getCouponId())
                .orElseThrow(() -> new BusinessException(404, "优惠券不存在"));
        checkPeriod(coupon);
        if (coupon.getPerLimit() != null && coupon.getPerLimit() > 0) {
            long held = userCouponRepository.countByCouponIdAndUserId(coupon.getId(), userId);
            if (held >= coupon.getPerLimit()) {
                throw new BusinessException(400, "该优惠券每人限领 " + coupon.getPerLimit() + " 张");
            }
        }
        uc.setUserId(userId);
        if (phone != null && !phone.isBlank()) {
            uc.setPhone(phone.trim());
        }
        uc.setClaimTime(System.currentTimeMillis());
        uc.setCoupon(coupon);
        return userCouponRepository.save(uc);
    }

    /**
     * 我的优惠券（当前登录用户在指定门店持有的券）
     */
    public List<UserCoupon> myCoupons(Long storeId, Long userId) {
        List<UserCoupon> list = userCouponRepository.findByStoreAndUser(storeId, userId, null);
        fillCoupons(list);
        return list;
    }

    /**
     * 按手机号查询该会员在某门店可用的券（店员结算时展示会员名下优惠券）
     *
     * @param amount 当前订单金额，用于试算每张券可优惠金额
     */
    public List<UserCoupon> memberAvailableCoupons(Long storeId, String phone, BigDecimal amount) {
        List<UserCoupon> list = userCouponRepository.findAvailableByPhone(storeId, phone);
        fillCoupons(list);
        // 试算优惠金额，剔除当前订单不可用的券（未达门槛）
        List<UserCoupon> usable = new ArrayList<>();
        for (UserCoupon uc : list) {
            try {
                uc.setDiscountAmount(calculateDiscount(uc.getCoupon(), amount));
                usable.add(uc);
            } catch (BusinessException e) {
                // 未达门槛等不可用券直接跳过
                uc.setDiscountAmount(BigDecimal.ZERO);
            }
        }
        return usable;
    }

    /**
     * 校验券码是否可用于指定订单金额（仅试算，不核销）
     *
     * @param storeId 门店ID
     * @param code    券码
     * @param amount  订单金额（菜品折后合计）
     * @return 校验结果，含优惠金额与优惠后应付金额
     */
    public CouponCheckResult check(Long storeId, String code, BigDecimal amount) {
        UserCoupon uc = findCode(code);
        if (!uc.getStoreId().equals(storeId)) {
            throw new BusinessException(400, "该优惠券不属于当前门店");
        }
        if (uc.getStatus() == UserCoupon.STATUS_USED) {
            throw new BusinessException(400, "该优惠券已被使用");
        }
        if (uc.getStatus() == UserCoupon.STATUS_INVALID) {
            throw new BusinessException(400, "该优惠券已作废");
        }
        Coupon coupon = couponRepository.findById(uc.getCouponId())
                .orElseThrow(() -> new BusinessException(404, "优惠券不存在"));
        if (coupon.getStatus() != null && coupon.getStatus() != Coupon.STATUS_ENABLED) {
            throw new BusinessException(400, "该优惠券已停用");
        }
        checkPeriod(coupon);

        BigDecimal discount = calculateDiscount(coupon, amount);
        BigDecimal payable = nvl(amount).subtract(discount);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }

        CouponCheckResult result = new CouponCheckResult();
        result.setUserCouponId(uc.getId());
        result.setCouponId(coupon.getId());
        result.setCode(uc.getCode());
        result.setName(coupon.getName());
        result.setType(coupon.getType());
        result.setTypeName(coupon.getTypeName());
        result.setThresholdAmount(coupon.getThresholdAmount());
        result.setDiscountAmount(discount.setScale(2, RoundingMode.HALF_UP));
        result.setPayableAmount(payable.setScale(2, RoundingMode.HALF_UP));
        result.setDescription(coupon.getDescription());
        result.setClaimed(uc.getUserId() != null);
        result.setPhone(uc.getPhone());
        return result;
    }

    /**
     * 核销优惠券（订单结算完成时调用）
     * <p>
     * 幂等保护：仅当券码当前状态为「未使用」时才翻转，避免并发重复核销。
     *
     * @return 已核销的券码
     */
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon use(Long storeId, String code, Long orderId, String orderNo, String phone) {
        UserCoupon uc = findCode(code);
        if (!uc.getStoreId().equals(storeId)) {
            throw new BusinessException(400, "该优惠券不属于当前门店");
        }
        if (uc.getStatus() == UserCoupon.STATUS_USED) {
            throw new BusinessException(400, "该优惠券已被使用");
        }
        if (uc.getStatus() == UserCoupon.STATUS_INVALID) {
            throw new BusinessException(400, "该优惠券已作废");
        }
        uc.setStatus(UserCoupon.STATUS_USED);
        uc.setUsedTime(System.currentTimeMillis());
        uc.setOrderId(orderId);
        uc.setOrderNo(orderNo);
        if (phone != null && !phone.isBlank()) {
            uc.setPhone(phone.trim());
        }
        UserCoupon saved = userCouponRepository.save(uc);
        couponRepository.findById(uc.getCouponId()).ifPresent(c -> {
            c.setUsedCount(Math.toIntExact(
                    userCouponRepository.countByCouponIdAndStatus(c.getId(), UserCoupon.STATUS_USED)));
            couponRepository.save(c);
        });
        logger.info("优惠券核销成功: code={}, orderNo={}", code, orderNo);
        return saved;
    }

    /**
     * 计算券在指定订单金额下的优惠金额（不做状态校验，仅算规则）
     *
     * @throws BusinessException 未达门槛时抛出
     */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        BigDecimal orderAmount = nvl(amount);
        CouponType type = CouponType.fromCode(coupon.getType());
        BigDecimal discount;
        if (type == CouponType.NO_THRESHOLD) {
            discount = nvl(coupon.getReduceAmount());
        } else if (type == CouponType.FULL_REDUCE) {
            BigDecimal threshold = nvl(coupon.getThresholdAmount());
            if (orderAmount.compareTo(threshold) < 0) {
                throw new BusinessException(400,
                        "订单金额未满 " + threshold.setScale(2, RoundingMode.HALF_UP) + " 元，无法使用该优惠券");
            }
            discount = nvl(coupon.getReduceAmount());
        } else {
            BigDecimal threshold = nvl(coupon.getThresholdAmount());
            if (orderAmount.compareTo(threshold) < 0) {
                throw new BusinessException(400,
                        "订单金额未满 " + threshold.setScale(2, RoundingMode.HALF_UP) + " 元，无法使用该优惠券");
            }
            BigDecimal rate = coupon.getDiscountRate() == null
                    ? BigDecimal.ONE : coupon.getDiscountRate();
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(rate));
            if (coupon.getMaxDiscountAmount() != null
                    && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        }
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 私有方法 ====================

    private UserCoupon findCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "优惠券码不能为空");
        }
        // 兼容带横线 UUID 或含空格的扫码结果
        String normalized = code.trim().replace("-", "").replace(" ", "");
        return userCouponRepository.findByCode(normalized)
                .orElseThrow(() -> new BusinessException(404, "优惠券码不存在"));
    }

    private void checkPeriod(Coupon coupon) {
        long now = System.currentTimeMillis();
        if (coupon.getStartTime() != null && coupon.getStartTime() > now) {
            throw new BusinessException(400, "该优惠券尚未到生效时间");
        }
        if (coupon.getEndTime() != null && coupon.getEndTime() > 0 && coupon.getEndTime() < now) {
            throw new BusinessException(400, "该优惠券已过期");
        }
    }

    private void fillCoupons(List<UserCoupon> list) {
        if (list.isEmpty()) {
            return;
        }
        Map<Long, Coupon> map = new HashMap<>();
        for (Coupon c : couponRepository.findAllById(list.stream()
                .map(UserCoupon::getCouponId).distinct().toList())) {
            map.put(c.getId(), c);
        }
        list.forEach(u -> u.setCoupon(map.get(u.getCouponId())));
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
