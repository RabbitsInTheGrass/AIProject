package com.agent.server.skill;

import com.agent.server.model.entity.SkillConfig;
import com.agent.server.repository.SkillConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillRegistry {

    private final SkillConfigRepository skillConfigRepository;
    private final FileTools fileTools;
    private final CodeSearchTools codeSearchTools;
    private final ShellTools shellTools;

    /**
     * Get enabled skills for a user
     */
    public List<SkillConfig> getEnabledSkills(Long userId) {
        if (userId == null) {
            return skillConfigRepository.findByIsEnabledTrue();
        }
        return skillConfigRepository.findByUserId(userId);
    }

    /**
     * Get all available built-in tool callbacks
     */
    public List<ToolCallback> getToolCallbacks(Long userId, List<Long> requestedSkillIds) {
        List<ToolCallback> callbacks = new ArrayList<>();

        // Always add built-in tools
        callbacks.addAll(buildCallbacksFromFileTools());
        callbacks.addAll(buildCallbacksFromCodeSearchTools());
        callbacks.addAll(buildCallbacksFromShellTools());

        log.debug("Loaded {} tool callbacks for user {}", callbacks.size(), userId);
        return callbacks;
    }

    private List<ToolCallback> buildCallbacksFromFileTools() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(fileTools)
                .build()
                .getToolCallbacks();
    }

    private List<ToolCallback> buildCallbacksFromCodeSearchTools() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(codeSearchTools)
                .build()
                .getToolCallbacks();
    }

    private List<ToolCallback> buildCallbacksFromShellTools() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(shellTools)
                .build()
                .getToolCallbacks();
    }

    /**
     * Initialize built-in skills in database (called on startup)
     */
    public void initBuiltinSkills() {
        String[][] builtinSkills = {
                {"file_read", "Read File", "Read file contents with optional line range", "file"},
                {"file_write", "Write File", "Write content to a file", "file"},
                {"file_list", "List Directory", "List files and directories", "file"},
                {"file_find", "Find Files", "Search files by glob pattern", "file"},
                {"code_search", "Search Code", "Search code with regex patterns", "code"},
                {"code_definition", "Search Definition", "Find class/method definitions", "code"},
                {"shell_exec", "Execute Shell", "Run shell commands", "shell"},
                {"system_info", "System Info", "Get system information", "shell"},
        };

        for (String[] skill : builtinSkills) {
            if (skillConfigRepository.findByName(skill[0]).isEmpty()) {
                SkillConfig config = new SkillConfig();
                config.setName(skill[0]);
                config.setDisplayName(skill[1]);
                config.setDescription(skill[2]);
                config.setCategory(skill[3]);
                config.setIsEnabled(true);
                skillConfigRepository.save(config);
            }
        }
        log.info("Built-in skills initialized");
    }
}
