package com.example.controller;

import com.example.config.AppConfig;
import com.example.config.DynamicConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 优化后的配置测试控制器
 * 验证直接刷新配置实例的效果
 */
@RestController
@RequestMapping("/api/test")
public class OptimizedConfigTestController {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedConfigTestController.class);

    private final DynamicConfigManager configManager;
    private final AppConfig appConfig;

    @Autowired
    public OptimizedConfigTestController(DynamicConfigManager configManager, AppConfig appConfig) {
        this.configManager = configManager;
        this.appConfig = appConfig;
    }

    /**
     * 测试配置切换前后的值变化
     */
    @PostMapping("/switch-and-compare/{targetEnv}")
    public ResponseEntity<Map<String, Object>> switchAndCompare(@PathVariable String targetEnv) {
        logger.info("开始测试配置切换: 目标环境 = {}", targetEnv);
        
        // 记录切换前的配置
        Map<String, Object> beforeConfig = captureCurrentConfig();
        String beforeEnv = configManager.getCurrentEnvironment();
        
        // 执行环境切换
        boolean success = configManager.switchEnvironment(targetEnv);
        
        // 记录切换后的配置
        Map<String, Object> afterConfig = captureCurrentConfig();
        String afterEnv = configManager.getCurrentEnvironment();
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("beforeEnvironment", beforeEnv);
        response.put("afterEnvironment", afterEnv);
        response.put("beforeConfig", beforeConfig);
        response.put("afterConfig", afterConfig);
        response.put("configChanged", !beforeConfig.equals(afterConfig));
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("配置切换测试完成: {} -> {}, 成功: {}", beforeEnv, afterEnv, success);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 验证配置实例引用的一致性
     */
    @GetMapping("/verify-instance")
    public ResponseEntity<Map<String, Object>> verifyInstance() {
        Map<String, Object> response = new HashMap<>();
        
        // 获取配置实例的hashCode来验证是否是同一个实例
        int configHashCode = System.identityHashCode(appConfig);
        
        AppConfig currentConfig = configManager.getCurrentConfig();
        response.put("configInstanceHashCode", configHashCode);
        response.put("currentEnvironment", configManager.getCurrentEnvironment());
        response.put("databaseUrl", currentConfig.getDatabase().getUrl());
        response.put("redisHost", currentConfig.getRedis().getHost());
        response.put("apiBaseUrl", currentConfig.getApi().getBaseUrl());
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("配置实例验证: HashCode = {}, 环境 = {}", configHashCode, configManager.getCurrentEnvironment());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 连续切换测试
     */
    @PostMapping("/continuous-switch")
    public ResponseEntity<Map<String, Object>> continuousSwitch() {
        logger.info("开始连续切换测试");
        
        String originalEnv = configManager.getCurrentEnvironment();
        Map<String, Object> results = new HashMap<>();
        
        // 测试切换序列: dev -> prod -> test -> dev
        String[] switchSequence = {"prod", "test", "dev"};
        
        for (String targetEnv : switchSequence) {
            long startTime = System.currentTimeMillis();
            boolean success = configManager.switchEnvironment(targetEnv);
            long endTime = System.currentTimeMillis();
            
            Map<String, Object> switchResult = new HashMap<>();
            switchResult.put("success", success);
            switchResult.put("duration", endTime - startTime);
            switchResult.put("currentConfig", captureCurrentConfig());
            
            results.put("switch_to_" + targetEnv, switchResult);
            
            logger.info("切换到 {} 环境: 成功={}, 耗时={}ms", targetEnv, success, endTime - startTime);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalEnvironment", originalEnv);
        response.put("finalEnvironment", configManager.getCurrentEnvironment());
        response.put("switchResults", results);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("连续切换测试完成");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 捕获当前配置状态
     */
    private Map<String, Object> captureCurrentConfig() {
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

        // 功能开关
        Map<String, Object> feature = new HashMap<>();
        feature.put("enableCache", currentConfig.getFeature().isEnableCache());
        feature.put("enableDebug", currentConfig.getFeature().isEnableDebug());
        feature.put("enableMonitoring", currentConfig.getFeature().isEnableMonitoring());
        config.put("feature", feature);

        return config;
    }
}
