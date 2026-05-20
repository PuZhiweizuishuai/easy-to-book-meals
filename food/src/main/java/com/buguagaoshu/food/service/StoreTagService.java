package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.StoreTag;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.StoreTagRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 * 店面标签服务
 */
@Service
public class StoreTagService {
    private final StoreTagRepository storeTagRepository;

    public StoreTagService(StoreTagRepository storeTagRepository) {
        this.storeTagRepository = storeTagRepository;
    }

    /**
     * 获取所有店面标签（从数据库查询，用于初始化缓存）
     * */
    public List<StoreTag> getAllStoreTagsFromDB() {
        List<StoreTag> list = new ArrayList<>();
        storeTagRepository.findAll().forEach(list::add);
        return list;
    }

    /**
     * 获取店面标签Map（用于初始化缓存）
     * */
    public Map<Long, StoreTag> getStoreTagMap() {
        List<StoreTag> allStoreTags = getAllStoreTagsFromDB();
        Map<Long, StoreTag> storeTagMap = new ConcurrentHashMap<>();
        for (StoreTag storeTag : allStoreTags) {
            storeTagMap.put(storeTag.getId(), storeTag);
        }
        return storeTagMap;
    }

    /**
     * 获取所有店面标签（从缓存读取）
     * */
    public List<StoreTag> getAllStoreTags() {
        return CommonlyUsedDataCache.storeTagList;
    }

    /**
     * 根据ID查询标签（从缓存读取）
     * */
    public StoreTag getStoreTag(Long id) {
        StoreTag tag = CommonlyUsedDataCache.storeTagMap.get(id);
        if (tag == null) {
            throw new BusinessException(404, "店面标签不存在");
        }
        return tag;
    }

    /**
     * 创建店面标签
     * 保存到数据库并同步缓存
     * */
    public StoreTag createTag(StoreTag storeTag) {
        storeTag.setCreateTime(System.currentTimeMillis());
        if (storeTag.getSort() == null) {
            storeTag.setSort(0);
        }
        StoreTag savedTag = storeTagRepository.save(storeTag);
        // 同步缓存
        CommonlyUsedDataCache.storeTagMap.put(savedTag.getId(), savedTag);
        CommonlyUsedDataCache.storeTagList.add(savedTag);
        CommonlyUsedDataCache.storeTagList.sort(Comparator.comparingInt(StoreTag::getSort));
        return savedTag;
    }

    /**
     * 更新店面标签
     * 保存到数据库并同步缓存
     * */
    public StoreTag updateTag(StoreTag storeTag) {
        Optional<StoreTag> dbTag = storeTagRepository.findById(storeTag.getId());
        if (!dbTag.isPresent()) {
            throw new BusinessException(404, "更新的店面标签不存在");
        }
        dbTag.get().setName(storeTag.getName());
        dbTag.get().setDescription(storeTag.getDescription());
        if (storeTag.getSort() != null) {
            dbTag.get().setSort(storeTag.getSort());
        }
        StoreTag savedTag = storeTagRepository.save(dbTag.get());
        // 同步缓存
        CommonlyUsedDataCache.storeTagMap.put(savedTag.getId(), savedTag);
        for (int i = 0; i < CommonlyUsedDataCache.storeTagList.size(); i++) {
            if (CommonlyUsedDataCache.storeTagList.get(i).getId().equals(savedTag.getId())) {
                CommonlyUsedDataCache.storeTagList.set(i, savedTag);
                break;
            }
        }
        CommonlyUsedDataCache.storeTagList.sort(Comparator.comparingInt(StoreTag::getSort));
        return savedTag;
    }

    /**
     * 删除店面标签
     * 从数据库删除并同步缓存
     * */
    public boolean deleteTag(Long id) {
        if (!storeTagRepository.existsById(id)) {
            throw new BusinessException(404, "店面标签不存在");
        }
        storeTagRepository.deleteById(id);
        // 同步缓存
        CommonlyUsedDataCache.storeTagMap.remove(id);
        CommonlyUsedDataCache.storeTagList.removeIf(tag -> tag.getId().equals(id));
        return true;
    }
}
