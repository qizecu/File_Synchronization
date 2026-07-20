package com.example.syncmanager.controller;

import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.StorageSourceCreateDTO;
import com.example.syncmanager.dto.StorageSourceUpdateDTO;
import com.example.syncmanager.entity.StorageSource;
import com.example.syncmanager.mapper.StorageSourceMapper;
import com.example.syncmanager.service.adapter.StorageAdapter;
import com.example.syncmanager.service.adapter.StorageAdapterFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storage-sources")
@RequiredArgsConstructor
public class StorageSourceController {

    private final StorageSourceMapper sourceMapper;

    /** 列表 */
    @GetMapping
    public Result<List<StorageSource>> list() {
        return Result.success(sourceMapper.selectList(null));
    }

    /** 新增 */
    @PostMapping
    public Result<StorageSource> create(@RequestBody StorageSourceCreateDTO dto) {
        StorageSource entity = new StorageSource();
        BeanUtils.copyProperties(dto, entity);
        sourceMapper.insert(entity);
        return Result.success(entity);
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody StorageSourceUpdateDTO dto) {
        StorageSource entity = new StorageSource();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        sourceMapper.updateById(entity);
        // 清除适配器缓存，下次使用新配置重连
        StorageAdapterFactory.invalidate(id);
        return Result.success();
    }

    /** 删除（软删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sourceMapper.deleteById(id);
        StorageAdapterFactory.invalidate(id);
        return Result.success();
    }

    /** 测试连通性 */
    @PostMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        StorageSource source = sourceMapper.selectById(id);
        StorageAdapter adapter = StorageAdapterFactory.getOrCreate(source);
        return Result.success(adapter.testConnection());
    }
}
