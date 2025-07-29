package com.example.controller;

import com.example.config.AppConfig;
import com.example.config.ConfigScope;
import com.example.config.DynamicConfigManager;
import com.example.config.EnvironmentChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 配置管理控制器
 * 提供REST API进行环境配置的查看和切换
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    private final DynamicConfigManager configManager;
    private final AppConfig appConfig;

    @Autowired
    public ConfigController(DynamicConfigManager configManager, AppConfig appConfig) {
        this.configManager = configManager;
        this.appConfig = appConfig;
    }

    /**
     * 获取当前环境信息
     */
    @GetMapping("/environment")
    public ResponseEntity<Map<String, Object>> getCurrentEnvironment() {
        Map<String, Object> response = new HashMap<>();
        response.put("currentEnvironment", configManager.getCurrentEnvironment());
        response.put("supportedEnvironments", configManager.getSupportedEnvironments());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 切换环境配置（默认全局作用域）
     */
    @PostMapping("/environment/{targetEnv}")
    public ResponseEntity<Map<String, Object>> switchEnvironment(@PathVariable String targetEnv) {
        return switchEnvironmentWithScope(targetEnv, "global");
    }

    /**
     * 切换环境配置（指定作用域）
     */
    @PostMapping("/environment/{targetEnv}/{scope}")
    public ResponseEntity<Map<String, Object>> switchEnvironmentWithScope(
            @PathVariable String targetEnv,
            @PathVariable String scope) {

        logger.info("收到环境切换请求: {} (作用域: {})", targetEnv, scope);

        try {
            ConfigScope configScope = ConfigScope.fromCode(scope);
            boolean success = configManager.switchEnvironment(targetEnv, configScope);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("currentEnvironment", configManager.getCurrentEnvironment());
            response.put("targetEnvironment", targetEnv);
            response.put("scope", configScope.getCode());
            response.put("scopeDescription", configScope.getDescription());
            response.put("hasTemporaryConfig", configManager.hasTemporaryConfig());
            response.put("timestamp", System.currentTimeMillis());

            if (success) {
                response.put("message", String.format("环境切换成功 (%s)", configScope.getDescription()));
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "环境切换失败");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取当前配置详情
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("environment", configManager.getCurrentEnvironment());
        response.put("hasTemporaryConfig", configManager.hasTemporaryConfig());
        response.put("config", buildConfigResponse());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取原始配置属性
     */
    @GetMapping("/properties")
    public ResponseEntity<Map<String, Object>> getCurrentProperties() {
        Properties properties = configManager.getCurrentProperties();
        Map<String, Object> response = new HashMap<>();
        response.put("environment", configManager.getCurrentEnvironment());
        response.put("properties", properties);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * 清除当前线程的临时配置
     */
    @DeleteMapping("/temporary")
    public ResponseEntity<Map<String, Object>> clearTemporaryConfig() {
        boolean hadTemporaryConfig = configManager.hasTemporaryConfig();
        configManager.clearTemporaryConfig();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("hadTemporaryConfig", hadTemporaryConfig);
        response.put("message", hadTemporaryConfig ? "临时配置已清除" : "当前线程没有临时配置");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取临时配置统计信息
     */
    @GetMapping("/temporary/statistics")
    public ResponseEntity<Map<String, Object>> getTemporaryConfigStatistics() {
        Map<String, Object> statistics = configManager.getTemporaryConfigStatistics();
        Map<String, Object> response = new HashMap<>();
        response.put("statistics", statistics);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("environment", configManager.getCurrentEnvironment());
        response.put("hasTemporaryConfig", configManager.hasTemporaryConfig());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * 构建配置响应对象
     */
    private Map<String, Object> buildConfigResponse() {
        // 获取当前有效的配置（临时配置优先）
        AppConfig currentConfig = configManager.getCurrentConfig();

        Map<String, Object> config = new HashMap<>();

        // 数据库配置
        Map<String, Object> database = new HashMap<>();
        database.put("url", currentConfig.getDatabase().getUrl());
        database.put("username", currentConfig.getDatabase().getUsername());
        database.put("maxPoolSize", currentConfig.getDatabase().getPool().getMaxSize());
        config.put("database", database);

        // Redis配置
        Map<String, Object> redis = new HashMap<>();
        redis.put("host", currentConfig.getRedis().getHost());
        redis.put("port", currentConfig.getRedis().getPort());
        redis.put("database", currentConfig.getRedis().getDatabase());
        config.put("redis", redis);

        // API配置
        Map<String, Object> api = new HashMap<>();
        api.put("baseUrl", currentConfig.getApi().getBaseUrl());
        api.put("timeout", currentConfig.getApi().getTimeout());
        api.put("retryCount", currentConfig.getApi().getRetryCount());
        config.put("api", api);

        // 功能开关配置
        Map<String, Object> feature = new HashMap<>();
        feature.put("enableCache", currentConfig.getFeature().isEnableCache());
        feature.put("enableDebug", currentConfig.getFeature().isEnableDebug());
        feature.put("enableMonitoring", currentConfig.getFeature().isEnableMonitoring());
        config.put("feature", feature);

        // 通知配置
        Map<String, Object> notification = new HashMap<>();
        notification.put("emailEnabled", currentConfig.getNotification().getEmail().isEnabled());
        notification.put("smsEnabled", currentConfig.getNotification().getSms().isEnabled());
        config.put("notification", notification);

        return config;
    }

    /**
     * 监听环境切换事件
     */
    @EventListener
    public void handleEnvironmentChangeEvent(EnvironmentChangeEvent event) {
        logger.info("环境切换事件: {} -> {} (作用域: {}), 时间: {}",
                   event.getOldEnvironment(),
                   event.getNewEnvironment(),
                   event.getScope(),
                   event.getEventTimestamp());

        // 这里可以添加环境切换后的后续处理逻辑
        // 例如：清理缓存、重新初始化连接池、发送通知等
        handlePostEnvironmentSwitch(event);
    }

    /**
     * 环境切换后的处理逻辑
     */
    private void handlePostEnvironmentSwitch(EnvironmentChangeEvent event) {
        try {
            // 示例：根据新环境的配置执行相应操作
            String newEnv = event.getNewEnvironment();
            ConfigScope scope = event.getScope();

            if (scope.isTemporary()) {
                logger.info("临时切换到 {} 环境 (线程: {})", newEnv, Thread.currentThread().getId());
                // 临时配置的特殊处理逻辑
            } else {
                logger.info("全局切换到 {} 环境", newEnv);

                if ("prod".equals(newEnv)) {
                    logger.info("启用生产环境监控和通知");
                    // 启用生产环境特有的功能
                } else if ("dev".equals(newEnv)) {
                    logger.info("启用开发环境调试模式");
                    // 启用开发环境特有的功能
                } else if ("test".equals(newEnv)) {
                    logger.info("配置测试环境，禁用外部依赖");
                    // 配置测试环境特有的设置
                }
            }

        } catch (Exception e) {
            logger.error("环境切换后处理失败", e);
        }
    }
}
