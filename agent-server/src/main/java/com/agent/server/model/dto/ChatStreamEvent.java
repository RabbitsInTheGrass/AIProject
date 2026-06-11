package com.agent.server.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatStreamEvent {
    private String type;
    private String content;
    private String toolName;
    private String arguments;
    private String result;
    private String conversationId;
    private UsageInfo usage;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageInfo {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
