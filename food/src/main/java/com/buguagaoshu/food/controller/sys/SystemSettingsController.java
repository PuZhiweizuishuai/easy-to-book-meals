package com.buguagaoshu.food.controller.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.buguagaoshu.food.data.SystemSettingsData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.UpdateSystemSettingsDto;
import com.buguagaoshu.food.service.SystemSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 系统设置控制器（超级后台）
 * <p>
 * 查看 / 修改全局系统配置：注册开关、登录失败次数、锁定时长。
 * 修改实时生效（服务端缓存自动刷新），无需重启。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 */
@RestController
@RequestMapping("/api/sys/settings")
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    public SystemSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    /**
     * 查询当前系统设置
     */
    @SaCheckPermission("system:config")
    @GetMapping
    public ResponseDetails getSettings() {
        return ResponseDetails.ok().put("data", systemSettingsService.getSettings());
    }

    /**
     * 更新系统设置（仅更新提交的字段）
     */
    @SaCheckPermission("system:config")
    @PostMapping
    public ResponseDetails updateSettings(@Valid @RequestBody UpdateSystemSettingsDto dto) {
        SystemSettingsData settings = systemSettingsService.updateSettings(dto);
        return ResponseDetails.ok("系统设置已保存").put("data", settings);
    }
}
