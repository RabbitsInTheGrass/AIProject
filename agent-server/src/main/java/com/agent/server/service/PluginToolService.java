package com.agent.server.service;

import com.agent.server.model.dto.PluginToolDTO;
import com.agent.server.model.entity.PluginTool;
import com.agent.server.repository.PluginToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginToolService {

    private final PluginToolRepository repository;

    @Value("${app.plugins.upload-dir:./plugins}")
    private String pluginDir;

    public List<PluginTool> listByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public PluginTool getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Plugin not found: " + id));
    }

    public List<PluginTool> getEnabledPlugins(Long userId) {
        return repository.findByUserIdAndIsEnabledTrue(userId);
    }

    @Transactional
    public PluginTool create(PluginToolDTO dto, MultipartFile jarFile, Long userId) throws IOException {
        // Save jar file
        Path pluginPath = Paths.get(pluginDir);
        Files.createDirectories(pluginPath);
        String jarFileName = userId + "_" + jarFile.getOriginalFilename();
        Path jarPath = pluginPath.resolve(jarFileName);
        jarFile.transferTo(jarPath.toFile());

        PluginTool plugin = new PluginTool();
        plugin.setUserId(userId);
        plugin.setName(dto.getName());
        plugin.setDisplayName(dto.getDisplayName());
        plugin.setDescription(dto.getDescription());
        plugin.setMainClass(dto.getMainClass());
        plugin.setJarPath(jarPath.toString());
        plugin.setConfigJson(dto.getConfigJson());
        plugin.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true);
        return repository.save(plugin);
    }

    @Transactional
    public PluginTool update(Long id, PluginToolDTO dto) {
        PluginTool plugin = getById(id);
        plugin.setName(dto.getName());
        plugin.setDisplayName(dto.getDisplayName());
        plugin.setDescription(dto.getDescription());
        plugin.setMainClass(dto.getMainClass());
        plugin.setConfigJson(dto.getConfigJson());
        if (dto.getIsEnabled() != null) plugin.setIsEnabled(dto.getIsEnabled());
        return repository.save(plugin);
    }

    @Transactional
    public PluginTool toggle(Long id) {
        PluginTool plugin = getById(id);
        plugin.setIsEnabled(!plugin.getIsEnabled());
        return repository.save(plugin);
    }

    @Transactional
    public void delete(Long id) {
        PluginTool plugin = getById(id);
        // Delete jar file
        try {
            Files.deleteIfExists(Paths.get(plugin.getJarPath()));
        } catch (IOException e) {
            log.warn("Failed to delete plugin jar: {}", plugin.getJarPath(), e);
        }
        repository.deleteById(id);
    }
}
