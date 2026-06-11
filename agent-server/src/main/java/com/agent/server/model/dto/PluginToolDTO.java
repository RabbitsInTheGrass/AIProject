package com.agent.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PluginToolDTO {
    private Long id;

    @NotBlank(message = "插件名称不能为空")
    private String name;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    private String description;

    @NotBlank(message = "主类名不能为空")
    private String mainClass;

    private Boolean isEnabled = true;
    private String configJson;
}
