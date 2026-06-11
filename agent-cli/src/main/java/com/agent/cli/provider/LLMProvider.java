package com.agent.cli.provider;

import com.agent.cli.model.ChatMessage;
import com.agent.cli.model.ChatResponse;

import java.util.List;
import java.util.function.Consumer;

/**
 * LLM 提供商接口 - 所有模型提供商的统一抽象
 */
public interface LLMProvider {

    /**
     * 获取提供商名称
     */
    String getName();

    /**
     * 同步聊天
     */
    ChatResponse chat(List<ChatMessage> messages, List<ChatMessage.ToolDefinition> tools);

    /**
     * 流式聊天
     * @param onContent 每收到一段文本时的回调
     * @param onComplete 完成时的回调，返回完整响应
     */
    void chatStream(List<ChatMessage> messages, List<ChatMessage.ToolDefinition> tools,
                    Consumer<String> onContent, Consumer<ChatResponse> onComplete,
                    Consumer<Throwable> onError);

    /**
     * 获取当前模型名称
     */
    String getModelName();
}
