package com.agent.cli.model;

/**
 * 模型响应
 */
public class ChatResponse {

    private String id;
    private String content;
    private java.util.List<ChatMessage.ToolCall> toolCalls;
    private Usage usage;
    private String finishReason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public java.util.List<ChatMessage.ToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(java.util.List<ChatMessage.ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;

        public int getPromptTokens() { return promptTokens; }
        public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
        public int getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
        public int getTotalTokens() { return totalTokens; }
        public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    }
}
