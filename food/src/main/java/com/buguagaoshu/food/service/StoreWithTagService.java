package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.StoreTag;
import com.buguagaoshu.food.data.StoreWithTag;
import com.buguagaoshu.food.repository.StoreTagRepository;
import com.buguagaoshu.food.repository.StoreWithTagRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 * 店面与标签关联服务
 */
@Service
public class StoreWithTagService {
    private final StoreWithTagRepository storeWithTagRepository;
    private final StoreTagRepository storeTagRepository;

    public StoreWithTagService(StoreWithTagRepository storeWithTagRepository,
                               StoreTagRepository storeTagRepository) {
        this.storeWithTagRepository = storeWithTagRepository;
        this.storeTagRepository = storeTagRepository;
    }

    /**
     * 关联标签信息（由超级管理员操作）
     * */
    public void associationTag(Long storeId, List<StoreTag> tags) {
        long time = System.currentTimeMillis();
        List<StoreWithTag> storeWithTagList = new ArrayList<>();
        tags.forEach(tag -> {
            // 只有缓存中存在的标签才关联
            if (CommonlyUsedDataCache.storeTagMap.containsKey(tag.getId())) {
                StoreWithTag storeWithTag = new StoreWithTag();
                storeWithTag.setStoreId(storeId);
                storeWithTag.setTagId(tag.getId());
                storeWithTag.setCreateTime(time);
                storeWithTagList.add(storeWithTag);
            }
        });
        storeWithTagRepository.saveAll(storeWithTagList);
    }

    /**
     * 补全标签详细信息（从缓存读取）
     * 将标签ID列表转换为包含完整信息的标签对象列表
     * */
    public List<StoreTag> completionTag(List<StoreTag> tags) {
        List<StoreTag> storeTagsInfo = new ArrayList<>();
        for (StoreTag storeTag : tags) {
            StoreTag fullTag = CommonlyUsedDataCache.storeTagMap.get(storeTag.getId());
            if (fullTag != null) {
                storeTagsInfo.add(fullTag);
            }
        }
        return storeTagsInfo;
    }

    /**
     * 获取店面关联的标签信息（从缓存读取）
     * */
    public List<StoreTag> getStoreTags(Long storeId) {
        List<StoreWithTag> storeWithTags = storeWithTagRepository.findAllByStoreId(storeId);
        List<StoreTag> storeTagsInfo = new ArrayList<>();
        if (storeWithTags != null && !storeWithTags.isEmpty()) {
            for (StoreWithTag storeWithTag : storeWithTags) {
                StoreTag tag = CommonlyUsedDataCache.storeTagMap.get(storeWithTag.getTagId());
                if (tag != null) {
                    storeTagsInfo.add(tag);
                }
            }
        }
        return storeTagsInfo;
    }

    /**
     * 批量查询多个店面关联的标签
     * */
    public List<StoreWithTag> findByStoreIdIn(List<Long> storeIds) {
        return storeWithTagRepository.findByStoreIdIn(storeIds);
    }

    /**
     * 批量根据标签ID查询标签详情（从缓存读取），返回 tagId -> StoreTag 的映射
     * 用于分页查询时一次性获取所有标签信息，避免N+1查询问题
     * */
    public Map<Long, StoreTag> findTagMapByStoreWithTags(List<StoreWithTag> storeWithTags) {
        Set<Long> tagIds = storeWithTags.stream()
                .map(StoreWithTag::getTagId)
                .collect(Collectors.toSet());
        if (tagIds.isEmpty()) {
            return Map.of();
        }
        // 从缓存中获取标签信息
        Map<Long, StoreTag> result = new java.util.HashMap<>();
        for (Long tagId : tagIds) {
            StoreTag tag = CommonlyUsedDataCache.storeTagMap.get(tagId);
            if (tag != null) {
                result.put(tagId, tag);
            }
        }
        return result;
    }

    /**
     * 更新店面关联的标签信息（由超级管理员操作）
     * 先删除旧的关联关系，再建立新的关联关系
     * */
    public void updateStoreTags(Long storeId, List<StoreTag> newTags) {
        storeWithTagRepository.deleteByStoreId(storeId);
        if (newTags != null && !newTags.isEmpty()) {
            associationTag(storeId, newTags);
        }
    }
}
