package com.agent.cli.model;

import java.util.Map;

/**
 * 模型提供商配置
 */
public class ProviderConfig {

    private String name;
    private String apiKey;
    private String baseUrl;
    private String model;
    private double temperature = 0.7;
    private int maxTokens = 4096;
    private boolean stream = true;
    private Map<String, String> extraHeaders;

    public ProviderConfig() {}

    public ProviderConfig(String name, String apiKey, String baseUrl, String model) {
        this.name = name;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
    public Map<String, String> getExtraHeaders() { return extraHeaders; }
    public void setExtraHeaders(Map<String, String> extraHeaders) { this.extraHeaders = extraHeaders; }

    // 预定义配置
    public static ProviderConfig deepseek(String apiKey) {
        return new ProviderConfig("DeepSeek", apiKey,
                "https://api.deepseek.com", "deepseek-chat");
    }

    public static ProviderConfig glm(String apiKey) {
        return new ProviderConfig("GLM", apiKey,
                "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash");
    }

    public static ProviderConfig moonshot(String apiKey) {
        return new ProviderConfig("Moonshot", apiKey,
                "https://api.moonshot.cn/v1", "moonshot-v1-8k");
    }

    public static ProviderConfig stepfun(String apiKey) {
        return new ProviderConfig("StepFun", apiKey,
                "https://api.stepfun.com/v1", "step-1-8k");
    }

    public static ProviderConfig spark(String apiKey) {
        return new ProviderConfig("Spark", apiKey,
                "https://spark-api-open.xf-yun.com/v1", "generalv3.5");
    }

    public static ProviderConfig openai(String apiKey) {
        return new ProviderConfig("OpenAI", apiKey,
                "https://api.openai.com/v1", "gpt-4o");
    }
}
