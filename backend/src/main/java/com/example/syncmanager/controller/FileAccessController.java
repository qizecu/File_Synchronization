package com.example.syncmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.GrantAccessDTO;
import com.example.syncmanager.dto.UserFileAccessVO;
import com.example.syncmanager.entity.SysUser;
import com.example.syncmanager.entity.UserFileAccess;
import com.example.syncmanager.mapper.SysUserMapper;
import com.example.syncmanager.mapper.UserFileAccessMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件访问权限管理（仅管理员可操作）
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileAccessController {

    private final UserFileAccessMapper userFileAccessMapper;
    private final SysUserMapper sysUserMapper;

    /** 管理员授权用户访问文件/目录 */
    @PostMapping("/grant-access")
    public Result<Void> grantAccess(@Valid @RequestBody GrantAccessDTO dto) {
        Long adminId = getCurrentUserId();

        // 检查用户是否存在
        SysUser user = sysUserMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查是否已存在相同授权（包括软删除的）
        Long existCount = userFileAccessMapper.selectCount(
                new LambdaQueryWrapper<UserFileAccess>()
                        .eq(UserFileAccess::getUserId, dto.getUserId())
                        .eq(UserFileAccess::getFilePath, dto.getFilePath())
        );
        if (existCount > 0) {
            throw new BusinessException("该文件/目录已授权给此用户");
        }

        UserFileAccess access = new UserFileAccess();
        access.setUserId(dto.getUserId());
        access.setFilePath(dto.getFilePath());
        access.setGrantedBy(adminId);
        userFileAccessMapper.insert(access);
        return Result.success();
    }

    /** 撤销授权（通过 access 记录 ID） */
    @DeleteMapping("/grant-access/{id}")
    public Result<Void> revokeAccess(@PathVariable Long id) {
        UserFileAccess access = userFileAccessMapper.selectById(id);
        if (access == null) {
            throw new BusinessException("授权记录不存在");
        }
        userFileAccessMapper.deleteById(id); // 软删除
        return Result.success();
    }

    /** 查看某用户的授权列表 */
    @GetMapping("/grant-access/{userId}")
    public Result<List<UserFileAccessVO>> listUserAccess(@PathVariable Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        List<UserFileAccess> accesses = userFileAccessMapper.selectList(
                new LambdaQueryWrapper<UserFileAccess>()
                        .eq(UserFileAccess::getUserId, userId)
                        .orderByDesc(UserFileAccess::getCreatedAt)
        );

        // 收集所有授权人以批量查询用户名
        Map<Long, String> usernameMap = accesses.stream()
                .map(UserFileAccess::getGrantedBy)
                .distinct()
                .filter(id -> id != null)
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            SysUser u = sysUserMapper.selectById(id);
                            return u != null ? u.getUsername() : "未知";
                        }
                ));

        List<UserFileAccessVO> vos = accesses.stream()
                .map(a -> UserFileAccessVO.builder()
                        .id(a.getId())
                        .userId(a.getUserId())
                        .username(user.getUsername())
                        .filePath(a.getFilePath())
                        .grantedBy(a.getGrantedBy())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        return Result.success(vos);
    }

    /** 获取所有用户的授权概要（管理员分配管理用） */
    @GetMapping("/grant-access/all")
    public Result<List<UserFileAccessVO>> listAllGrants() {
        List<UserFileAccess> accesses = userFileAccessMapper.selectList(
                new LambdaQueryWrapper<UserFileAccess>()
                        .orderByDesc(UserFileAccess::getCreatedAt)
        );

        // 批量查询所有用户
        Map<Long, String> usernameMap = accesses.stream()
                .map(UserFileAccess::getUserId)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            SysUser u = sysUserMapper.selectById(id);
                            return u != null ? u.getUsername() : "未知";
                        }
                ));

        List<UserFileAccessVO> vos = accesses.stream()
                .map(a -> UserFileAccessVO.builder()
                        .id(a.getId())
                        .userId(a.getUserId())
                        .username(usernameMap.getOrDefault(a.getUserId(), "未知"))
                        .filePath(a.getFilePath())
                        .grantedBy(a.getGrantedBy())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        return Result.success(vos);
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException("无法识别当前用户");
        }
        return user.getId();
    }
}
