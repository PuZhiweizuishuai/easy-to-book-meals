package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.SpecValue;
import com.buguagaoshu.food.repository.SpecValueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Service
public class SpecValueService {
    private final SpecValueRepository specValueRepository;

    public SpecValueService(SpecValueRepository specValueRepository) {
        this.specValueRepository = specValueRepository;
    }

    public SpecValue saveSpecValue(SpecValue specValue) {
        return specValueRepository.save(specValue);
    }

    public void saveAllSpecValue(Iterable<SpecValue> specValues) {
        specValueRepository.saveAll(specValues);
    }

    public List<SpecValue> findSpecValueByGroupIds(List<Long> ids) {
        return specValueRepository.findAllByGroupIdIn(ids);
    }
}
