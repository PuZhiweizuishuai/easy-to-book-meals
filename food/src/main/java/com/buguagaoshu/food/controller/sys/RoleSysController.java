package com.buguagaoshu.food.controller.sys;

import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.RolePermissionData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.PermissionDataService;
import com.buguagaoshu.food.service.RoleDataService;
import com.buguagaoshu.food.service.RolePermissionDataService;
import org.springframework.web.bind.annotation.*;

/**
 * 系统级角色管理控制器
 * 提供角色的增删改查、角色权限分配等系统级操作
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@RestController
@RequestMapping("/api/sys/role")
public class RoleSysController {

    private final RoleDataService roleDataService;
    private final RolePermissionDataService rolePermissionDataService;
    private final PermissionDataService permissionDataService;

    public RoleSysController(RoleDataService roleDataService,
                             RolePermissionDataService rolePermissionDataService,
                             PermissionDataService permissionDataService) {
        this.roleDataService = roleDataService;
        this.rolePermissionDataService = rolePermissionDataService;
        this.permissionDataService = permissionDataService;
    }

    /**
     * 查询所有角色
     */
    @GetMapping("/list")
    public ResponseDetails listAllRoles() {
        return ResponseDetails.ok().put("data", roleDataService.findAll());
    }

    /**
     * 根据ID查询角色
     */
    @GetMapping("/{id}")
    public ResponseDetails getRoleById(@PathVariable Long id) {
        return ResponseDetails.ok().put("data",
                roleDataService.findById(id).orElse(null));
    }

    /**
     * 创建角色
     */
    @PostMapping("/add")
    public ResponseDetails addRole(@RequestBody RoleData roleData) {
        return ResponseDetails.ok().put("data", roleDataService.save(roleData));
    }

    /**
     * 更新角色信息
     */
    @PostMapping("/update")
    public ResponseDetails updateRole(@RequestBody RoleData roleData) {
        return ResponseDetails.ok().put("data", roleDataService.update(roleData));
    }

    /**
     * 删除角色
     */
    @PostMapping("/delete/{id}")
    public ResponseDetails deleteRole(@PathVariable Long id) {
        roleDataService.deleteById(id);
        return ResponseDetails.ok().put("message", "角色删除成功");
    }

    /**
     * 为角色分配权限
     */
    @PostMapping("/permission/assign")
    public ResponseDetails assignPermission(@RequestParam Long roleId,
                                            @RequestParam Long permissionId) {
        roleDataService.findById(roleId)
                .orElseThrow(() -> new com.buguagaoshu.food.exception.BusinessException(404, "角色不存在"));
        permissionDataService.findById(permissionId)
                .orElseThrow(() -> new com.buguagaoshu.food.exception.BusinessException(404, "权限不存在"));

        RolePermissionData rp = new RolePermissionData();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        return ResponseDetails.ok().put("data", rolePermissionDataService.save(rp));
    }

    /**
     * 移除角色权限
     */
    @PostMapping("/permission/remove")
    public ResponseDetails removePermission(@RequestParam Long roleId,
                                            @RequestParam Long permissionId) {
        rolePermissionDataService.deleteByRoleIdAndPermissionId(roleId, permissionId);
        return ResponseDetails.ok().put("message", "权限移除成功");
    }

    /**
     * 查询角色拥有的权限列表
     */
    @GetMapping("/permission/{roleId}")
    public ResponseDetails getRolePermissions(@PathVariable Long roleId) {
        return ResponseDetails.ok().put("data", rolePermissionDataService.findByRoleId(roleId));
    }

    /**
     * 查询所有权限（只读）
     */
    @GetMapping("/permission/list")
    public ResponseDetails listAllPermissions() {
        return ResponseDetails.ok().put("data", permissionDataService.findAll());
    }
}
