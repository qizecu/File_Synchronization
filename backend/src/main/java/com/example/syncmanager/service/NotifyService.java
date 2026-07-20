package com.example.syncmanager.service;

import com.example.syncmanager.entity.NotifyConfig;
import com.example.syncmanager.entity.NotifyLog;
import com.example.syncmanager.mapper.NotifyLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 通知服务：钉钉 / 企业微信 Webhook 真实 HTTP 调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotifyLogMapper notifyLogMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送通知到指定渠道
     * @param cfg      通知配置
     * @param taskId   关联任务 ID
     * @param message  通知文本内容
     */
    public void send(NotifyConfig cfg, Long taskId, String message) {
        NotifyLog logEntry = buildLogEntry(cfg, taskId, message);

        try {
            String url = cfg.getWebhookUrl();
            String body;

            if ("DINGTALK".equals(cfg.getNotifyType())) {
                // 钉钉：如果需要加签，拼接 timestamp + sign
                if (cfg.getSecret() != null && !cfg.getSecret().isEmpty()) {
                    long timestamp = System.currentTimeMillis();
                    String sign = dingTalkSign(timestamp, cfg.getSecret());
                    String separator = url.contains("?") ? "&" : "?";
                    url = url + separator + "timestamp=" + timestamp + "&sign=" + sign;
                }
                body = buildDingTalkBody(message);
            } else {
                // 企业微信：直接 POST JSON
                body = buildWeComBody(message);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            logEntry.setNotifyStatus(success ? "SUCCESS" : "FAILED");
            logEntry.setResponseBody(truncate(response.getBody(), 2000));
            log.info("通知发送结果: type={}, status={}, httpStatus={}",
                    cfg.getNotifyType(), success ? "SUCCESS" : "FAILED", response.getStatusCode());

        } catch (Exception e) {
            log.error("通知发送异常: type={}, error={}", cfg.getNotifyType(), e.getMessage());
            logEntry.setNotifyStatus("FAILED");
            logEntry.setErrorMsg(truncate(e.getMessage(), 500));
        } finally {
            notifyLogMapper.insert(logEntry);
        }
    }

    // ==================== 消息体构建 ====================

    /** 钉钉消息体 */
    private String buildDingTalkBody(String message) {
        return "{\"msgtype\":\"text\",\"text\":{\"content\":\"" +
                escapeJson(message) + "\"}}";
    }

    /** 企业微信消息体 */
    private String buildWeComBody(String message) {
        return "{\"msgtype\":\"text\",\"text\":{\"content\":\"" +
                escapeJson(message) + "\"}}";
    }

    // ==================== 钉钉加签 ====================

    /**
     * 钉钉机器人加签算法
     * sign = Base64(HmacSHA256(timestamp + "\n" + secret, secret))
     */
    private String dingTalkSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(
                Base64.getEncoder().encodeToString(signData),
                StandardCharsets.UTF_8);
    }

    // ==================== 辅助 ====================

    private NotifyLog buildLogEntry(NotifyConfig cfg, Long taskId, String message) {
        NotifyLog entry = new NotifyLog();
        entry.setTaskId(taskId);
        entry.setNotifyConfigId(cfg.getId());
        entry.setNotifyType(cfg.getNotifyType());
        entry.setRequestBody(message);
        entry.setNotifyStatus("PENDING");
        return entry;
    }

    /** JSON 字符串转义 */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** 截断长文本 */
    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...(truncated)";
    }
}
