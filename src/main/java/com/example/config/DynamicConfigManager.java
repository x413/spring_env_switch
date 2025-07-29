package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 动态配置管理器
 * 支持全局配置和临时配置（线程级）的简单实现
 */
@Component
public class DynamicConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfigManager.class);

    private static final String DYNAMIC_CONFIG_SOURCE_NAME = "dynamicConfigSource";
    private static final Set<String> SUPPORTED_ENVIRONMENTS = Set.of("dev", "prod", "test");

    private final ConfigurableEnvironment environment;
    private final ApplicationEventPublisher eventPublisher;
    private final AppConfig appConfig;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // 简单的ThreadLocal存储临时配置
    private final ThreadLocal<AppConfig> temporaryConfig = new ThreadLocal<>();

    private volatile String currentEnvironment = "dev";

    @Autowired
    public DynamicConfigManager(ConfigurableEnvironment environment,
                               ApplicationEventPublisher eventPublisher,
                               AppConfig appConfig) {
        this.environment = environment;
        this.eventPublisher = eventPublisher;
        this.appConfig = appConfig;

        // 初始化时加载默认环境配置
        initializeDefaultConfig();
    }

    /**
     * 初始化默认配置
     */
    private void initializeDefaultConfig() {
        try {
            switchEnvironment(currentEnvironment, ConfigScope.GLOBAL);
            logger.info("初始化默认环境配置: {}", currentEnvironment);
        } catch (Exception e) {
            logger.error("初始化默认配置失败", e);
        }
    }

    /**
     * 切换环境配置（默认全局作用域）
     *
     * @param targetEnvironment 目标环境
     * @return 切换是否成功
     */
    public boolean switchEnvironment(String targetEnvironment) {
        return switchEnvironment(targetEnvironment, ConfigScope.GLOBAL);
    }

    /**
     * 切换环境配置（指定作用域）
     *
     * @param targetEnvironment 目标环境
     * @param scope 配置作用域
     * @return 切换是否成功
     */
    public boolean switchEnvironment(String targetEnvironment, ConfigScope scope) {
        if (!SUPPORTED_ENVIRONMENTS.contains(targetEnvironment)) {
            logger.warn("不支持的环境: {}，支持的环境: {}", targetEnvironment, SUPPORTED_ENVIRONMENTS);
            return false;
        }

        // 对于临时配置，不需要检查是否与当前环境相同
        if (scope.isGlobal() && targetEnvironment.equals(currentEnvironment)) {
            logger.info("当前已经是目标环境: {}", targetEnvironment);
            return true;
        }

        try {
            logger.info("开始切换环境: {} -> {} (作用域: {})", currentEnvironment, targetEnvironment, scope);

            // 加载新的配置文件
            Properties newProperties = loadConfigProperties(targetEnvironment);
            if (newProperties == null) {
                logger.error("加载配置文件失败: config-{}.properties", targetEnvironment);
                return false;
            }

            if (scope.isGlobal()) {
                // 全局修改：需要加锁
                lock.writeLock().lock();
                try {
                    return performGlobalSwitch(targetEnvironment, newProperties);
                } finally {
                    lock.writeLock().unlock();
                }
            } else {
                // 临时修改：不需要加锁，只影响当前线程
                return performTemporarySwitch(targetEnvironment, newProperties);
            }

        } catch (Exception e) {
            logger.error("环境切换失败: {} -> {} (作用域: {})", currentEnvironment, targetEnvironment, scope, e);
            return false;
        }
    }

    /**
     * 执行全局配置切换
     */
    private boolean performGlobalSwitch(String targetEnvironment, Properties newProperties) {
        try {
            // 移除旧的动态配置源
            removeDynamicConfigSource();

            // 添加新的配置源
            addDynamicConfigSource(newProperties);

            // 直接刷新配置实例
            refreshConfigInstance();

            // 更新当前环境
            String oldEnvironment = currentEnvironment;
            currentEnvironment = targetEnvironment;

            // 发布环境切换事件
            publishEnvironmentChangeEvent(oldEnvironment, targetEnvironment, ConfigScope.GLOBAL);

            logger.info("全局环境切换成功: {} -> {}", oldEnvironment, targetEnvironment);
            return true;

        } catch (Exception e) {
            logger.error("全局环境切换失败", e);
            throw e;
        }
    }

    /**
     * 执行临时配置切换
     */
    private boolean performTemporarySwitch(String targetEnvironment, Properties newProperties) {
        try {
            // 创建临时配置实例
            AppConfig tempConfig = createTemporaryConfig(newProperties);

            // 设置到当前线程的ThreadLocal
            temporaryConfig.set(tempConfig);

            // 发布临时配置切换事件
            publishEnvironmentChangeEvent(currentEnvironment, targetEnvironment, ConfigScope.TEMPORARY);

            logger.info("临时环境切换成功: {} -> {} (线程: {})",
                       currentEnvironment, targetEnvironment, Thread.currentThread().getId());
            return true;

        } catch (Exception e) {
            logger.error("临时环境切换失败", e);
            throw e;
        }
    }

    /**
     * 创建临时配置实例
     */
    private AppConfig createTemporaryConfig(Properties properties) {
        // 临时添加配置源
        PropertiesPropertySource tempSource = new PropertiesPropertySource("tempConfigSource", properties);
        environment.getPropertySources().addFirst(tempSource);

        try {
            // 使用Binder创建新的配置实例
            Binder binder = Binder.get(environment);
            return binder.bind("app", AppConfig.class)
                        .orElseThrow(() -> new RuntimeException("无法绑定临时配置"));
        } finally {
            // 移除临时配置源
            environment.getPropertySources().remove("tempConfigSource");
        }
    }

    /**
     * 加载配置文件属性
     */
    private Properties loadConfigProperties(String env) {
        String configFileName = "config-" + env + ".properties";
        ClassPathResource resource = new ClassPathResource(configFileName);
        
        if (!resource.exists()) {
            logger.error("配置文件不存在: {}", configFileName);
            return null;
        }

        Properties properties = new Properties();
        try {
            properties.load(resource.getInputStream());
            logger.debug("成功加载配置文件: {}，包含 {} 个配置项", configFileName, properties.size());
            return properties;
        } catch (IOException e) {
            logger.error("读取配置文件失败: {}", configFileName, e);
            return null;
        }
    }

    /**
     * 移除动态配置源
     */
    private void removeDynamicConfigSource() {
        if (environment.getPropertySources().contains(DYNAMIC_CONFIG_SOURCE_NAME)) {
            environment.getPropertySources().remove(DYNAMIC_CONFIG_SOURCE_NAME);
            logger.debug("移除旧的动态配置源");
        }
    }

    /**
     * 添加动态配置源
     */
    private void addDynamicConfigSource(Properties properties) {
        PropertiesPropertySource propertySource = new PropertiesPropertySource(
            DYNAMIC_CONFIG_SOURCE_NAME, properties);
        
        // 将动态配置源添加到最高优先级
        environment.getPropertySources().addFirst(propertySource);
        logger.debug("添加新的动态配置源，优先级最高");
    }

    /**
     * 直接刷新配置实例
     * 使用Binder重新绑定配置属性到现有的AppConfig实例
     */
    private void refreshConfigInstance() {
        try {
            // 使用Spring Boot的Binder重新绑定配置
            Binder binder = Binder.get(environment);

            // 重新绑定配置到现有的AppConfig实例
            binder.bind("app", AppConfig.class).ifBound(newConfig -> {
                // 直接更新现有实例的属性
                updateConfigInstance(newConfig);
                logger.debug("成功刷新配置实例，所有引用将自动获得新值");
            });

        } catch (Exception e) {
            logger.error("刷新配置实例失败", e);
            throw new RuntimeException("配置刷新失败", e);
        }
    }

    /**
     * 更新配置实例的所有属性
     */
    private void updateConfigInstance(AppConfig newConfig) {
        // 更新数据库配置
        appConfig.getDatabase().setUrl(newConfig.getDatabase().getUrl());
        appConfig.getDatabase().setUsername(newConfig.getDatabase().getUsername());
        appConfig.getDatabase().setPassword(newConfig.getDatabase().getPassword());
        appConfig.getDatabase().getPool().setMaxSize(newConfig.getDatabase().getPool().getMaxSize());

        // 更新Redis配置
        appConfig.getRedis().setHost(newConfig.getRedis().getHost());
        appConfig.getRedis().setPort(newConfig.getRedis().getPort());
        appConfig.getRedis().setDatabase(newConfig.getRedis().getDatabase());

        // 更新API配置
        appConfig.getApi().setBaseUrl(newConfig.getApi().getBaseUrl());
        appConfig.getApi().setTimeout(newConfig.getApi().getTimeout());
        appConfig.getApi().setRetryCount(newConfig.getApi().getRetryCount());

        // 更新功能开关配置
        appConfig.getFeature().setEnableCache(newConfig.getFeature().isEnableCache());
        appConfig.getFeature().setEnableDebug(newConfig.getFeature().isEnableDebug());
        appConfig.getFeature().setEnableMonitoring(newConfig.getFeature().isEnableMonitoring());

        // 更新通知配置
        appConfig.getNotification().getEmail().setEnabled(newConfig.getNotification().getEmail().isEnabled());
        appConfig.getNotification().getSms().setEnabled(newConfig.getNotification().getSms().isEnabled());

        logger.debug("配置实例属性更新完成");
    }

    /**
     * 发布环境切换事件（默认全局作用域）
     */
    private void publishEnvironmentChangeEvent(String oldEnv, String newEnv) {
        publishEnvironmentChangeEvent(oldEnv, newEnv, ConfigScope.GLOBAL);
    }

    /**
     * 发布环境切换事件（指定作用域）
     */
    private void publishEnvironmentChangeEvent(String oldEnv, String newEnv, ConfigScope scope) {
        EnvironmentChangeEvent event = new EnvironmentChangeEvent(this, oldEnv, newEnv, scope);
        eventPublisher.publishEvent(event);
        logger.debug("发布环境切换事件: {} -> {} (作用域: {})", oldEnv, newEnv, scope);
    }

    /**
     * 获取当前环境
     */
    public String getCurrentEnvironment() {
        lock.readLock().lock();
        try {
            return currentEnvironment;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取支持的环境列表
     */
    public Set<String> getSupportedEnvironments() {
        return SUPPORTED_ENVIRONMENTS;
    }

    /**
     * 获取当前有效的配置（临时配置优先）
     */
    public AppConfig getCurrentConfig() {
        AppConfig tempConfig = temporaryConfig.get();
        return tempConfig != null ? tempConfig : appConfig;
    }

    /**
     * 获取当前环境的配置属性
     */
    public Properties getCurrentProperties() {
        lock.readLock().lock();
        try {
            return loadConfigProperties(currentEnvironment);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清除当前线程的临时配置
     */
    public void clearTemporaryConfig() {
        temporaryConfig.remove();
        logger.info("清除线程 {} 的临时配置", Thread.currentThread().getId());
    }

    /**
     * 检查当前线程是否有临时配置
     */
    public boolean hasTemporaryConfig() {
        return temporaryConfig.get() != null;
    }

    /**
     * 获取临时配置统计信息
     */
    public Map<String, Object> getTemporaryConfigStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("hasTemporaryConfig", hasTemporaryConfig());
        stats.put("threadId", Thread.currentThread().getId());
        stats.put("threadName", Thread.currentThread().getName());
        return stats;
    }
}
