package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.Membership;
import com.buguagaoshu.food.repository.MembershipRepository;
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

    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
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

    @Transactional(rollbackFor = Exception.class)
    public Membership save(Membership membership) {
        return membershipRepository.save(membership);
    }

    @Transactional(rollbackFor = Exception.class)
    public Membership addPoints(Long storeId, String phone, Long points) {
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
        return membershipRepository.save(m);
    }
}
