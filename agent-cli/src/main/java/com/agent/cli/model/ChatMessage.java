package com.agent.cli.model;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息
 */
public class ChatMessage {

    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant"),
        TOOL("tool");

        private final String value;
        Role(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    private Role role;
    private String content;
    private String name;
    private List<ToolCall> toolCalls;
    private String toolCallId;

    public ChatMessage() {}

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }

    public static ChatMessage assistantWithToolCalls(List<ToolCall> toolCalls) {
        ChatMessage msg = new ChatMessage(Role.ASSISTANT, null);
        msg.toolCalls = toolCalls;
        return msg;
    }

    public static ChatMessage tool(String toolCallId, String content) {
        ChatMessage msg = new ChatMessage(Role.TOOL, content);
        msg.toolCallId = toolCallId;
        return msg;
    }

    // Getters and setters
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    /**
     * Tool调用信息
     */
    public static class ToolCall {
        private String id;
        private String type = "function";
        private FunctionCall function;

        public ToolCall() {}

        public ToolCall(String id, String name, String arguments) {
            this.id = id;
            this.type = "function";
            this.function = new FunctionCall(name, arguments);
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public FunctionCall getFunction() { return function; }
        public void setFunction(FunctionCall function) { this.function = function; }

        public static class FunctionCall {
            private String name;
            private String arguments;

            public FunctionCall() {}

            public FunctionCall(String name, String arguments) {
                this.name = name;
                this.arguments = arguments;
            }

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getArguments() { return arguments; }
            public void setArguments(String arguments) { this.arguments = arguments; }
        }
    }

    /**
     * Tool定义 (用于发送给模型)
     */
    public static class ToolDefinition {
        private String type = "function";
        private FunctionDef function;

        public ToolDefinition(String name, String description, Map<String, Object> parameters) {
            this.function = new FunctionDef(name, description, parameters);
        }

        public String getType() { return type; }
        public FunctionDef getFunction() { return function; }

        public static class FunctionDef {
            private String name;
            private String description;
            private Map<String, Object> parameters;

            public FunctionDef(String name, String description, Map<String, Object> parameters) {
                this.name = name;
                this.description = description;
                this.parameters = parameters;
            }

            public String getName() { return name; }
            public String getDescription() { return description; }
            public Map<String, Object> getParameters() { return parameters; }
        }
    }
}
