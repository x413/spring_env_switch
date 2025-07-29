package com.example.config;

import org.springframework.context.ApplicationEvent;

/**
 * 环境切换事件
 * 当环境配置发生切换时发布此事件
 */
public class EnvironmentChangeEvent extends ApplicationEvent {

    private final String oldEnvironment;
    private final String newEnvironment;
    private final ConfigScope scope;
    private final long timestamp;

    public EnvironmentChangeEvent(Object source, String oldEnvironment, String newEnvironment) {
        this(source, oldEnvironment, newEnvironment, ConfigScope.GLOBAL);
    }

    public EnvironmentChangeEvent(Object source, String oldEnvironment, String newEnvironment, ConfigScope scope) {
        super(source);
        this.oldEnvironment = oldEnvironment;
        this.newEnvironment = newEnvironment;
        this.scope = scope;
        this.timestamp = System.currentTimeMillis();
    }

    public String getOldEnvironment() {
        return oldEnvironment;
    }

    public String getNewEnvironment() {
        return newEnvironment;
    }

    public ConfigScope getScope() {
        return scope;
    }

    public long getEventTimestamp() {
        return timestamp;
    }

    public boolean isTemporary() {
        return scope.isTemporary();
    }

    public boolean isGlobal() {
        return scope.isGlobal();
    }

    @Override
    public String toString() {
        return "EnvironmentChangeEvent{" +
                "oldEnvironment='" + oldEnvironment + '\'' +
                ", newEnvironment='" + newEnvironment + '\'' +
                ", scope=" + scope +
                ", timestamp=" + timestamp +
                '}';
    }
}
