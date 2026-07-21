package com.example.syncmanager.common;

/**
 * 错误信息转换器 — 将技术性异常信息转为用户友好的中文提示
 */
public final class ErrorMsgTranslator {

    private ErrorMsgTranslator() {}

    /** 将异常转为用户可读的错误信息 */
    public static String translate(Throwable e) {
        if (e == null) return "系统内部错误";
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            String friendly = translate(msg);
            if (friendly != null) return friendly;
        }
        // 遍历 cause 链尝试匹配
        Throwable cause = e.getCause();
        while (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null && !causeMsg.isBlank()) {
                String friendly = translate(causeMsg);
                if (friendly != null) return friendly;
            }
            cause = cause.getCause();
        }
        // 无法匹配时：去掉 Java 异常堆栈，只保留关键信息
        return stripStackTrace(msg);
    }

    /** 将原始错误消息转为用户可读的提示，匹配不到返回 null */
    public static String translate(String rawMsg) {
        if (rawMsg == null || rawMsg.isBlank()) return null;
        String lower = rawMsg.toLowerCase();

        // 文件已存在
        if (lower.contains("file already exists")
                || lower.contains("文件已存在")
                || lower.contains("eexist")) {
            return "文件已存在，跳过";
        }
        // MD5 校验失败
        if (lower.contains("md5 校验不匹配")
                || lower.contains("md5 mismatch")
                || lower.contains("md5校验不匹配")) {
            return "文件校验失败";
        }
        // 连接被拒绝
        if (lower.contains("connection refused")
                || lower.contains("连接被拒绝")) {
            return "连接存储源失败";
        }
        // 超时
        if (lower.contains("timeout")
                || lower.contains("超时")) {
            return "连接超时";
        }
        // 401 认证失败
        if (lower.contains("401") || lower.contains("unauthorized")) {
            return "存储源认证失败，请检查密钥";
        }
        // 403 无权限
        if (lower.contains("403") || lower.contains("forbidden")) {
            return "无访问权限";
        }
        // 404 文件不存在
        if (lower.contains("404") || lower.contains("not found")) {
            return "文件不存在";
        }
        // 磁盘空间不足
        if (lower.contains("disk full")
                || lower.contains("磁盘空间不足")
                || lower.contains("no space left on device")) {
            return "磁盘空间不足，无法存储";
        }
        // 权限不足
        if (lower.contains("permission denied")) {
            return "文件写入权限不足";
        }
        // 网络异常
        if (lower.contains("network error")) {
            return "网络连接异常";
        }
        // 空指针
        if (lower.contains("null")
                || lower.contains("nullpointerexception")) {
            return "系统内部错误";
        }
        return null;
    }

    /** 去掉 Java 异常堆栈，只保留关键信息 */
    private static String stripStackTrace(String msg) {
        if (msg == null) return "系统内部错误";
        // 截断异常堆栈（以 "\n\tat " 开头的行为堆栈）
        int stackIdx = msg.indexOf("\n\tat ");
        if (stackIdx > 0) {
            msg = msg.substring(0, stackIdx);
        }
        // 去除常见的 Java 异常类名前缀，但保留有用的消息
        msg = msg.replaceAll("^java\\.\\w+\\.\\w+Exception:\\s*", "");
        msg = msg.replaceAll("^java\\.\\w+\\.\\w+Error:\\s*", "");
        if (msg.isBlank()) {
            msg = "系统内部错误";
        }
        return msg;
    }
}
