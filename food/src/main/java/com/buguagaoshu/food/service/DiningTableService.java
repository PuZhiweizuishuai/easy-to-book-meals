package com.buguagaoshu.food.service;

import com.buguagaoshu.food.repository.DiningTableRepository;
import org.springframework.stereotype.Service;

@Service
public class DiningTableService {
    private final DiningTableRepository diningTableRepository;

    public DiningTableService(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }


}
