package com.agent.server.service;

import com.agent.server.chat.ChatModelFactory;
import com.agent.server.model.dto.ModelConfigDTO;
import com.agent.server.model.entity.ModelConfig;
import com.agent.server.repository.ModelConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final ModelConfigRepository repository;
    private final ChatModelFactory chatModelFactory;

    public List<ModelConfig> listByUser(Long userId) {
        if (userId == null) return repository.findAll();
        return repository.findByUserId(userId);
    }

    public ModelConfig getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Model config not found: " + id));
    }

    @Transactional
    public ModelConfig create(ModelConfigDTO dto, Long userId) {
        ModelConfig config = new ModelConfig();
        copyFromDTO(config, dto);
        config.setUserId(userId);
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearOtherDefaults(userId);
        }
        return repository.save(config);
    }

    @Transactional
    public ModelConfig update(Long id, ModelConfigDTO dto, Long userId) {
        ModelConfig config = getById(id);
        copyFromDTO(config, dto);
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearOtherDefaults(userId);
        }
        chatModelFactory.evict(id);
        return repository.save(config);
    }

    @Transactional
    public void delete(Long id) {
        chatModelFactory.evict(id);
        repository.deleteById(id);
    }

    public List<Map<String, String>> getPresets() {
        return List.of(
                Map.of("name", "DeepSeek", "provider", "deepseek", "baseUrl", "https://api.deepseek.com", "model", "deepseek-chat"),
                Map.of("name", "OpenAI GPT-4o", "provider", "openai", "baseUrl", "https://api.openai.com", "model", "gpt-4o"),
                Map.of("name", "Claude 3.5 Sonnet", "provider", "anthropic", "baseUrl", "https://api.anthropic.com", "model", "claude-3-5-sonnet-20241022"),
                Map.of("name", "通义千问", "provider", "dashscope", "baseUrl", "https://dashscope.aliyuncs.com/compatible-mode", "model", "qwen-max"),
                Map.of("name", "GLM-4", "provider", "zhipu", "baseUrl", "https://open.bigmodel.cn/api/paas", "model", "glm-4"),
                Map.of("name", "Moonshot", "provider", "moonshot", "baseUrl", "https://api.moonshot.cn", "model", "moonshot-v1-8k")
        );
    }

    private void clearOtherDefaults(Long userId) {
        repository.findByUserId(userId).stream()
                .filter(ModelConfig::getIsDefault)
                .forEach(c -> { c.setIsDefault(false); repository.save(c); });
    }

    private void copyFromDTO(ModelConfig config, ModelConfigDTO dto) {
        config.setName(dto.getName());
        config.setProvider(dto.getProvider());
        config.setBaseUrl(dto.getBaseUrl());
        config.setApiKey(dto.getApiKey());
        config.setModelName(dto.getModelName());
        config.setTemperature(dto.getTemperature());
        config.setMaxTokens(dto.getMaxTokens());
        config.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);
        config.setExtraHeaders(dto.getExtraHeaders());
    }
}
