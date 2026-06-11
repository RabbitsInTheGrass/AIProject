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
@Table(name = "http_tool_config")
public class HttpToolConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column(name = "request_url", nullable = false, length = 500)
    private String requestUrl;

    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod = "GET";

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body_template", columnDefinition = "TEXT")
    private String requestBodyTemplate;

    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    @Column(name = "response_extract_path", length = 200)
    private String responseExtractPath;

    @Column(name = "parameter_schema", columnDefinition = "TEXT")
    private String parameterSchema;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 30000;

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
