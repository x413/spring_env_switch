package com.example.config;

/**
 * 配置作用域枚举
 * 定义配置修改的影响范围
 */
public enum ConfigScope {
    
    /**
     * 临时修改 - 只影响当前请求线程，不修改全局配置实例
     * 使用ThreadLocal存储，请求结束后自动清理
     */
    TEMPORARY("temporary", "临时修改"),
    
    /**
     * 全局修改 - 修改全局配置实例，影响所有后续请求
     * 直接更新AppConfig实例的属性值
     */
    GLOBAL("global", "全局修改");
    
    private final String code;
    private final String description;
    
    ConfigScope(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取枚举值
     */
    public static ConfigScope fromCode(String code) {
        if (code == null) {
            return GLOBAL; // 默认为全局修改
        }
        
        for (ConfigScope scope : values()) {
            if (scope.code.equalsIgnoreCase(code)) {
                return scope;
            }
        }
        
        throw new IllegalArgumentException("不支持的配置作用域: " + code + 
                                         "，支持的作用域: temporary, global");
    }
    
    /**
     * 是否为临时作用域
     */
    public boolean isTemporary() {
        return this == TEMPORARY;
    }
    
    /**
     * 是否为全局作用域
     */
    public boolean isGlobal() {
        return this == GLOBAL;
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s)", description, code);
    }
}
