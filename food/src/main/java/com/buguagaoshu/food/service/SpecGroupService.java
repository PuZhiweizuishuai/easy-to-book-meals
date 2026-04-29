package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.data.SpecValue;
import com.buguagaoshu.food.repository.SpecGroupRepository;
import com.buguagaoshu.food.repository.SpecValueRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public void saveAllSpecGroup(Iterable<SpecGroup> specGroups) {
        specGroupRepository.saveAll(specGroups);
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

        System.out.println(specGroupIds.size());
        for (var s : specValuesIterable) {
            System.out.println(s.getName());
        }

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
