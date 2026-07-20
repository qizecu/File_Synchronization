package com.example.syncmanager.controller;

import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.NotifyConfigCreateDTO;
import com.example.syncmanager.dto.NotifyConfigUpdateDTO;
import com.example.syncmanager.entity.NotifyConfig;
import com.example.syncmanager.mapper.NotifyConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notify-configs")
@RequiredArgsConstructor
public class NotifyConfigController {

    private final NotifyConfigMapper notifyConfigMapper;

    /** 列表 */
    @GetMapping
    public Result<List<NotifyConfig>> list() {
        return Result.success(notifyConfigMapper.selectList(null));
    }

    /** 新增 */
    @PostMapping
    public Result<NotifyConfig> create(@RequestBody NotifyConfigCreateDTO dto) {
        NotifyConfig entity = new NotifyConfig();
        BeanUtils.copyProperties(dto, entity);
        notifyConfigMapper.insert(entity);
        return Result.success(entity);
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody NotifyConfigUpdateDTO dto) {
        NotifyConfig entity = new NotifyConfig();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        notifyConfigMapper.updateById(entity);
        return Result.success();
    }

    /** 删除（软删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notifyConfigMapper.deleteById(id);
        return Result.success();
    }
}
