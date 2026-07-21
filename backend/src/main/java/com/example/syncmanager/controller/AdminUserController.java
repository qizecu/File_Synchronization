package com.example.syncmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.ResetPasswordDTO;
import com.example.syncmanager.dto.UserCreateDTO;
import com.example.syncmanager.dto.UserUpdateDTO;
import com.example.syncmanager.dto.UserVO;
import com.example.syncmanager.entity.SysUser;
import com.example.syncmanager.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

/**
 * 用户管理接口（仅 ADMIN 角色可访问）
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${sync.local.storage-path}")
    private String baseStoragePath;

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
        // 检查活跃用户（@TableLogic 自动过滤 is_deleted=1）
        Long activeCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (activeCount > 0) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setNickname(dto.getNickname());
        user.setRole(dto.getRole());
        user.setStatus(1);

        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突：存在 is_deleted=1 的历史记录，清理后重试
            log.warn("用户名 {} 存在软删除记录，正在清理...", dto.getUsername());
            cleanDeletedRecords(dto.getUsername());
            sysUserMapper.insert(user);
        }

        // 自动创建用户上传目录：{storagePath}/users/{username}_file/
        String userDir = baseStoragePath + "/users/" + user.getUsername() + "_file";
        File dir = new File(userDir);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("已为用户 {} 创建上传目录: {}", user.getUsername(), userDir);
        }

        return Result.success();
    }

    /** 清理同名软删除记录：重命名释放 uk_username 唯一索引 */
    private void cleanDeletedRecords(String username) {
        // 使用自定义 SQL 绕过 @TableLogic，直接查询 is_deleted=1 的记录
        List<SysUser> deletedList = sysUserMapper.selectDeletedByUsername(username);
        for (SysUser u : deletedList) {
            String newName = username + "_deleted_" + u.getId();
            u.setUsername(newName);
            sysUserMapper.updateById(u);
            log.info("已重命名软删除记录: {} -> {}", username, newName);
        }
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
        // 软删除前重命名用户名，释放原用户名避免唯一性冲突
        String oldUsername = user.getUsername();
        String newUsername = oldUsername + "_deleted_" + id;
        user.setUsername(newUsername);
        sysUserMapper.updateById(user);
        log.info("用户已重命名: {} -> {}", oldUsername, newUsername);

        sysUserMapper.deleteById(id);
        return Result.success();
    }

    /** 重置密码 */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserMapper.updateById(user);
        return Result.success();
    }
}
