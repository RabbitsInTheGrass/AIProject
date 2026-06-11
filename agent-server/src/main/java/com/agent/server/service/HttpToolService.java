package com.agent.server.service;

import com.agent.server.model.dto.HttpToolConfigDTO;
import com.agent.server.model.entity.HttpToolConfig;
import com.agent.server.repository.HttpToolConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HttpToolService {

    private final HttpToolConfigRepository repository;

    public List<HttpToolConfig> listByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public HttpToolConfig getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("HTTP tool not found: " + id));
    }

    public List<HttpToolConfig> getEnabledTools(Long userId) {
        return repository.findByUserIdAndIsEnabledTrue(userId);
    }

    @Transactional
    public HttpToolConfig create(HttpToolConfigDTO dto, Long userId) {
        HttpToolConfig config = new HttpToolConfig();
        copyFromDTO(config, dto);
        config.setUserId(userId);
        return repository.save(config);
    }

    @Transactional
    public HttpToolConfig update(Long id, HttpToolConfigDTO dto) {
        HttpToolConfig config = getById(id);
        copyFromDTO(config, dto);
        return repository.save(config);
    }

    @Transactional
    public HttpToolConfig toggle(Long id) {
        HttpToolConfig config = getById(id);
        config.setIsEnabled(!config.getIsEnabled());
        return repository.save(config);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void copyFromDTO(HttpToolConfig config, HttpToolConfigDTO dto) {
        config.setName(dto.getName());
        config.setDisplayName(dto.getDisplayName());
        config.setDescription(dto.getDescription());
        config.setRequestUrl(dto.getRequestUrl());
        config.setRequestMethod(dto.getRequestMethod());
        config.setRequestHeaders(dto.getRequestHeaders());
        config.setRequestBodyTemplate(dto.getRequestBodyTemplate());
        config.setRequestParams(dto.getRequestParams());
        config.setResponseExtractPath(dto.getResponseExtractPath());
        config.setParameterSchema(dto.getParameterSchema());
        config.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true);
        config.setTimeoutMs(dto.getTimeoutMs() != null ? dto.getTimeoutMs() : 30000);
    }
}
