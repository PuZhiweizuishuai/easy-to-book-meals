package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDiningTableDto {

    @NotNull(message = "门店ID不能为空")
    private Long storefrontId;

    @NotBlank(message = "餐桌号不能为空")
    private String tableNumber;

    @NotNull(message = "座位数不能为空")
    @Min(value = 1, message = "座位数至少为1")
    @Max(value = 100, message = "座位数不能超过100")
    private Integer capacity;

    private Integer status;

    private String area;

    private String qrCodeKey;

    private Integer sortOrder;

    private String remark;

    private Long creatUser;
}
