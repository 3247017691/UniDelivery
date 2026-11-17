package com.unidelivery.mapi;

import java.util.Map;

/**
 * path 配置注册表：默认值来自手册，配置可覆盖（应对 uni_project/oc_project 迁移与文档不一致）。
 * 未配置且无默认值的接口拒绝调用，绝不猜 path。
 */
public final class MapiPaths {

    private final Map<String, String> overrides;

    public MapiPaths(Map<String, String> overrides) {
        this.overrides = Map.copyOf(overrides);
    }

    public String resolve(MapiEndpoint endpoint) {
        String override = overrides.get(endpoint.name());
        if (override != null && !override.isBlank()) {
            return override;
        }
        String defaultPath = endpoint.defaultPath();
        if (defaultPath == null) {
            throw new IllegalStateException("接口 " + endpoint.name()
                    + " 的 path 未核实且未配置覆盖（mapi.paths." + endpoint.name() + "），拒绝调用");
        }
        return defaultPath;
    }
}
