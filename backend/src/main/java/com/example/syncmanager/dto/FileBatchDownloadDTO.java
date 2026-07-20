package com.example.syncmanager.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量下载请求
 */
@Data
public class FileBatchDownloadDTO {

    @NotEmpty(message = "文件路径列表不能为空")
    private List<String> paths;
}
