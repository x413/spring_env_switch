package com.example;

import com.example.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot 应用启动类
 */
@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
public class Application implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private AppConfig appConfig;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== Spring 环境动态切换应用启动成功 ===");
        logger.info("当前配置信息: {}", appConfig.toString());
        logger.info("访问 http://localhost:8080/api/config/current 查看当前配置");
        logger.info("访问 http://localhost:8080/api/config/environment 查看当前环境");
        logger.info("使用 POST http://localhost:8080/api/config/environment/{env} 切换环境");
        logger.info("支持的环境: dev, prod, test");
        logger.info("=======================================");
    }
}
