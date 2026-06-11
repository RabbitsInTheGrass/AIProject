package com.agent.server.service;

import com.agent.server.model.entity.UserLongTermMemory;
import com.agent.server.repository.UserLongTermMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LongTermMemoryService {

    private final UserLongTermMemoryRepository repository;

    public List<UserLongTermMemory> getActiveMemories(Long userId) {
        return repository.findByUserIdAndIsActiveTrue(userId);
    }

    public String buildMemoryContext(Long userId) {
        List<UserLongTermMemory> memories = getActiveMemories(userId);
        if (memories.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("\n=== User Long-term Memory ===\n");
        for (UserLongTermMemory mem : memories) {
            sb.append(String.format("[%s] %s\n", mem.getMemoryType(), mem.getContent()));
        }
        return sb.toString();
    }

    @Async
    public void extractAndSaveMemory(Long userId, String conversationContent, String conversationId) {
        try {
            // Simple keyword-based extraction (Phase 1)
            // TODO: Use LLM to extract memories in Phase 5

            if (conversationContent.contains("我喜欢") || conversationContent.contains("I prefer") ||
                    conversationContent.contains("I like")) {
                UserLongTermMemory memory = new UserLongTermMemory();
                memory.setUserId(userId);
                memory.setMemoryType("PREFERENCE");
                memory.setContent("User preference extracted from conversation: " + conversationId);
                memory.setSourceConversationId(conversationId);
                repository.save(memory);
            }

            log.debug("Memory extraction completed for conversation: {}", conversationId);
        } catch (Exception e) {
            log.error("Failed to extract memory from conversation: {}", conversationId, e);
        }
    }

    public UserLongTermMemory addMemory(Long userId, String type, String content) {
        UserLongTermMemory memory = new UserLongTermMemory();
        memory.setUserId(userId);
        memory.setMemoryType(type);
        memory.setContent(content);
        return repository.save(memory);
    }

    public void deleteMemory(Long id) {
        repository.deleteById(id);
    }

    public void deactivateMemory(Long id) {
        UserLongTermMemory memory = repository.findById(id).orElseThrow();
        memory.setIsActive(false);
        repository.save(memory);
    }
}
