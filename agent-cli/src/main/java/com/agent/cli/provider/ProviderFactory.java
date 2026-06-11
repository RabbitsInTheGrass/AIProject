package com.agent.cli.provider;

import com.agent.cli.model.ProviderConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM Provider 工厂 - 根据配置创建对应的 Provider
 */
public class ProviderFactory {

    private static final Map<String, ProviderConfig> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("deepseek", ProviderConfig.deepseek(""));
        PRESETS.put("glm", ProviderConfig.glm(""));
        PRESETS.put("moonshot", ProviderConfig.moonshot(""));
        PRESETS.put("stepfun", ProviderConfig.stepfun(""));
        PRESETS.put("spark", ProviderConfig.spark(""));
        PRESETS.put("openai", ProviderConfig.openai(""));
    }

    /**
     * 获取所有预设提供商
     */
    public static Map<String, ProviderConfig> getPresets() {
        return PRESETS;
    }

    /**
     * 根据提供商名称创建 Provider
     */
    public static LLMProvider create(String providerName, String apiKey, String model) {
        ProviderConfig preset = PRESETS.get(providerName.toLowerCase());
        if (preset == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName +
                    ". Available: " + String.join(", ", PRESETS.keySet()));
        }
        ProviderConfig config = new ProviderConfig(
                preset.getName(),
                apiKey,
                preset.getBaseUrl(),
                model != null ? model : preset.getModel()
        );
        return new OpenAICompatibleProvider(config);
    }

    /**
     * 根据自定义配置创建 Provider
     */
    public static LLMProvider create(ProviderConfig config) {
        return new OpenAICompatibleProvider(config);
    }

    /**
     * 获取支持的提供商列表描述
     */
    public static String getProviderListDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("  支持的模型提供商:\n");
        sb.append("  ┌─────────────┬──────────────────────────────────────────────┬─────────────────┐\n");
        sb.append(String.format("  │ %-11s │ %-44s │ %-15s │%n", "名称", "API 地址", "默认模型"));
        sb.append("  ├─────────────┼──────────────────────────────────────────────┼─────────────────┤\n");
        for (Map.Entry<String, ProviderConfig> entry : PRESETS.entrySet()) {
            ProviderConfig c = entry.getValue();
            sb.append(String.format("  │ %-11s │ %-44s │ %-15s │%n",
                    entry.getKey(), c.getBaseUrl(), c.getModel()));
        }
        sb.append("  └─────────────┴──────────────────────────────────────────────┴─────────────────┘");
        return sb.toString();
    }
}
