# Spring 环境动态切换示例

这个项目演示了如何在Spring Boot应用中实现运行时动态切换不同环境的配置文件。

## 功能特性

- ✅ 运行时动态切换配置文件（dev、prod、test）
- ✅ 使用 `@ConfigurationProperties` 自动绑定配置
- ✅ **直接刷新配置实例**，无需刷新整个Spring上下文
- ✅ **支持配置作用域**：临时配置（线程级）和全局配置
- ✅ **极简设计**：最少的类和最简单的API
- ✅ 线程安全的配置管理
- ✅ 配置变更事件通知
- ✅ REST API 接口管理
- ✅ 高性能，轻量级配置切换
- ✅ 完整的示例和测试

## 项目结构

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── Application.java                 # Spring Boot 启动类
│   │   ├── config/
│   │   │   ├── AppConfig.java              # 配置实体类
│   │   │   ├── DynamicConfigManager.java   # 动态配置管理器
│   │   │   └── EnvironmentChangeEvent.java # 环境切换事件
│   │   ├── controller/
│   │   │   ├── ConfigController.java       # 配置管理API
│   │   │   └── DemoController.java         # 演示API
│   │   └── service/
│   │       └── ConfigDemoService.java      # 配置演示服务
│   └── resources/
│       ├── application.properties          # 主配置文件
│       ├── config-dev.properties          # 开发环境配置
│       ├── config-prod.properties         # 生产环境配置
│       └── config-test.properties         # 测试环境配置
└── test/
    └── java/com/example/
        └── ApplicationTest.java            # 测试类
```

## 快速开始

### 1. 启动应用

```bash
# 使用Maven启动
mvn spring-boot:run

# 或者编译后运行
mvn clean package
java -jar target/spring-env-switch-1.0.0.jar
```

### 2. 访问应用

应用启动后，访问 http://localhost:8080

### 3. API 接口

#### 配置管理接口

- `GET /api/config/environment` - 获取当前环境信息
- `POST /api/config/environment/{env}` - 切换环境（默认全局作用域）
- `POST /api/config/environment/{env}/{scope}` - 切换环境（指定作用域：temporary/global）
- `GET /api/config/current` - 获取当前配置详情
- `GET /api/config/properties` - 获取原始配置属性
- `DELETE /api/config/temporary` - 清除当前线程的临时配置
- `GET /api/config/temporary/statistics` - 获取临时配置统计信息
- `GET /api/config/health` - 健康检查
- `GET /api/health` - 基本健康检查
- `GET /api/health/detailed` - 详细健康检查

#### 演示接口

- `GET /api/demo/database` - 演示数据库配置
- `GET /api/demo/redis` - 演示Redis配置
- `GET /api/demo/api` - 演示API配置
- `GET /api/demo/features` - 演示功能开关
- `GET /api/demo/notifications` - 演示通知配置
- `GET /api/demo/summary` - 获取完整配置摘要

#### 优化测试接口

- `POST /api/test/switch-and-compare/{env}` - 测试配置切换前后的值变化
- `GET /api/test/verify-instance` - 验证配置实例引用的一致性
- `POST /api/test/continuous-switch` - 连续切换性能测试

#### 简单测试接口

- `POST /api/simple-test/compare/{env}` - 对比临时配置与全局配置
- `GET /api/simple-test/status` - 获取当前配置状态
- `POST /api/simple-test/quick-switch` - 快速切换性能测试

## 使用示例

### 1. 查看当前环境

```bash
curl http://localhost:8080/api/config/environment
```

响应：
```json
{
  "currentEnvironment": "dev",
  "supportedEnvironments": ["dev", "prod", "test"],
  "timestamp": 1640995200000
}
```

### 2. 全局切换到生产环境

```bash
curl -X POST http://localhost:8080/api/config/environment/prod/global
```

响应：
```json
{
  "success": true,
  "currentEnvironment": "prod",
  "targetEnvironment": "prod",
  "scope": "global",
  "scopeDescription": "全局修改",
  "message": "环境切换成功 (全局修改)",
  "timestamp": 1640995200000
}
```

### 3. 临时切换到测试环境（仅当前线程）

```bash
curl -X POST http://localhost:8080/api/config/environment/test/temporary
```

响应：
```json
{
  "success": true,
  "currentEnvironment": "prod",
  "targetEnvironment": "test",
  "scope": "temporary",
  "scopeDescription": "临时修改",
  "hasTemporaryConfig": true,
  "message": "环境切换成功 (临时修改)",
  "timestamp": 1640995200000
}
```

### 4. 清除临时配置

```bash
curl -X DELETE http://localhost:8080/api/config/temporary
```

### 5. 查看当前配置

```bash
curl http://localhost:8080/api/config/current
```

### 6. 演示配置使用

```bash
curl http://localhost:8080/api/demo/summary
```

### 7. 测试作用域功能

```bash
# 对比临时配置与全局配置
curl -X POST http://localhost:8080/api/simple-test/compare/prod

# 查看当前配置状态
curl http://localhost:8080/api/simple-test/status

# 快速切换性能测试
curl -X POST http://localhost:8080/api/simple-test/quick-switch
```

## 配置文件说明

### 开发环境 (config-dev.properties)
- 使用本地数据库和Redis
- 启用调试模式
- 禁用通知功能

### 生产环境 (config-prod.properties)
- 使用生产数据库和Redis
- 启用监控和通知
- 禁用调试模式

### 测试环境 (config-test.properties)
- 使用内存数据库
- 禁用缓存和通知
- 启用调试模式

## 核心实现原理

### 极简设计理念
- **单一职责**: `DynamicConfigManager` 负责所有配置管理
- **最少抽象**: 直接使用 `ThreadLocal` 存储临时配置
- **简单API**: 核心方法只有 `switchEnvironment()` 和 `getCurrentConfig()`

### 技术实现
1. **动态配置源**: 使用 `PropertiesPropertySource` 动态添加配置源
2. **直接配置刷新**: 使用 `Binder` 重新绑定配置并直接更新配置实例属性
3. **配置作用域**: 支持全局配置和临时配置两种作用域
4. **线程隔离**: 使用 `ThreadLocal<AppConfig>` 实现线程级别的临时配置
5. **智能选择**: `getCurrentConfig()` 自动选择临时配置或全局配置
6. **线程安全**: 全局配置使用 `ReentrantReadWriteLock` 保证并发安全
7. **事件通知**: 使用 `ApplicationEventPublisher` 发布配置变更事件
8. **轻量级刷新**: 避免全局上下文刷新，只更新目标配置实例

### 配置作用域详解

#### 全局配置 (Global)
- 修改全局 `AppConfig` 实例
- 影响所有线程和后续请求
- 适用于永久性的环境切换

#### 临时配置 (Temporary)
- 使用 `ThreadLocal` 存储线程级配置
- 只影响当前请求线程
- 适用于测试、调试或特殊业务场景

### 使用方式
```java
// 业务代码中获取当前有效配置
AppConfig config = configManager.getCurrentConfig();
String dbUrl = config.getDatabase().getUrl();
```

## 扩展功能

- 支持配置中心集成（如Nacos、Apollo）
- 支持配置加密/解密
- 支持配置版本管理
- 支持配置回滚功能
- 支持配置变更审计

## 注意事项

1. 配置切换是全局操作，会影响整个应用
2. 频繁切换配置可能影响性能
3. 建议在生产环境谨慎使用动态切换功能
4. 配置文件必须存在于classpath中

## 技术栈

- Spring Boot 3.2.0
- Spring Cloud Context
- Maven
- JUnit 5
