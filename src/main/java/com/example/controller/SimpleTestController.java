package com.example.controller;

import com.example.config.ConfigScope;
import com.example.config.DynamicConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单的配置测试控制器
 * 演示临时配置和全局配置的基本功能
 */
@RestController
@RequestMapping("/api/simple-test")
public class SimpleTestController {

    private static final Logger logger = LoggerFactory.getLogger(SimpleTestController.class);

    private final DynamicConfigManager configManager;

    @Autowired
    public SimpleTestController(DynamicConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 测试临时配置和全局配置的区别
     */
    @PostMapping("/compare/{targetEnv}")
    public ResponseEntity<Map<String, Object>> compareConfigs(@PathVariable String targetEnv) {
        logger.info("开始对比测试: 目标环境 = {}", targetEnv);
        
        Map<String, Object> response = new HashMap<>();
        
        // 记录初始状态
        String initialEnv = configManager.getCurrentEnvironment();
        String initialDbUrl = configManager.getCurrentConfig().getDatabase().getUrl();
        
        // 测试临时配置
        configManager.switchEnvironment(targetEnv, ConfigScope.TEMPORARY);
        String tempDbUrl = configManager.getCurrentConfig().getDatabase().getUrl();
        boolean hasTemp = configManager.hasTemporaryConfig();
        
        // 清除临时配置
        configManager.clearTemporaryConfig();
        String afterClearDbUrl = configManager.getCurrentConfig().getDatabase().getUrl();
        
        // 测试全局配置
        configManager.switchEnvironment(targetEnv, ConfigScope.GLOBAL);
        String globalDbUrl = configManager.getCurrentConfig().getDatabase().getUrl();
        String currentEnv = configManager.getCurrentEnvironment();
        
        response.put("initialEnvironment", initialEnv);
        response.put("targetEnvironment", targetEnv);
        response.put("initialDbUrl", initialDbUrl);
        response.put("temporaryDbUrl", tempDbUrl);
        response.put("hadTemporaryConfig", hasTemp);
        response.put("afterClearDbUrl", afterClearDbUrl);
        response.put("globalDbUrl", globalDbUrl);
        response.put("finalEnvironment", currentEnv);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("对比测试完成");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前配置状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("globalEnvironment", configManager.getCurrentEnvironment());
        response.put("hasTemporaryConfig", configManager.hasTemporaryConfig());
        response.put("threadId", Thread.currentThread().getId());
        response.put("threadName", Thread.currentThread().getName());
        
        // 当前有效配置
        var currentConfig = configManager.getCurrentConfig();
        Map<String, Object> config = new HashMap<>();
        config.put("databaseUrl", currentConfig.getDatabase().getUrl());
        config.put("redisHost", currentConfig.getRedis().getHost());
        config.put("apiBaseUrl", currentConfig.getApi().getBaseUrl());
        config.put("enableCache", currentConfig.getFeature().isEnableCache());
        config.put("enableDebug", currentConfig.getFeature().isEnableDebug());
        
        response.put("currentConfig", config);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 快速切换测试
     */
    @PostMapping("/quick-switch")
    public ResponseEntity<Map<String, Object>> quickSwitch() {
        Map<String, Object> response = new HashMap<>();
        
        // 记录开始状态
        String startEnv = configManager.getCurrentEnvironment();
        
        // 快速切换序列
        String[] envs = {"dev", "prod", "test"};
        Map<String, Long> switchTimes = new HashMap<>();
        
        for (String env : envs) {
            long start = System.currentTimeMillis();
            configManager.switchEnvironment(env, ConfigScope.GLOBAL);
            long end = System.currentTimeMillis();
            switchTimes.put(env, end - start);
        }
        
        response.put("startEnvironment", startEnv);
        response.put("finalEnvironment", configManager.getCurrentEnvironment());
        response.put("switchTimes", switchTimes);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
