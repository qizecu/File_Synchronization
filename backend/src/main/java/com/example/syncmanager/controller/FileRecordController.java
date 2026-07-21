package com.example.syncmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.FileRecordQueryDTO;
import com.example.syncmanager.dto.FileRecordVO;
import com.example.syncmanager.entity.SysUser;
import com.example.syncmanager.mapper.SyncTaskFileMapper;
import com.example.syncmanager.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/file-records")
@RequiredArgsConstructor
public class FileRecordController {

    private final SyncTaskFileMapper taskFileMapper;
    private final SysUserMapper sysUserMapper;

    /** 文件记录查询（分页） */
    @GetMapping
    public Result<IPage<FileRecordVO>> list(FileRecordQueryDTO query) {
        SysUser currentUser = getCurrentUser();
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        // 普通用户只能看自己的记录，type 只允许 UPLOAD
        Long effectiveUserId = isAdmin ? query.getUserId() : currentUser.getId();
        String effectiveType = query.getType();
        if (!isAdmin) {
            // 普通用户只显示上传记录
            if (effectiveType == null || "ALL".equals(effectiveType)) {
                effectiveType = "UPLOAD";
            }
        }

        // -1 表示筛选 user_id IS NULL（系统同步记录）
        boolean filterNullUser = isAdmin && effectiveUserId != null && effectiveUserId == -1;
        Long queryUserId = filterNullUser ? null : effectiveUserId;

        return Result.success(taskFileMapper.selectFileRecordPage(
                new Page<>(query.getPage(), query.getSize()),
                effectiveType, queryUserId, filterNullUser, query.getStatus(), query.getFileName()));
    }

    /** 从 SecurityContext 获取当前用户 */
    private SysUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException("无法识别当前用户");
        }
        return user;
    }
}
