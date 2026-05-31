package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.data.StoreTag;
import com.buguagaoshu.food.data.StoreWithTag;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.enums.RbacRoleCode;
import com.buguagaoshu.food.enums.StoreStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 * 店面服务
 */
@Service
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreWithTagService storeWithTagService;
    private final UserRoleDataService userRoleDataService;

    public StoreService(StoreRepository storeRepository,
                        StoreWithTagService storeWithTagService,
                        UserRoleDataService userRoleDataService) {
        this.storeRepository = storeRepository;
        this.storeWithTagService = storeWithTagService;
        this.userRoleDataService = userRoleDataService;
    }

    /**
     * 创建店面
     * 新增店面时不对标签关联做任何修改，标签由超级管理员独立管理
     * */
    @Transactional(rollbackFor = Exception.class)
    public Store saveStore(Store store) {
        store.setCreateUser(StpUtil.getLoginIdAsLong());
        long time = System.currentTimeMillis();
        store.setCreateTime(time);
        store.setUpdateTime(time);
        store.setIsDeleted(0);
        store.setMonthlySales(0L);
        store.setAllSales(0L);
        store.setStatus(StoreStatus.PENDING_REVIEW.getCode());

        Store savedStore = storeRepository.save(store);
        savedStore.setTags(new ArrayList<>());
        // 写入角色权限关联
        UserRoleData ownerRole = new UserRoleData();
        ownerRole.setUserId(store.getCreateUser());
        ownerRole.setRoleId(RolePermissionCache.ROLE_CODE_MAP.get(RbacRoleCode.STORE_OWNER.getCode()).getId());
        ownerRole.setStoreId(savedStore.getId());
        userRoleDataService.save(ownerRole);
        return savedStore;
    }

    /**
     * 更新店面信息
     * 只允许修改用户可编辑的属性，保护系统维护的属性不被篡改
     * 系统维护的属性：monthlySales、allSales、createTime、isDeleted（由系统自动管理）
     * */
    @Transactional(rollbackFor = Exception.class)
    public Store updateStore(Store updateData) {
        Store existingStore = getStore(updateData.getId());

        // 复制用户可修改的属性
        existingStore.setStoreName(updateData.getStoreName());
        existingStore.setAddress(updateData.getAddress());
        existingStore.setPhone(updateData.getPhone());
        existingStore.setLogoUrl(updateData.getLogoUrl());
        existingStore.setBusinessHours(updateData.getBusinessHours());
        existingStore.setDescription(updateData.getDescription());
        existingStore.setStatus(updateData.getStatus());
        existingStore.setSort(updateData.getSort());

        // 更新系统维护的时间戳
        existingStore.setUpdateTime(System.currentTimeMillis());

        Store savedStore = storeRepository.save(existingStore);

        // 处理标签变更：先删除旧关联，再建立新关联
        List<StoreTag> newTags = updateData.getTags();
        storeWithTagService.updateStoreTags(savedStore.getId(), newTags);

        // 补全标签信息用于返回
        if (newTags != null && !newTags.isEmpty()) {
            savedStore.setTags(storeWithTagService.completionTag(newTags));
        } else {
            savedStore.setTags(new ArrayList<>());
        }

        return savedStore;
    }

    /**
     * 根据ID查询店面
     * */
    public Store getStore(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "店面不存在"));
    }

    /**
     * 删除店面（逻辑删除）
     * */
    @Transactional(rollbackFor = Exception.class)
    public Store deleteStore(Long id) {
        Store store = getStore(id);
        store.setIsDeleted(1);
        store.setUpdateTime(System.currentTimeMillis());
        return storeRepository.save(store);
    }

    /**
     * 用户端分页查询店面列表，不显示待审核的店面
     * */
    public Page<Store> findStoresForUser(Integer page, Integer size, String key) {
        List<Integer> excludedStatuses = List.of(StoreStatus.PENDING_REVIEW.getCode());
        Page<Store> stores;

        if (StringUtils.hasLength(key)) {
            stores = storeRepository.findByStoreNameLikeAndStatusNotIn(
                    "%" + key + "%",
                    excludedStatuses,
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")))
            );
        } else {
            stores = storeRepository.findByStatusNotIn(
                    excludedStatuses,
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime"))));
        }

        return fillStoreTags(stores);
    }

    /**
     * 管理员分页查询店面列表，显示所有店面（包含待审核）
     * */
    public Page<Store> findStoresForAdmin(Integer page, Integer size, String key) {
        Page<Store> stores;

        if (StringUtils.hasLength(key)) {
            stores = storeRepository.findByStoreNameLike(
                    "%" + key + "%",
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")))
            );
        } else {
            stores = storeRepository.findAll(
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime"))));
        }

        return fillStoreTags(stores);
    }

    /**
     * 公共方法：为分页结果中的店面填充标签数据
     * 批量查询标签关联和详情，避免N+1查询问题
     * */
    private Page<Store> fillStoreTags(Page<Store> stores) {
        if (stores.isEmpty()) {
            return stores;
        }

        List<Long> storeIds = stores.getContent().stream()
                .map(Store::getId)
                .collect(Collectors.toList());

        List<StoreWithTag> allStoreWithTags = storeWithTagService.findByStoreIdIn(storeIds);

        Map<Long, List<StoreWithTag>> tagMap = allStoreWithTags.stream()
                .collect(Collectors.groupingBy(StoreWithTag::getStoreId));

        Map<Long, StoreTag> storeTagMap = storeWithTagService.findTagMapByStoreWithTags(allStoreWithTags);

        stores.forEach(store -> {
            List<StoreWithTag> storeWithTags = tagMap.get(store.getId());
            if (storeWithTags != null) {
                List<StoreTag> storeTags = new ArrayList<>();
                for (StoreWithTag storeWithTag : storeWithTags) {
                    StoreTag tag = storeTagMap.get(storeWithTag.getTagId());
                    if (tag != null) {
                        storeTags.add(tag);
                    }
                }
                store.setTags(storeTags);
            }
        });

        return stores;
    }
}
