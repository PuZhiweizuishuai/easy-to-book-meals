package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.MembershipPoint;
import com.buguagaoshu.food.repository.MembershipPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 会员积分服务
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@Service
public class MembershipPointService {

    private final MembershipPointRepository membershipPointRepository;

    public MembershipPointService(MembershipPointRepository membershipPointRepository) {
        this.membershipPointRepository = membershipPointRepository;
    }

    public List<MembershipPoint> findAll() {
        List<MembershipPoint> result = new ArrayList<>();
        membershipPointRepository.findAll().forEach(result::add);
        return result;
    }

    public List<MembershipPoint> findByUserId(Long userId) {
        return membershipPointRepository.findByUserId(userId);
    }

    public List<MembershipPoint> findByStoreId(Long storeId) {
        return membershipPointRepository.findByStoreId(storeId);
    }

    public List<MembershipPoint> findByStoreIdAndUserId(Long storeId, Long userId) {
        return membershipPointRepository.findByStoreIdAndUserId(storeId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public MembershipPoint save(MembershipPoint membershipPoint) {
        return membershipPointRepository.save(membershipPoint);
    }

    @Transactional(rollbackFor = Exception.class)
    public MembershipPoint addPoints(Long storeId, Long userId, Long points) {
        List<MembershipPoint> existing = findByStoreIdAndUserId(storeId, userId);
        MembershipPoint mp;
        if (existing.isEmpty()) {
            mp = new MembershipPoint();
            mp.setStoreId(storeId);
            mp.setUserId(userId);
            mp.setNum(points);
        } else {
            mp = existing.get(0);
            mp.setNum(mp.getNum() + points);
        }
        return membershipPointRepository.save(mp);
    }
}
