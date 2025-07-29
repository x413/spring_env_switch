package com.example.service;

import com.example.config.DynamicConfigManager;
import com.example.config.AppConfig;
import com.example.config.EnvironmentChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;



/**
 * 配置演示服务
 * 展示如何在业务代码中使用动态配置
 */
@Service
public class ConfigDemoService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigDemoService.class);

    private final DynamicConfigManager configManager;

    @Autowired
    public ConfigDemoService(DynamicConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 模拟数据库连接
     */
    public String connectToDatabase() {
        AppConfig config = configManager.getCurrentConfig();
        String url = config.getDatabase().getUrl();
        String username = config.getDatabase().getUsername();
        int maxPoolSize = config.getDatabase().getPool().getMaxSize();

        logger.info("连接数据库: URL={}, User={}, MaxPool={}", url, username, maxPoolSize);
        return String.format("已连接到数据库: %s (用户: %s, 最大连接数: %d)", url, username, maxPoolSize);
    }

    /**
     * 模拟Redis连接
     */
    public String connectToRedis() {
        AppConfig config = configManager.getCurrentConfig();
        String host = config.getRedis().getHost();
        int port = config.getRedis().getPort();
        int database = config.getRedis().getDatabase();

        logger.info("连接Redis: {}:{}, DB={}", host, port, database);
        return String.format("已连接到Redis: %s:%d (数据库: %d)", host, port, database);
    }

    /**
     * 模拟API调用
     */
    public String callExternalApi() {
        AppConfig config = configManager.getCurrentConfig();
        String baseUrl = config.getApi().getBaseUrl();
        int timeout = config.getApi().getTimeout();
        int retryCount = config.getApi().getRetryCount();

        logger.info("调用外部API: URL={}, Timeout={}ms, Retry={}", baseUrl, timeout, retryCount);
        return String.format("调用API: %s (超时: %dms, 重试: %d次)", baseUrl, timeout, retryCount);
    }

    /**
     * 检查功能开关
     */
    public String checkFeatureFlags() {
        AppConfig config = configManager.getCurrentConfig();
        boolean cacheEnabled = config.getFeature().isEnableCache();
        boolean debugEnabled = config.getFeature().isEnableDebug();
        boolean monitoringEnabled = config.getFeature().isEnableMonitoring();

        logger.info("功能开关状态: Cache={}, Debug={}, Monitoring={}",
                   cacheEnabled, debugEnabled, monitoringEnabled);

        return String.format("功能开关 - 缓存: %s, 调试: %s, 监控: %s",
                           cacheEnabled ? "开启" : "关闭",
                           debugEnabled ? "开启" : "关闭",
                           monitoringEnabled ? "开启" : "关闭");
    }

    /**
     * 检查通知配置
     */
    public String checkNotificationConfig() {
        AppConfig config = configManager.getCurrentConfig();
        boolean emailEnabled = config.getNotification().getEmail().isEnabled();
        boolean smsEnabled = config.getNotification().getSms().isEnabled();

        logger.info("通知配置: Email={}, SMS={}", emailEnabled, smsEnabled);
        return String.format("通知配置 - 邮件: %s, 短信: %s",
                           emailEnabled ? "启用" : "禁用",
                           smsEnabled ? "启用" : "禁用");
    }

    /**
     * 获取完整的配置摘要
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== 当前配置摘要 ===\n");
        summary.append(connectToDatabase()).append("\n");
        summary.append(connectToRedis()).append("\n");
        summary.append(callExternalApi()).append("\n");
        summary.append(checkFeatureFlags()).append("\n");
        summary.append(checkNotificationConfig()).append("\n");
        summary.append("==================");
        
        return summary.toString();
    }

    /**
     * 监听环境切换事件，执行相应的业务逻辑
     */
    @EventListener
    public void handleEnvironmentChange(EnvironmentChangeEvent event) {
        logger.info("检测到环境切换: {} -> {}", event.getOldEnvironment(), event.getNewEnvironment());
        
        // 根据新环境执行相应的业务逻辑
        String newEnv = event.getNewEnvironment();
        switch (newEnv) {
            case "prod":
                handleProductionEnvironment();
                break;
            case "dev":
                handleDevelopmentEnvironment();
                break;
            case "test":
                handleTestEnvironment();
                break;
            default:
                logger.warn("未知环境: {}", newEnv);
        }
        
        // 输出新配置摘要
        logger.info("新环境配置:\n{}", getConfigSummary());
    }

    private void handleProductionEnvironment() {
        logger.info("切换到生产环境 - 启用所有监控和通知功能");
        AppConfig config = configManager.getCurrentConfig();
        // 生产环境特有的初始化逻辑
        if (config.getFeature().isEnableMonitoring()) {
            logger.info("启动生产环境监控");
        }
        if (config.getNotification().getEmail().isEnabled()) {
            logger.info("启用邮件通知");
        }
    }

    private void handleDevelopmentEnvironment() {
        logger.info("切换到开发环境 - 启用调试模式");
        AppConfig config = configManager.getCurrentConfig();
        // 开发环境特有的初始化逻辑
        if (config.getFeature().isEnableDebug()) {
            logger.info("启用调试模式");
        }
    }

    private void handleTestEnvironment() {
        logger.info("切换到测试环境 - 使用内存数据库和模拟服务");
        AppConfig config = configManager.getCurrentConfig();
        // 测试环境特有的初始化逻辑
        if (config.getDatabase().getUrl().contains("h2:mem")) {
            logger.info("使用内存数据库进行测试");
        }
    }
}
