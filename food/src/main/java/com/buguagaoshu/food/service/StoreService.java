package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.config.WebConstant;
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
     * <p>
     * 状态保护：待审核/停业状态的店面，店主不能自行修改状态（需超管审核）；
     * 已开业店面仅允许在 营业中/休息中 之间切换
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
        existingStore.setSort(updateData.getSort());

        // 状态保护：待审核/停业店面不能自行改状态；已开业店面仅能在 营业中/休息中 切换
        boolean statusLocked = existingStore.getStatus().equals(StoreStatus.PENDING_REVIEW.getCode())
                || existingStore.getStatus().equals(StoreStatus.DISABLED.getCode());
        if (!statusLocked && updateData.getStatus() != null
                && (updateData.getStatus().equals(StoreStatus.OPEN.getCode())
                || updateData.getStatus().equals(StoreStatus.CLOSED.getCode()))) {
            existingStore.setStatus(updateData.getStatus());
        }

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
     * 审核店面（超管专用，允许任意合法状态流转，包括 PENDING_REVIEW → OPEN）
     * 与 updateStore 的状态保护逻辑区分开
     * */
    @Transactional(rollbackFor = Exception.class)
    public Store auditStore(Long id, Integer status) {
        Store store = getStore(id);
        store.setStatus(status);
        store.setUpdateTime(System.currentTimeMillis());
        return storeRepository.save(store);
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
     * 用户端分页查询店面列表
     * 不显示待审核、已停业、已删除的店面
     * */
    public Page<Store> findStoresForUser(Integer page, Integer size, String key) {
        List<Integer> excludedStatuses = List.of(
                StoreStatus.PENDING_REVIEW.getCode(), StoreStatus.DISABLED.getCode());
        Page<Store> stores;

        if (StringUtils.hasLength(key)) {
            stores = storeRepository.findByStoreNameLikeAndStatusNotInAndIsDeleted(
                    "%" + key + "%",
                    excludedStatuses,
                    WebConstant.UNDELETED,
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")))
            );
        } else {
            stores = storeRepository.findByStatusNotInAndIsDeleted(
                    excludedStatuses,
                    WebConstant.UNDELETED,
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime"))));
        }

        return fillStoreTags(stores);
    }

    /**
     * 管理员分页查询店面列表，显示所有未删除店面（包含待审核）
     * */
    public Page<Store> findStoresForAdmin(Integer page, Integer size, String key) {
        return findStoresForAdmin(page, size, key, null);
    }

    /**
     * 管理员分页查询店面列表（可选状态过滤，用于审核筛选）
     * status 为 null 时查询全部状态
     * */
    public Page<Store> findStoresForAdmin(Integer page, Integer size, String key, Integer status) {
        Page<Store> stores;
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")));

        if (status != null) {
            if (StringUtils.hasLength(key)) {
                stores = storeRepository.findByStoreNameLikeAndStatusAndIsDeleted(
                        "%" + key + "%", status, WebConstant.UNDELETED, pageRequest);
            } else {
                stores = storeRepository.findByStatusAndIsDeleted(status, WebConstant.UNDELETED, pageRequest);
            }
        } else {
            if (StringUtils.hasLength(key)) {
                stores = storeRepository.findByStoreNameLikeAndIsDeleted(
                        "%" + key + "%", WebConstant.UNDELETED, pageRequest);
            } else {
                stores = storeRepository.findByIsDeleted(WebConstant.UNDELETED, pageRequest);
            }
        }

        return fillStoreTags(stores);
    }

    /**
     * 查询当前用户管理的店面列表（店主/店员端）
     * 依据用户拥有的门店角色（UserRoleData.storeId）确定管理范围：
     * - 普通用户：返回自己拥有门店角色的店面（含待审核）
     * - 超级管理员：返回所有未删除店面
     * */
    public Page<Store> findMyStores(Long userId, Integer page, Integer size, String key, boolean isSuperAdmin) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")));

        // 超级管理员管理所有店面
        if (isSuperAdmin) {
            return findStoresForAdmin(page, size, key);
        }

        // 收集用户拥有门店角色的店面 ID
        List<UserRoleData> storeRoles = userRoleDataService.findStoreRolesByUserId(userId);
        if (storeRoles.isEmpty()) {
            return Page.empty(pageRequest);
        }
        List<Long> storeIds = storeRoles.stream()
                .map(UserRoleData::getStoreId)
                .distinct()
                .collect(Collectors.toList());

        Page<Store> stores;
        if (StringUtils.hasLength(key)) {
            stores = storeRepository.findByIdInAndStoreNameLikeAndIsDeleted(
                    storeIds, "%" + key + "%", WebConstant.UNDELETED, pageRequest);
        } else {
            stores = storeRepository.findByIdInAndIsDeleted(storeIds, WebConstant.UNDELETED, pageRequest);
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
