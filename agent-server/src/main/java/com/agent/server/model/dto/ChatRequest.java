package com.agent.server.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String conversationId;
    private String content;
    private Long modelConfigId;
    private List<Long> enabledSkillIds;
    private Long knowledgeBaseId;
}
