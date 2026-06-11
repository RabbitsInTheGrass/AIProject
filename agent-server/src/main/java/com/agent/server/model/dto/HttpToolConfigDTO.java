package com.agent.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HttpToolConfigDTO {
    private Long id;

    @NotBlank(message = "工具名称不能为空")
    private String name;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    private String description;

    @NotBlank(message = "请求URL不能为空")
    private String requestUrl;

    private String requestMethod = "GET";
    private String requestHeaders;
    private String requestBodyTemplate;
    private String requestParams;
    private String responseExtractPath;
    private String parameterSchema;
    private Boolean isEnabled = true;
    private Integer timeoutMs = 30000;
}
