package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateReservationDto {

    @NotNull(message = "预定ID不能为空")
    private Long id;

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    private Long tableId;

    @Size(max = 30, message = "预订人姓名不能超过30个字符")
    private String contactName;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    private Integer guestCount;

    private Long reservedStartTime;

    private Long reservedEndTime;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    private Integer status;

    private Long operator;
}
