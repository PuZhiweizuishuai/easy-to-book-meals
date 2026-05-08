package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.data.SpecValue;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.dto.UpdateSpecGroupDto;
import com.buguagaoshu.food.repository.SpecGroupRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Service
public class SpecGroupService {
    private final SpecGroupRepository specGroupRepository;

    private final SpecValueService specValueService;

    public SpecGroupService(SpecGroupRepository specGroupRepository,
                            SpecValueService specValueService) {
        this.specGroupRepository = specGroupRepository;

        this.specValueService = specValueService;
    }

    public SpecGroup saveSpecGroup(SpecGroup specGroup) {
        return specGroupRepository.save(specGroup);
    }

    public Iterable<SpecGroup> saveALlGroups(List<SpecGroup> specGroups) {
        return specGroupRepository.saveAll(specGroups);
    }

    /**
     * 创建 SpecGroup 并保存
     * */
    public List<SpecGroup> createSpecGroups(CreateDishes createDishes, BaseFood baseFood) {
        List<SpecGroup> specGroups = createDishes.getSpecGroups();

        if (specGroups != null && !specGroups.isEmpty()) {
            for (SpecGroup specGroup : specGroups) {
                specGroup.setDishId(baseFood.getId());
            }
            // 批量保存
            saveALlGroups(specGroups);

            for (SpecGroup specGroup : specGroups) {
                // 3. 创建 SpecValue 并保存
                List<SpecValue> specValues = specGroup.getValues();

                if (specValues != null && !specValues.isEmpty()) {
                    // 批量保存 SpecValue
                    specValueService.saveAllSpecValue(specValues);

                    for (SpecValue specValue : specValues) {
                        specValue.setGroupId(specGroup.getId());
                    }
                }
            }
        }
        return specGroups;
    }

