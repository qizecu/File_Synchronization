package com.example.syncmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.syncmanager.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 查询软删除记录（绕过 @TableLogic 自动过滤） */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 1")
    List<SysUser> selectDeletedByUsername(@Param("username") String username);
}
