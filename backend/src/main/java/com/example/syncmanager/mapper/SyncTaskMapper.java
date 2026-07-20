package com.example.syncmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.syncmanager.entity.SyncTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SyncTaskMapper extends BaseMapper<SyncTask> {
}
