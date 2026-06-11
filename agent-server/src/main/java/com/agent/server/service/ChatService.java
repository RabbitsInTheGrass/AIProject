package com.agent.server.service;

import com.agent.server.chat.ChatModelFactory;
import com.agent.server.chat.SystemPromptBuilder;
import com.agent.server.model.dto.ChatRequest;
import com.agent.server.model.dto.ChatStreamEvent;
import com.agent.server.model.entity.ChatMessageEntity;
import com.agent.server.model.entity.Conversation;
import com.agent.server.model.entity.ModelConfig;
import com.agent.server.model.entity.SkillConfig;
import com.agent.server.repository.ChatMessageRepository;
import com.agent.server.skill.SkillRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationService conversationService;
    private final ModelConfigService modelConfigService;
    private final ChatModelFactory chatModelFactory;
    private final SystemPromptBuilder systemPromptBuilder;
    private final SkillRegistry skillRegistry;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_HISTORY = 20;

    public void streamChat(ChatRequest request, SseEmitter emitter, Long userId) {
        try {
            // 1. Get or create conversation
            Conversation conversation = conversationService.getOrCreate(
                    request.getConversationId(), userId, request.getModelConfigId());

            // 2. Get model config
            ModelConfig modelConfig = resolveModelConfig(request.getModelConfigId());

            // 3. Create ChatModel
            OpenAiChatModel chatModel = chatModelFactory.getChatModel(modelConfig);

            // 4. Build system prompt
            List<SkillConfig> enabledSkills = skillRegistry.getEnabledSkills(userId);
            String systemPrompt = systemPromptBuilder.build(enabledSkills);

            // 5. Build message list
            List<Message> messages = buildMessages(conversation.getId(), systemPrompt);

            // 6. Add user message
            UserMessage userMsg = new UserMessage(request.getContent());
            messages.add(userMsg);

            // Save user message
            saveMessage(conversation.getId(), "user", request.getContent(), null);

            // 7. Get tool callbacks
            var toolCallbacks = skillRegistry.getToolCallbacks(userId, request.getEnabledSkillIds());

            // 8. Build prompt with options
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(modelConfig.getModelName())
                    .temperature(modelConfig.getTemperature())
                    .maxTokens(modelConfig.getMaxTokens())
                    .toolCallbacks(toolCallbacks.toArray(new org.springframework.ai.tool.ToolCallback[0]))
                    .build();

            Prompt prompt = new Prompt(messages, options);

            // 9. Stream response
            StringBuilder fullResponse = new StringBuilder();
            chatModel.stream(prompt)
                    .doOnNext(response -> {
                        try {
                            String content = extractContent(response);
                            if (content != null && !content.isEmpty()) {
                                fullResponse.append(content);
                                emitter.send(SseEmitter.event().data(
                                        ChatStreamEvent.builder()
                                                .type("content")
                                                .content(content)
                                                .conversationId(conversation.getId())
                                                .build()
                                ));
                            }
                        } catch (IOException e) {
                            log.error("Error sending SSE event", e);
                        }
                    })
                    .doOnError(error -> {
                        try {
                            log.error("Stream error", error);
                            emitter.send(SseEmitter.event().data(
                                    ChatStreamEvent.builder()
                                            .type("error")
                                            .message(error.getMessage())
                                            .build()
                            ));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("Error sending error event", e);
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            // Save assistant message
                            saveMessage(conversation.getId(), "assistant", fullResponse.toString(), null);

                            emitter.send(SseEmitter.event().data(
                                    ChatStreamEvent.builder()
                                            .type("done")
                                            .conversationId(conversation.getId())
                                            .build()
                            ));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("Error sending done event", e);
                        }
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("Chat stream error", e);
            try {
                emitter.send(SseEmitter.event().data(
                        ChatStreamEvent.builder()
                                .type("error")
                                .message(e.getMessage())
                                .build()
                ));
                emitter.complete();
            } catch (IOException ex) {
                log.error("Error sending error", ex);
            }
        }
    }

    private ModelConfig resolveModelConfig(Long modelConfigId) {
        if (modelConfigId != null) {
            return modelConfigService.getById(modelConfigId);
        }
        // Return default or first
        List<ModelConfig> configs = modelConfigService.listByUser(null);
        return configs.stream()
                .filter(ModelConfig::getIsDefault)
                .findFirst()
                .orElse(configs.isEmpty() ? null : configs.get(0));
    }

    private List<Message> buildMessages(String conversationId, String systemPrompt) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        List<ChatMessageEntity> history = messageRepository
                .findByConversationIdOrderBySortOrder(conversationId);

        int start = Math.max(0, history.size() - MAX_HISTORY);
        for (int i = start; i < history.size(); i++) {
            ChatMessageEntity msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return messages;
    }

    private void saveMessage(String conversationId, String role, String content, String toolCalls) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setConversationId(conversationId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setToolCalls(toolCalls);
        entity.setSortOrder(messageRepository.countByConversationId(conversationId));
        messageRepository.save(entity);
    }

    private String extractContent(ChatResponse response) {
        if (response.getResult() != null && response.getResult().getOutput() != null) {
            return response.getResult().getOutput().getText();
        }
        return null;
    }
}
