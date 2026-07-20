package com.example.syncmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.UserCreateDTO;
import com.example.syncmanager.dto.UserUpdateDTO;
import com.example.syncmanager.dto.UserVO;
import com.example.syncmanager.entity.SysUser;
import com.example.syncmanager.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口（仅 ADMIN 角色可访问）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /** 默认新用户密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    /** 用户列表（分页） */
    @GetMapping
    public Result<IPage<UserVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        IPage<SysUser> result = sysUserMapper.selectPage(pageParam, wrapper);
        IPage<UserVO> voPage = result.convert(u -> UserVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .role(u.getRole())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .build());
        return Result.success(voPage);
    }

    /** 新增用户 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        // 检查用户名唯一性
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD)); // 默认密码 123456
        user.setNickname(dto.getNickname());
        user.setRole(dto.getRole());
        user.setStatus(1);
        sysUserMapper.insert(user);
        return Result.success();
    }

    /** 编辑用户 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getRole())) {
            user.setRole(dto.getRole());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        sysUserMapper.updateById(user);
        return Result.success();
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 不允许删除自己或 admin
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("不能删除超级管理员");
        }
        sysUserMapper.deleteById(id); // 软删除（MyBatis-Plus 逻辑删除）
        return Result.success();
    }

    /** 重置密码 */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        sysUserMapper.updateById(user);
        return Result.success();
    }
}
