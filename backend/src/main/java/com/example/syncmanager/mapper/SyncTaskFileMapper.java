package com.example.syncmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.syncmanager.dto.FileRecordVO;
import com.example.syncmanager.entity.SyncTaskFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SyncTaskFileMapper extends BaseMapper<SyncTaskFile> {

    @Select("<script>" +
            "SELECT tf.id, tf.source_path AS fileName, tf.file_origin AS fileOrigin, " +
            "tf.user_id AS userId, u.nickname AS userNickname, " +
            "s.source_name AS sourceName, tf.source_size AS fileSize, " +
            "tf.file_status AS fileStatus, tf.error_msg AS errorMsg, tf.created_at AS createdAt " +
            "FROM sync_task_file tf " +
            "LEFT JOIN sys_user u ON tf.user_id = u.id AND u.is_deleted = 0 " +
            "LEFT JOIN sync_task t ON tf.task_id = t.id AND t.is_deleted = 0 " +
            "LEFT JOIN storage_source s ON t.source_id = s.id AND s.is_deleted = 0 " +
            "WHERE tf.is_deleted = 0 " +
            "<if test='type != null and type != \"ALL\"'>AND tf.file_origin = #{type}</if>" +
            "<if test='filterNullUser'>AND tf.user_id IS NULL</if>" +
            "<if test='!filterNullUser and userId != null'>AND tf.user_id = #{userId}</if>" +
            "<if test='status != null and status != \"\"'>AND tf.file_status = #{status}</if>" +
            "<if test='fileName != null and fileName != \"\"'>AND tf.source_path LIKE CONCAT('%', #{fileName}, '%')</if>" +
            " ORDER BY tf.created_at DESC" +
            "</script>")
    IPage<FileRecordVO> selectFileRecordPage(Page<FileRecordVO> page,
                                              @Param("type") String type,
                                              @Param("userId") Long userId,
                                              @Param("filterNullUser") boolean filterNullUser,
                                              @Param("status") String status,
                                              @Param("fileName") String fileName);
}
