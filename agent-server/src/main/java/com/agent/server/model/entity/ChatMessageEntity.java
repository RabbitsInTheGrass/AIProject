package com.agent.server.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_message", indexes = {
    @Index(name = "idx_conversation_sort", columnList = "conversation_id, sort_order")
})
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 36)
    private String conversationId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_calls", columnDefinition = "TEXT")
    private String toolCalls;

    @Column(name = "tool_call_id", length = 100)
    private String toolCallId;

    @Column(name = "token_usage", length = 500)
    private String tokenUsage;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
