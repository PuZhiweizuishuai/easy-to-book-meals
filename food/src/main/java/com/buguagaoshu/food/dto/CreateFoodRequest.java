package com.buguagaoshu.food.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateFoodRequest {

    private String name;

    private String description;

    private String topImage;

    private String imageList;

    private Long categoryId;

    private List<SpecGroupInput> specGroups;

    @Data
    public static class SpecGroupInput {
        private String name;
        private Integer sort;
        private List<SpecValueInput> values;
    }

    @Data
    public static class SpecValueInput {
        private String name;
        private Integer sort;
    }
}
