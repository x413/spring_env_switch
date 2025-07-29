package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置实体类
 * 使用@ConfigurationProperties自动绑定配置属性
 * 支持运行时动态更新配置实例
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private DatabaseConfig database = new DatabaseConfig();
    private RedisConfig redis = new RedisConfig();
    private ApiConfig api = new ApiConfig();
    private FeatureConfig feature = new FeatureConfig();
    private NotificationConfig notification = new NotificationConfig();

    // Getters and Setters
    public DatabaseConfig getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseConfig database) {
        this.database = database;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public ApiConfig getApi() {
        return api;
    }

    public void setApi(ApiConfig api) {
        this.api = api;
    }

    public FeatureConfig getFeature() {
        return feature;
    }

    public void setFeature(FeatureConfig feature) {
        this.feature = feature;
    }

    public NotificationConfig getNotification() {
        return notification;
    }

    public void setNotification(NotificationConfig notification) {
        this.notification = notification;
    }

    /**
     * 数据库配置
     */
    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;
        private PoolConfig pool = new PoolConfig();

        // Getters and Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public PoolConfig getPool() {
            return pool;
        }

        public void setPool(PoolConfig pool) {
            this.pool = pool;
        }

        public static class PoolConfig {
            private int maxSize;

            public int getMaxSize() {
                return maxSize;
            }

            public void setMaxSize(int maxSize) {
                this.maxSize = maxSize;
            }
        }
    }

    /**
     * Redis配置
     */
    public static class RedisConfig {
        private String host;
        private int port;
        private int database;

        // Getters and Setters
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }
    }

    /**
     * API配置
     */
    public static class ApiConfig {
        private String baseUrl;
        private int timeout;
        private int retryCount;

        // Getters and Setters
        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }
    }

    /**
     * 功能开关配置
     */
    public static class FeatureConfig {
        private boolean enableCache;
        private boolean enableDebug;
        private boolean enableMonitoring;

        // Getters and Setters
        public boolean isEnableCache() {
            return enableCache;
        }

        public void setEnableCache(boolean enableCache) {
            this.enableCache = enableCache;
        }

        public boolean isEnableDebug() {
            return enableDebug;
        }

        public void setEnableDebug(boolean enableDebug) {
            this.enableDebug = enableDebug;
        }

        public boolean isEnableMonitoring() {
            return enableMonitoring;
        }

        public void setEnableMonitoring(boolean enableMonitoring) {
            this.enableMonitoring = enableMonitoring;
        }
    }

    /**
     * 通知配置
     */
    public static class NotificationConfig {
        private EmailConfig email = new EmailConfig();
        private SmsConfig sms = new SmsConfig();

        public EmailConfig getEmail() {
            return email;
        }

        public void setEmail(EmailConfig email) {
            this.email = email;
        }

        public SmsConfig getSms() {
            return sms;
        }

        public void setSms(SmsConfig sms) {
            this.sms = sms;
        }

        public static class EmailConfig {
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        public static class SmsConfig {
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    @Override
    public String toString() {
        return "AppConfig{" +
                "database=" + database.getUrl() +
                ", redis=" + redis.getHost() + ":" + redis.getPort() +
                ", api=" + api.getBaseUrl() +
                ", feature.cache=" + feature.isEnableCache() +
                ", feature.debug=" + feature.isEnableDebug() +
                ", feature.monitoring=" + feature.isEnableMonitoring() +
                '}';
    }
}
