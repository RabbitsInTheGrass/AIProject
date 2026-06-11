package com.agent.server.chat;

import com.agent.server.model.entity.ModelConfig;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态 ChatModel 工厂 - 根据数据库配置创建/缓存 ChatModel 实例
 */
@Component
public class ChatModelFactory {

    private final Map<Long, OpenAiChatModel> cache = new ConcurrentHashMap<>();

    /**
     * 根据 ModelConfig 获取 ChatModel，带缓存
     */
    public OpenAiChatModel getChatModel(ModelConfig config) {
        return cache.computeIfAbsent(config.getId(), id -> createChatModel(config));
    }

    /**
     * 清除指定配置的缓存（配置更新时调用）
     */
    public void evict(Long configId) {
        cache.remove(configId);
    }

    /**
     * 清除所有缓存
     */
    public void evictAll() {
        cache.clear();
    }

    private OpenAiChatModel createChatModel(ModelConfig config) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModelName())
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.7)
                .maxTokens(config.getMaxTokens() != null ? config.getMaxTokens() : 4096)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
