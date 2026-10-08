package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.Store;
import com.buguagaoshu.food.data.StoreTag;
import com.buguagaoshu.food.data.StoreWithTag;
import com.buguagaoshu.food.data.UserRoleData;
import com.buguagaoshu.food.enums.RbacRoleCode;
import com.buguagaoshu.food.enums.ReturnCodeEnum;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ClientAuditService clientAuditService;

    public StoreService(StoreRepository storeRepository,
                        StoreWithTagService storeWithTagService,
                        UserRoleDataService userRoleDataService,
                        ClientAuditService clientAuditService) {
        this.storeRepository = storeRepository;
        this.storeWithTagService = storeWithTagService;
        this.userRoleDataService = userRoleDataService;
        this.clientAuditService = clientAuditService;
    }

    /**
     * 创建店面（申请开店）
     * 新增店面时不对标签关联做任何修改，标签由超级管理员独立管理
     * <p>
     * 角色授予规则：创建时不授予店主角色，门店审核通过（auditStore 置为营业中）后
     * 才为创建者绑定 STORE_OWNER 角色
     * */
    @Transactional(rollbackFor = Exception.class)
    public Store saveStore(Store store) {
        String storeName = requireText(store.getStoreName(), "店面名称不能为空");
        requireText(store.getAddress(), "店面地址不能为空");
        requireText(store.getPhone(), "联系电话不能为空");
        // storeName 是唯一列，MySQL 中空串 '' 会因唯一键冲突触发 1062，须先归一化并查重
        store.setStoreName(storeName);
        if (storeRepository.findByStoreNameAndIsDeleted(storeName, WebConstant.UNDELETED).isPresent()) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "店面名称已存在: " + storeName);
        }

        long userId = StpUtil.getLoginIdAsLong();
        // 同一用户名下存在待审核门店时，须等审核结束（通过/拒绝）后才能再次创建
        if (storeRepository.existsByCreateUserAndStatusAndIsDeleted(
                userId, StoreStatus.PENDING_REVIEW.getCode(), WebConstant.UNDELETED)) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(),
                    "您有待审核的门店，需审核通过后才能创建新门店");
        }

        store.setCreateUser(userId);
        long time = System.currentTimeMillis();
        store.setCreateTime(time);
        store.setUpdateTime(time);
        store.setIsDeleted(0);
        store.setMonthlySales(0L);
        store.setAllSales(0L);
        store.setStatus(StoreStatus.PENDING_REVIEW.getCode());
        // 开店申请来源留痕（IP/UA/位置）：由服务端按当前请求写入，不取请求体中的同名字段
        clientAuditService.fill(store);

        Store savedStore = storeRepository.save(store);
        savedStore.setTags(new ArrayList<>());
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

        String storeName = requireText(updateData.getStoreName(), "店面名称不能为空");
        requireText(updateData.getAddress(), "店面地址不能为空");
        requireText(updateData.getPhone(), "联系电话不能为空");
        // 改名时排除自身查重，避免空串或重名写入唯一列触发 1062
        if (!storeName.equals(existingStore.getStoreName())
                && storeRepository.findByStoreNameAndIsDeletedAndIdNot(storeName, WebConstant.UNDELETED, existingStore.getId()).isPresent()) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "店面名称已存在: " + storeName);
        }

        // 复制用户可修改的属性
        existingStore.setStoreName(storeName);
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
     * <p>
     * 审核通过（置为营业中）时，为门店创建者授予 STORE_OWNER 店主角色（幂等）
     * */
    @Transactional(rollbackFor = Exception.class)
    public Store auditStore(Long id, Integer status) {
        Store store = getStore(id);
        store.setStatus(status);
        store.setUpdateTime(System.currentTimeMillis());
        Store savedStore = storeRepository.save(store);

        if (StoreStatus.OPEN.getCode() == status) {
            grantOwnerRoleIfNeeded(savedStore);
        }
        return savedStore;
    }

    /**
     * 为门店创建者绑定 STORE_OWNER 角色（已绑定时跳过）
     * 恢复营业等重复审核场景下幂等执行
     * */
    private void grantOwnerRoleIfNeeded(Store store) {
        RoleData ownerRole = RolePermissionCache.ROLE_CODE_MAP.get(RbacRoleCode.STORE_OWNER.getCode());
        if (ownerRole == null || store.getCreateUser() == null) {
            return;
        }
        if (userRoleDataService.existsByUserIdAndRoleIdAndStoreId(store.getCreateUser(), ownerRole.getId(), store.getId())) {
            return;
        }
        UserRoleData ownerRoleBinding = new UserRoleData();
        ownerRoleBinding.setUserId(store.getCreateUser());
        ownerRoleBinding.setRoleId(ownerRole.getId());
        ownerRoleBinding.setStoreId(store.getId());
        userRoleDataService.save(ownerRoleBinding);
    }

    /**
     * 校验必填文本非空白，返回去除首尾空白后的值
     */
    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), message);
        }
        return value.trim();
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
     * 查询与当前用户相关的店面列表（店主/店员端）
     * 范围 = 拥有门店角色的店面 ∪ 自己创建的店面（含待审核：
     * 店主角色在审核通过后才授予，待审核门店靠创建人身份纳入）
     * <p>
     * 结果中 hasManageRole 标记用户是否在该店拥有门店角色，
     * 前端据此决定是否展示“进入管理”入口；
     * 超级管理员查看所有门店请走 /api/sys/store/list（店铺管理）
     * */
    public Page<Store> findMyStores(Long userId, Integer page, Integer size, String key) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")));

        // 拥有门店角色的店面 ID（可实际管理）
        List<UserRoleData> storeRoles = userRoleDataService.findStoreRolesByUserId(userId);
        Set<Long> managedStoreIds = storeRoles.stream()
                .map(UserRoleData::getStoreId)
                .collect(Collectors.toSet());

        // 并入自己创建的店面（待审核门店此时还没有店主角色）
        Set<Long> storeIds = new HashSet<>(managedStoreIds);
        storeRepository.findByCreateUserAndIsDeleted(userId, WebConstant.UNDELETED)
                .forEach(s -> storeIds.add(s.getId()));

        if (storeIds.isEmpty()) {
            return Page.empty(pageRequest);
        }
        List<Long> idList = new ArrayList<>(storeIds);

        Page<Store> stores;
        if (StringUtils.hasLength(key)) {
            stores = storeRepository.findByIdInAndStoreNameLikeAndIsDeleted(
                    idList, "%" + key + "%", WebConstant.UNDELETED, pageRequest);
        } else {
            stores = storeRepository.findByIdInAndIsDeleted(idList, WebConstant.UNDELETED, pageRequest);
        }

        stores.forEach(s -> s.setHasManageRole(managedStoreIds.contains(s.getId())));
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
