package com.example.syncmanager.dto;

import lombok.Data;

@Data
public class NotifyConfigCreateDTO {

    private String configName;
    private String notifyType;
    private String webhookUrl;
    private String secret;
    private Boolean enabled;
}
