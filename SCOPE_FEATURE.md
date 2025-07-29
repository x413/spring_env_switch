# 配置作用域功能详解

## 功能概述

配置作用域功能允许您选择配置修改的影响范围：
- **全局配置 (Global)**: 修改全局配置实例，影响所有线程
- **临时配置 (Temporary)**: 只影响当前请求线程，不修改全局配置

## 使用场景

### 全局配置适用场景
- 生产环境的永久性配置切换
- 系统维护时的环境切换
- 部署时的配置更新

### 临时配置适用场景
- 单个请求的特殊配置需求
- 测试和调试场景
- A/B测试或灰度发布
- 特定用户的个性化配置

## API 使用方法

### 1. 全局配置切换

```bash
# 方式1：默认全局作用域
curl -X POST http://localhost:8080/api/config/environment/prod

# 方式2：显式指定全局作用域
curl -X POST http://localhost:8080/api/config/environment/prod/global
```

**效果**: 修改全局 `AppConfig` 实例，所有线程都会获得新配置

### 2. 临时配置切换

```bash
curl -X POST http://localhost:8080/api/config/environment/test/temporary
```

**效果**: 只在当前请求线程设置临时配置，不影响其他线程

### 3. 清除临时配置

```bash
curl -X DELETE http://localhost:8080/api/config/temporary
```

### 4. 查看临时配置统计

```bash
curl http://localhost:8080/api/config/temporary/statistics
```

## 技术实现

### 1. 配置作用域枚举

```java
public enum ConfigScope {
    TEMPORARY("temporary", "临时修改"),
    GLOBAL("global", "全局修改");
}
```

### 2. 请求级配置管理

```java
@Component
public class RequestScopedConfigManager {
    private final ThreadLocal<AppConfig> temporaryConfigHolder = new ThreadLocal<>();
    
    public void setTemporaryConfig(AppConfig tempConfig, String environment) {
        temporaryConfigHolder.set(tempConfig);
    }
    
    public AppConfig getTemporaryConfig() {
        return temporaryConfigHolder.get();
    }
}
```

### 3. 配置代理

```java
@Component
public class ConfigProxy {
    public AppConfig getCurrentConfig() {
        AppConfig tempConfig = requestScopedConfigManager.getTemporaryConfig();
        return tempConfig != null ? tempConfig : globalConfig;
    }
}
```

## 使用示例

### 示例1：业务服务中使用配置代理

```java
@Service
public class BusinessService {
    @Autowired
    private ConfigProxy configProxy;
    
    public void doSomething() {
        // 自动获取当前有效的配置（临时配置优先）
        String dbUrl = configProxy.getDatabase().getUrl();
        boolean cacheEnabled = configProxy.getFeature().isEnableCache();
        
        // 业务逻辑...
    }
}
```

### 示例2：测试场景

```java
@Test
public void testWithDifferentConfig() {
    // 设置临时配置进行测试
    configManager.switchEnvironment("test", ConfigScope.TEMPORARY);
    
    // 执行测试逻辑
    String result = businessService.doSomething();
    
    // 测试完成后自动清理（或手动清理）
    configManager.clearTemporaryConfig();
}
```

### 示例3：多线程环境

```java
// 线程1：使用全局配置
CompletableFuture.runAsync(() -> {
    // 使用全局配置
    String url = configProxy.getDatabase().getUrl(); // 全局配置的URL
});

// 线程2：使用临时配置
CompletableFuture.runAsync(() -> {
    configManager.switchEnvironment("test", ConfigScope.TEMPORARY);
    String url = configProxy.getDatabase().getUrl(); // 临时配置的URL
    configManager.clearTemporaryConfig();
});
```

## 测试接口

### 1. 作用域对比测试

```bash
curl -X POST http://localhost:8080/api/scope-test/compare-scopes/prod
```

**功能**: 依次测试临时配置和全局配置，对比两者的区别

### 2. 多线程隔离测试

```bash
curl -X POST http://localhost:8080/api/scope-test/multi-thread-test
```

**功能**: 启动多个线程，每个线程设置不同的临时配置，验证线程间的配置隔离

### 3. 配置代理测试

```bash
curl http://localhost:8080/api/scope-test/proxy-test
```

**功能**: 测试配置代理的智能选择机制

## 最佳实践

### 1. 临时配置的生命周期管理

```java
try {
    // 设置临时配置
    configManager.switchEnvironment("test", ConfigScope.TEMPORARY);
    
    // 执行业务逻辑
    doBusinessLogic();
    
} finally {
    // 确保清理临时配置
    configManager.clearTemporaryConfig();
}
```

### 2. 配置超时清理

```java
// 定期清理超时的临时配置（5分钟超时）
@Scheduled(fixedRate = 60000) // 每分钟执行一次
public void cleanupExpiredConfigs() {
    int cleaned = configManager.cleanupExpiredTemporaryConfigs(300000); // 5分钟
    if (cleaned > 0) {
        logger.info("清理了 {} 个超时的临时配置", cleaned);
    }
}
```

### 3. 监听配置变更事件

```java
@EventListener
public void handleConfigChange(EnvironmentChangeEvent event) {
    if (event.isTemporary()) {
        logger.info("临时配置切换: {} -> {} (线程: {})", 
                   event.getOldEnvironment(), 
                   event.getNewEnvironment(),
                   Thread.currentThread().getId());
    } else {
        logger.info("全局配置切换: {} -> {}", 
                   event.getOldEnvironment(), 
                   event.getNewEnvironment());
    }
}
```

## 注意事项

1. **内存管理**: 临时配置存储在ThreadLocal中，请及时清理避免内存泄漏
2. **线程安全**: 全局配置的修改是线程安全的，但临时配置只在当前线程有效
3. **性能考虑**: 临时配置的创建有一定开销，不建议频繁使用
4. **调试建议**: 使用统计接口监控临时配置的使用情况

## 故障排查

### 1. 检查当前配置状态

```bash
curl http://localhost:8080/api/config/health
```

### 2. 查看临时配置统计

```bash
curl http://localhost:8080/api/config/temporary/statistics
```

### 3. 强制清理所有临时配置

```bash
curl -X POST "http://localhost:8080/api/config/temporary/cleanup?timeoutMs=0"
```

这个配置作用域功能为您的应用提供了更灵活、更精确的配置管理能力！
