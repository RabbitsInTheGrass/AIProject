package com.agent.server.chat;

import com.agent.server.model.entity.SkillConfig;
import com.agent.server.service.LongTermMemoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SystemPromptBuilder {

    @Value("${app.work-dir:./workspace}")
    private String workDir;

    private final LongTermMemoryService memoryService;

    public SystemPromptBuilder(LongTermMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    private static final String BASE_PROMPT = """
            You are a powerful AI coding assistant, integrated with a web-based chat platform.
            You help users with coding tasks including writing, debugging, reviewing, and understanding code.
            
            Guidelines:
            1. Always use tools to gather information before answering questions about the codebase
            2. When modifying code, read the file first, then use write_file
            3. Explain your reasoning clearly and concisely
            4. When writing code, ensure it's complete and correct
            5. Use search_code for precise pattern matching
            
            Current working directory: %s
            """;

    public String build(List<SkillConfig> enabledSkills) {
        return build(enabledSkills, null);
    }

    public String build(List<SkillConfig> enabledSkills, Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(BASE_PROMPT, workDir));

        // Inject long-term memory if user is authenticated
        if (userId != null) {
            String memoryContext = memoryService.buildMemoryContext(userId);
            if (!memoryContext.isEmpty()) {
                sb.append(memoryContext);
            }
        }

        if (enabledSkills != null && !enabledSkills.isEmpty()) {
            sb.append("\nAvailable tools:\n");
            for (SkillConfig skill : enabledSkills) {
                sb.append(String.format("- %s: %s\n", skill.getName(), skill.getDescription()));
            }
        }

        return sb.toString();
    }
}
