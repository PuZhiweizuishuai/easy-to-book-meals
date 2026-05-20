package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateReservationDto {

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotNull(message = "餐桌ID不能为空")
    private Long tableId;

    private Long userId;

    @NotBlank(message = "预订人姓名不能为空")
    @Size(max = 30, message = "预订人姓名不能超过30个字符")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    @NotNull(message = "就餐人数不能为空")
    @Min(value = 1, message = "就餐人数至少为1")
    @Max(value = 100, message = "就餐人数不能超过100")
    private Integer guestCount;

    @NotNull(message = "预定开始时间不能为空")
    private Long reservedStartTime;

    @NotNull(message = "预定结束时间不能为空")
    private Long reservedEndTime;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    private Long operator;
}
