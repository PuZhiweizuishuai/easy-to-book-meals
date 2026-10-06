package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改自己的头像、昵称入参
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@Data
public class UpdateProfileDto {

    /**
     * 昵称
     */
    @Size(max = 50, message = "昵称最长不超过 50 个字符")
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatarUrl;
}
