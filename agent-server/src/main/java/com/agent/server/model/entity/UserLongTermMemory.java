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
@Table(name = "user_long_term_memory", indexes = {
    @Index(name = "idx_user_memory_type", columnList = "user_id, memory_type")
})
public class UserLongTermMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "memory_type", nullable = false, length = 30)
    private String memoryType; // PREFERENCE, FACT, SUMMARY

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_conversation_id", length = 36)
    private String sourceConversationId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
