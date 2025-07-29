package com.example;

import com.example.config.AppConfig;
import com.example.config.DynamicConfigManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应用测试类
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class ApplicationTest {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private DynamicConfigManager configManager;

    @Test
    void contextLoads() {
        assertNotNull(appConfig);
        assertNotNull(configManager);
    }

    @Test
    void testConfigLoading() {
        // 测试配置是否正确加载
        assertNotNull(appConfig.getDatabase());
        assertNotNull(appConfig.getRedis());
        assertNotNull(appConfig.getApi());
        assertNotNull(appConfig.getFeature());
        assertNotNull(appConfig.getNotification());
    }

    @Test
    void testEnvironmentSwitch() {
        // 测试环境切换
        String originalEnv = configManager.getCurrentEnvironment();
        
        // 切换到测试环境
        boolean success = configManager.switchEnvironment("test");
        assertTrue(success);
        assertEquals("test", configManager.getCurrentEnvironment());
        
        // 切换回原环境
        success = configManager.switchEnvironment(originalEnv);
        assertTrue(success);
        assertEquals(originalEnv, configManager.getCurrentEnvironment());
    }

    @Test
    void testSupportedEnvironments() {
        var supportedEnvs = configManager.getSupportedEnvironments();
        assertTrue(supportedEnvs.contains("dev"));
        assertTrue(supportedEnvs.contains("prod"));
        assertTrue(supportedEnvs.contains("test"));
    }
}
