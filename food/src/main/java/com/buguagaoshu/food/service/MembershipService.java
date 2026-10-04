package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.Membership;
import com.buguagaoshu.food.data.PointsTransaction;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.MembershipRepository;
import com.buguagaoshu.food.repository.PointsTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 会员服务
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final PointsTransactionRepository pointsTransactionRepository;

    public MembershipService(MembershipRepository membershipRepository,
                            PointsTransactionRepository pointsTransactionRepository) {
        this.membershipRepository = membershipRepository;
        this.pointsTransactionRepository = pointsTransactionRepository;
    }

    public List<Membership> findAll() {
        List<Membership> result = new ArrayList<>();
        membershipRepository.findAll().forEach(result::add);
        return result;
    }

    public List<Membership> findByUserId(Long userId) {
        return membershipRepository.findByUserId(userId);
    }

    public List<Membership> findByStoreId(Long storeId) {
        return membershipRepository.findByStoreId(storeId);
    }

    public List<Membership> findByStoreIdAndUserId(Long storeId, Long userId) {
        return membershipRepository.findByStoreIdAndUserId(storeId, userId);
    }

    public List<Membership> findByPhone(String phone) {
        return membershipRepository.findByPhone(phone);
    }

    public List<Membership> findByStoreIdAndPhone(Long storeId, String phone) {
        return membershipRepository.findByStoreIdAndPhone(storeId, phone);
    }

    /**
     * 按会员ID+门店ID查询（安全查询，确保门店隔离）
     */
    public Membership findByIdAndStoreId(Long id, Long storeId) {
        return membershipRepository.findByIdAndStoreId(id, storeId).orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Membership save(Membership membership) {
        return membershipRepository.save(membership);
    }

    /**
     * 累加会员积分（无订单上下文，用于手动调整等场景）
     */
    @Transactional(rollbackFor = Exception.class)
    public Membership addPoints(Long storeId, String phone, Long points) {
        return addPointsInternal(storeId, phone, points, null, null,
                PointsTransaction.TYPE_ADJUST, "手动调整积分");
    }

    /**
     * 订单完成时累加会员积分，并记录积分流水。
     *
     * @param storeId  门店ID
     * @param phone    会员手机号（统一关联键）
     * @param points   积分值（1 元 = 1 分）
     * @param orderId  关联订单ID
     * @param orderNo  关联订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public Membership addPointsForOrder(Long storeId, String phone, Long points,
                                        Long orderId, String orderNo) {
        return addPointsInternal(storeId, phone, points, orderId, orderNo,
                PointsTransaction.TYPE_EARN, "消费获取积分");
    }

    private Membership addPointsInternal(Long storeId, String phone, Long points,
                                         Long orderId, String orderNo,
                                         String type, String remark) {
        List<Membership> existing = findByStoreIdAndPhone(storeId, phone);
        Membership m;
        if (existing.isEmpty()) {
            m = new Membership();
            m.setStoreId(storeId);
            m.setPhone(phone);
            m.setNum(points);
        } else {
            m = existing.get(0);
            m.setNum(m.getNum() + points);
        }
        m = membershipRepository.save(m);

        // 记录积分流水
        PointsTransaction tx = new PointsTransaction();
        tx.setStoreId(storeId);
        tx.setMembershipId(m.getId());
        tx.setPhone(phone);
        tx.setOrderId(orderId);
        tx.setOrderNo(orderNo);
        tx.setPoints(points);
        tx.setType(type);
        tx.setRemark(remark);
        if (PointsTransaction.TYPE_ADJUST.equals(type) && StpUtil.isLogin()) {
            tx.setOperatorId(StpUtil.getLoginIdAsLong());
        }
        pointsTransactionRepository.save(tx);

        return m;
    }

    /**
     * 手动调整会员积分（正数增加，负数扣减），并记录流水。
     */
    @Transactional(rollbackFor = Exception.class)
    public Membership adjustPoints(Long storeId, String phone, Long points, String remark) {
        return addPointsInternal(storeId, phone, points, null, null,
                PointsTransaction.TYPE_ADJUST, remark != null ? remark : "手动调整积分");
    }

    /**
     * 分页查询会员积分流水
     */
    public Page<PointsTransaction> findTransactionsByMembershipId(Long membershipId, Pageable pageable) {
        return pointsTransactionRepository.findByMembershipIdOrderByCreateTimeDesc(membershipId, pageable);
    }

    /**
     * 分页查询门店+手机号的积分流水
     */
    public Page<PointsTransaction> findTransactionsByStoreAndPhone(Long storeId, String phone, Pageable pageable) {
        return pointsTransactionRepository.findByStoreIdAndPhoneOrderByCreateTimeDesc(storeId, phone, pageable);
    }

    /**
     * 查询门店会员列表（支持按手机号/昵称模糊搜索）
     */
    public Page<Membership> findStoreMembers(Long storeId, String key, Pageable pageable) {
        if (key != null && !key.trim().isEmpty()) {
            String trimmed = key.trim();
            return membershipRepository.searchByStore(storeId, trimmed, pageable);
        }
        return membershipRepository.findByStoreId(storeId, pageable);
    }

    /**
     * 按门店 + 手机号查询会员（结算时查询会员信息用，无记录返回 null）
     */
    public Membership findByStoreAndPhone(Long storeId, String phone) {
        if (storeId == null || phone == null || phone.isBlank()) {
            return null;
        }
        return membershipRepository.findFirstByStoreIdAndPhone(storeId, phone.trim()).orElse(null);
    }

    /**
     * 修改会员昵称（仅本店生效）
     * <p>
     * 会员昵称是 <b>门店维度</b> 的展示名（membership_table.nickname），与顾客账号昵称
     * （user_table.nickname）相互独立：
     * <ul>
     *   <li>只改本店 membership 记录，不影响顾客在自己账号中设置的昵称</li>
     *   <li>不影响该顾客在其他门店的会员昵称（各门店各存一条 membership）</li>
     * </ul>
     * 因此此处严禁回写 UserData，否则会污染全局账号昵称并跨店串数据。
     * 门店隔离由 membershipId + storeId 双重定位保证。
     */
    @Transactional(rollbackFor = Exception.class)
    public Membership updateNickname(Long storeId, Long membershipId, String nickname) {
        Membership m = membershipRepository.findByIdAndStoreId(membershipId, storeId)
                .orElseThrow(() -> new BusinessException(404, "会员不存在或不属于当前门店"));
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.length() > 30) {
            throw new BusinessException(400, "会员昵称不能超过 30 个字符");
        }
        m.setNickname(trimmed.isEmpty() ? null : trimmed);
        return membershipRepository.save(m);
    }
}
