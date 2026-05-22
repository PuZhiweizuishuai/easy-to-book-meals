package com.buguagaoshu.food.controller.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.enums.RoleDomainEnum;
import com.buguagaoshu.food.service.PermissionDataService;
import org.springframework.web.bind.annotation.*;

/**
 * 权限管理控制器（RBAC-D 域感知）
 * <p>
 * 权限数据由系统初始化时写入，运行期间以只读查询为主
 * 通过 domain 参数区分全局权限和门店权限
 * <p>
 * 全局权限: 平台管理相关权限，分配给全局角色
 * 门店权限: 门店运营相关权限，分配给门店角色
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-22
 */
@RestController
@RequestMapping("/api/sys/permission")
public class PermissionSysController {

    private final PermissionDataService permissionDataService;

    public PermissionSysController(PermissionDataService permissionDataService) {
        this.permissionDataService = permissionDataService;
    }

    /**
     * 查询所有权限
     */
    @SaCheckPermission("permission:view")
    @GetMapping("/list")
    public ResponseDetails listAll() {
        return ResponseDetails.ok().put("data", permissionDataService.findAll());
    }

    /**
     * 查询全局域权限列表
     */
    @SaCheckPermission("permission:view")
    @GetMapping("/global/list")
    public ResponseDetails listGlobalPermissions() {
        return ResponseDetails.ok().put("data", permissionDataService.findGlobalPermissions());
    }

    /**
     * 查询门店域权限列表
     */
    @GetMapping("/store/list")
    public ResponseDetails listStorePermissions() {
        return ResponseDetails.ok().put("data", permissionDataService.findStorePermissions());
    }

    /**
     * 根据ID查询权限
     */
    @SaCheckPermission("permission:view")
    @GetMapping("/{id}")
    public ResponseDetails getById(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", permissionDataService.findById(id).orElse(null));
    }

    /**
     * 按域查询权限
     */
    @SaCheckPermission("permission:view")
    @GetMapping("/domain/{domain}")
    public ResponseDetails listByDomain(@PathVariable String domain) {
        return ResponseDetails.ok().put("data", permissionDataService.findByDomain(domain));
    }
}
