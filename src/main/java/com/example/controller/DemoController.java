package com.example.controller;

import com.example.service.ConfigDemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示控制器
 * 展示如何在实际业务中使用动态配置
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final ConfigDemoService configDemoService;

    @Autowired
    public DemoController(ConfigDemoService configDemoService) {
        this.configDemoService = configDemoService;
    }

    /**
     * 演示数据库连接
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        String result = configDemoService.connectToDatabase();
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "database_connection");
        response.put("result", result);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 演示Redis连接
     */
    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> testRedis() {
        String result = configDemoService.connectToRedis();
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "redis_connection");
        response.put("result", result);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 演示API调用
     */
    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> testApi() {
        String result = configDemoService.callExternalApi();
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "external_api_call");
        response.put("result", result);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 演示功能开关
     */
    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> testFeatures() {
        String result = configDemoService.checkFeatureFlags();
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "feature_flags_check");
        response.put("result", result);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 演示通知配置
     */
    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> testNotifications() {
        String result = configDemoService.checkNotificationConfig();
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "notification_config_check");
        response.put("result", result);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取完整的配置演示
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getConfigSummary() {
        String result = configDemoService.getConfigSummary();
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "config_summary");
        response.put("result", result);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