    /**
     * 更新 SpecGroup
     * 逻辑如下：
     * 1、获取用户传递过来需要更新的 SpecGroup 组
     * 2、使用 getSpecGroupsByDishId 方法获取当前数据库中的 SpecGroup 组
     * 3、将用户传递过来的新的  SpecGroup 组与旧 SpecGroup 对比
     * 4、批量更新并保存数据
     * 5、返回新的 SpecGroup 组数据
     * */
    public List<SpecGroup> updateSpecGroups(UpdateSpecGroupDto updateSpecGroupDto) {
        // 1. 获取用户传递过来需要更新的 SpecGroup 组
        List<SpecGroup> newSpecGroups = updateSpecGroupDto.getSpecGroups();
        Long dishId = updateSpecGroupDto.getDishesId();

        if (newSpecGroups == null || newSpecGroups.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 使用 getSpecGroupsByDishId 方法获取当前数据库中的 SpecGroup 组
        List<SpecGroup> oldSpecGroups = getSpecGroupsByDishId(dishId);

        // 将旧规格组转换为 Map，方便查找
        Map<Long, SpecGroup> oldSpecGroupMap = oldSpecGroups.stream()
                .collect(Collectors.toMap(SpecGroup::getId, group -> group));

        // 3. 对比新旧数据，进行增删改操作
        List<SpecGroup> updatedGroups = new ArrayList<>();
        List<SpecGroup> groupsToSave = new ArrayList<>();
        List<Long> oldGroupIds = oldSpecGroups.stream()
                .map(SpecGroup::getId)
                .toList();
        List<Long> newGroupIds = newSpecGroups.stream()
                .map(SpecGroup::getId)
                .filter(Objects::nonNull)
                .toList();

        // 找出需要删除的规格组ID
        List<Long> groupIdsToDelete = oldGroupIds.stream()
                .filter(id -> !newGroupIds.contains(id))
                .collect(Collectors.toList());

        for (SpecGroup newGroup : newSpecGroups) {
            newGroup.setDishId(dishId);
            
            if (newGroup.getId() != null && oldSpecGroupMap.containsKey(newGroup.getId())) {
                // 更新现有规格组
                SpecGroup oldGroup = oldSpecGroupMap.get(newGroup.getId());
                // 更新名称和排序
                oldGroup.setName(newGroup.getName());
                oldGroup.setSort(newGroup.getSort());
                
                // 处理规格值
                updateSpecValues(oldGroup, newGroup.getValues());
                
                groupsToSave.add(oldGroup);
                updatedGroups.add(oldGroup);
            } else {
                // 创建新规格组
                newGroup.setId(null); // 确保是新增
                SpecGroup savedGroup = saveSpecGroup(newGroup);
                
                // 处理规格值
                List<SpecValue> newValues = newGroup.getValues();
                if (newValues != null && !newValues.isEmpty()) {
                    for (SpecValue value : newValues) {
                        value.setGroupId(savedGroup.getId());
                    }
                    specValueService.saveAllSpecValue(newValues);
                    savedGroup.setValues(newValues);
                }
                
                updatedGroups.add(savedGroup);
            }
        }

        // 删除需要删除的规格组
        if (!groupIdsToDelete.isEmpty()) {
            specGroupRepository.deleteAllById(groupIdsToDelete);
        }

        // 4. 批量更新并保存数据
        if (!groupsToSave.isEmpty()) {
            saveALlGroups(groupsToSave);
        }

        // 5. 返回新的 SpecGroup 组数据
        return updatedGroups;
    }

    /**
     * 更新规格值
     * */
    private void updateSpecValues(SpecGroup group, List<SpecValue> newValues) {
        List<SpecValue> oldValues = group.getValues();
        if (oldValues == null) {
            oldValues = new ArrayList<>();
        }
        if (newValues == null) {
            newValues = new ArrayList<>();
        }

        // 将旧规格值转换为 Map
        Map<Long, SpecValue> oldValueMap = oldValues.stream()
                .collect(Collectors.toMap(SpecValue::getId, value -> value));

        List<SpecValue> valuesToSave = new ArrayList<>();
        List<Long> oldValueIds = oldValues.stream()
                .map(SpecValue::getId)
                .toList();
        List<Long> newValueIds = newValues.stream()
                .map(SpecValue::getId)
                .filter(Objects::nonNull)
                .toList();

        // 找出需要删除的规格值ID
        List<Long> valueIdsToDelete = oldValueIds.stream()
                .filter(id -> !newValueIds.contains(id))
                .collect(Collectors.toList());

        for (SpecValue newValue : newValues) {
            newValue.setGroupId(group.getId());
            
            if (newValue.getId() != null && oldValueMap.containsKey(newValue.getId())) {
                // 更新现有规格值
                SpecValue oldValue = oldValueMap.get(newValue.getId());
                oldValue.setName(newValue.getName());
                oldValue.setSort(newValue.getSort());
                valuesToSave.add(oldValue);
            } else {
                // 创建新规格值
                newValue.setId(null);
                valuesToSave.add(newValue);
            }
        }

        // 保存规格值
        if (!valuesToSave.isEmpty()) {
            specValueService.saveAllSpecValue(valuesToSave);
        }

        // 删除需要删除的规格值
        if (!valueIdsToDelete.isEmpty()) {
            specValueService.deleteAllSpecValue(valueIdsToDelete);
        }

        // 更新组中的规格值列表
        group.setValues(newValues);
    }


    /**
     * 获取当前菜品所有规格，构建前端选择器
     * */ 
    public List<SpecGroup> getSpecGroupsByDishId(Long dishId) {
        // 1. 查询所有规格组
        List<SpecGroup> specGroups = specGroupRepository.findByDishId(dishId);
        if (specGroups.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 提取所有规格组ID
        List<Long> specGroupIds = specGroups.stream()
                .map(SpecGroup::getId)
                .collect(Collectors.toList());

        // 3. 批量查询出所有规格值 (返回的是 Iterable)
        Iterable<SpecValue> specValuesIterable = specValueService.findSpecValueByGroupIds(specGroupIds);

        // 这样后续遍历时，直接通过 Map.get() 获取，无需反复过滤遍历
        Map<Long, List<SpecValue>> specValueMap = StreamSupport.stream(specValuesIterable.spliterator(), false)
                .collect(Collectors.groupingBy(SpecValue::getGroupId));

        // 5. 遍历规格组，直接从 Map 中取出对应的规格值列表进行赋值
        specGroups.forEach(group -> {
            // 如果该组没有对应的规格值，则返回一个空列表，避免空指针
            List<SpecValue> values = specValueMap.getOrDefault(group.getId(), new ArrayList<>());
            group.setValues(values);
        });

        return specGroups;
    }
}
