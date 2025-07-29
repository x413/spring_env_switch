# 极简配置作用域设计

## 设计理念

**极简原则**: 用最少的代码实现最核心的功能

## 核心类设计

### 1. DynamicConfigManager - 唯一的管理类

```java
@Component
public class DynamicConfigManager {
    // 简单的ThreadLocal存储临时配置
    private final ThreadLocal<AppConfig> temporaryConfig = new ThreadLocal<>();
    
    // 核心方法1: 切换配置
    public boolean switchEnvironment(String env, ConfigScope scope) {
        if (scope.isGlobal()) {
            // 全局切换：更新appConfig实例
        } else {
            // 临时切换：设置ThreadLocal
            temporaryConfig.set(newConfig);
        }
    }
    
    // 核心方法2: 获取当前配置
    public AppConfig getCurrentConfig() {
        AppConfig temp = temporaryConfig.get();
        return temp != null ? temp : appConfig; // 临时配置优先
    }
}
```

### 2. ConfigScope - 简单的枚举

```java
public enum ConfigScope {
    TEMPORARY("temporary", "临时修改"),
    GLOBAL("global", "全局修改");
}
```

## 使用方式

### 业务代码中的使用

```java
@Service
public class BusinessService {
    @Autowired
    private DynamicConfigManager configManager;
    
    public void doSomething() {
        // 自动获取当前有效配置（临时配置优先）
        AppConfig config = configManager.getCurrentConfig();
        String dbUrl = config.getDatabase().getUrl();
        // 业务逻辑...
    }
}
```

### API调用

```bash
# 全局切换
curl -X POST http://localhost:8080/api/config/environment/prod/global

# 临时切换
curl -X POST http://localhost:8080/api/config/environment/test/temporary

# 清除临时配置
curl -X DELETE http://localhost:8080/api/config/temporary
```

## 简化对比

### 简化前（复杂设计）
- `RequestScopedConfigManager` - 复杂的请求级管理
- `ConfigProxy` - 额外的代理层
- `ConfigMetadata` - 复杂的元数据管理
- 多个统计和清理接口

### 简化后（极简设计）
- 只有 `DynamicConfigManager` 一个核心类
- 直接使用 `ThreadLocal<AppConfig>`
- 简单的统计信息
- 核心API只有3个方法

## 核心优势

1. **代码量少**: 核心逻辑不到200行
2. **易理解**: 没有复杂的抽象层
3. **易维护**: 所有逻辑集中在一个类中
4. **性能好**: 没有额外的代理开销
5. **功能完整**: 支持临时配置和全局配置

## 实现细节

### 临时配置的生命周期

```java
// 设置临时配置
configManager.switchEnvironment("test", ConfigScope.TEMPORARY);

// 使用配置
AppConfig config = configManager.getCurrentConfig(); // 返回临时配置

// 清除临时配置
configManager.clearTemporaryConfig();

// 再次使用配置
AppConfig config = configManager.getCurrentConfig(); // 返回全局配置
```

### 线程隔离

```java
// 线程1
CompletableFuture.runAsync(() -> {
    configManager.switchEnvironment("test", ConfigScope.TEMPORARY);
    String url = configManager.getCurrentConfig().getDatabase().getUrl(); // test环境的URL
});

// 线程2
CompletableFuture.runAsync(() -> {
    String url = configManager.getCurrentConfig().getDatabase().getUrl(); // 全局环境的URL
});
```

## 测试验证

### 基本功能测试

```bash
# 对比测试
curl -X POST http://localhost:8080/api/simple-test/compare/prod

# 状态查看
curl http://localhost:8080/api/simple-test/status

# 性能测试
curl -X POST http://localhost:8080/api/simple-test/quick-switch
```

### 预期结果

```json
{
  "initialEnvironment": "dev",
  "targetEnvironment": "prod",
  "initialDbUrl": "jdbc:mysql://localhost:3306/dev_db",
  "temporaryDbUrl": "jdbc:mysql://prod-db:3306/prod_db",
  "hadTemporaryConfig": true,
  "afterClearDbUrl": "jdbc:mysql://localhost:3306/dev_db",
  "globalDbUrl": "jdbc:mysql://prod-db:3306/prod_db",
  "finalEnvironment": "prod"
}
```

## 总结

通过极简设计，我们用最少的代码实现了：
- ✅ 全局配置切换
- ✅ 临时配置切换  
- ✅ 线程隔离
- ✅ 自动选择
- ✅ 事件通知

**核心思想**: 简单就是美，能用一个类解决的问题，就不要用两个类。
