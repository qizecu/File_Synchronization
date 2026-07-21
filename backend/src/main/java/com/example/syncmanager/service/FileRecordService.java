package com.example.syncmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.syncmanager.entity.SyncTaskFile;
import com.example.syncmanager.mapper.SyncTaskFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文件记录去重服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final SyncTaskFileMapper syncTaskFileMapper;

    /**
     * 同步去重：检查同一 source_path 是否已有 SUCCESS 记录
     * @return true 表示已存在（应跳过）
     */
    public boolean existsBySourcePath(String sourcePath) {
        long count = syncTaskFileMapper.selectCount(new LambdaQueryWrapper<SyncTaskFile>()
                .eq(SyncTaskFile::getSourcePath, sourcePath)
                .eq(SyncTaskFile::getFileStatus, "SUCCESS"));
        return count > 0;
    }

    /**
     * 上传去重：检查同一用户的上传目录下是否已有同名文件
     * @return true 表示已存在（应拒绝）
     */
    public boolean existsByUserIdAndSourcePath(Long userId, String sourcePath) {
        long count = syncTaskFileMapper.selectCount(new LambdaQueryWrapper<SyncTaskFile>()
                .eq(SyncTaskFile::getUserId, userId)
                .eq(SyncTaskFile::getSourcePath, sourcePath));
        return count > 0;
    }
}
