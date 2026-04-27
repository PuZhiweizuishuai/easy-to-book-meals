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

    public SpecGroupService(SpecGroupRepository specGroupRepository) {
        this.specGroupRepository = specGroupRepository;
    }

    public SpecGroup saveSpecGroup(SpecGroup specGroup) {
        return specGroupRepository.save(specGroup);
    }

    public void saveAllSpecGroup(Iterable<SpecGroup> specGroups) {
        specGroupRepository.saveAll(specGroups);
    }
}
