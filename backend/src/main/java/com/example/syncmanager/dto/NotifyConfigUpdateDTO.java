package com.example.syncmanager.dto;

import lombok.Data;

@Data
public class NotifyConfigUpdateDTO {

    private String configName;
    private String webhookUrl;
    private String secret;
    private Integer enabled;
}
