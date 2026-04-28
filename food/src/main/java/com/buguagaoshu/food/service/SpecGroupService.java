package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.repository.SpecGroupRepository;
import org.springframework.stereotype.Service;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Service
public class SpecGroupService {
    private final SpecGroupRepository specGroupRepository;
    private final SpecValueRepository specValueRepository;

    public SpecGroupService(SpecGroupRepository specGroupRepository, 
                            SpecValueRepository specValueRepository) {
        this.specGroupRepository = specGroupRepository;
        this.specValueRepository = specValueRepository;
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
        List<SpecGroup> specGroups = specGroupRepository.findByDishId(dishId);
        // 补充规格值
        // 提取所有规格组ID
        List<Long> specGroupIds = specGroups.stream()
                .map(SpecGroup::getId)
                .collect(Collectors.toList());
        // 用提取到的规格组ID批量查询出所有规格值
        List<SpecValue> specValues = specValueRepository.findAllById(specGroupIds);
        // 合并规格组和规格值
        specGroups.forEach(group -> group.setValues(specValues.stream()
                .filter(value -> value.getSpecGroupId().equals(group.getId()))
                .collect(Collectors.toList())));
        return specGroups;
    }
}
