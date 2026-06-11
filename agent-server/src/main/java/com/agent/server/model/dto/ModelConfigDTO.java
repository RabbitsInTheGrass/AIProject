package com.agent.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelConfigDTO {
    private Long id;

    @NotBlank(message = "模型名称不能为空")
    private String name;

    @NotBlank(message = "提供商标识不能为空")
    private String provider;

    @NotBlank(message = "API地址不能为空")
    private String baseUrl;

    @NotBlank(message = "API密钥不能为空")
    private String apiKey;

    @NotBlank(message = "模型标识不能为空")
    private String modelName;

    private Double temperature = 0.7;
    private Integer maxTokens = 4096;
    private Boolean isDefault = false;
    private String extraHeaders;
}
