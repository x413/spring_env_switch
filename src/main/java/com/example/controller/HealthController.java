package com.example.controller;

import com.example.config.DynamicConfigManager;
import com.example.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 用于验证系统是否正常工作
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DynamicConfigManager configManager;

    @Autowired
    public HealthController(DynamicConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 基本健康检查
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 测试基本功能
            String currentEnv = configManager.getCurrentEnvironment();
            AppConfig config = configManager.getCurrentConfig();
            boolean hasTemp = configManager.hasTemporaryConfig();
            
            response.put("status", "UP");
            response.put("currentEnvironment", currentEnv);
            response.put("hasTemporaryConfig", hasTemp);
            response.put("databaseUrl", config.getDatabase().getUrl());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 详细检查
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            AppConfig config = configManager.getCurrentConfig();
            
            // 基本信息
            response.put("status", "UP");
            response.put("currentEnvironment", configManager.getCurrentEnvironment());
            response.put("supportedEnvironments", configManager.getSupportedEnvironments());
            response.put("hasTemporaryConfig", configManager.hasTemporaryConfig());
            response.put("temporaryStats", configManager.getTemporaryConfigStatistics());
            
            // 配置详情
            Map<String, Object> configDetails = new HashMap<>();
            configDetails.put("database", Map.of(
                "url", config.getDatabase().getUrl(),
                "username", config.getDatabase().getUsername(),
                "maxPoolSize", config.getDatabase().getPool().getMaxSize()
            ));
            configDetails.put("redis", Map.of(
                "host", config.getRedis().getHost(),
                "port", config.getRedis().getPort(),
                "database", config.getRedis().getDatabase()
            ));
            configDetails.put("api", Map.of(
                "baseUrl", config.getApi().getBaseUrl(),
                "timeout", config.getApi().getTimeout(),
                "retryCount", config.getApi().getRetryCount()
            ));
            configDetails.put("features", Map.of(
                "enableCache", config.getFeature().isEnableCache(),
                "enableDebug", config.getFeature().isEnableDebug(),
                "enableMonitoring", config.getFeature().isEnableMonitoring()
            ));
            
            response.put("config", configDetails);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            response.put("stackTrace", e.getStackTrace());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
