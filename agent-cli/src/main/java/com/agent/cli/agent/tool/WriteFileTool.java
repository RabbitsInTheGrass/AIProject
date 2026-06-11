package com.agent.cli.agent.tool;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 写入文件工具
 */
public class WriteFileTool implements AgentTool {

    private final Path workDir;

    public WriteFileTool(Path workDir) {
        this.workDir = workDir;
    }

    @Override
    public String getName() { return "write_file"; }

    @Override
    public String getDescription() {
        return "Write content to a file. Creates the file if it doesn't exist, or overwrites it. " +
               "Automatically creates parent directories.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> pathProp = new LinkedHashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "Path to the file to write (relative to working directory)");
        properties.put("path", pathProp);

        Map<String, Object> contentProp = new LinkedHashMap<>();
        contentProp.put("type", "string");
        contentProp.put("description", "Content to write to the file");
        properties.put("content", contentProp);

        schema.put("properties", properties);
        schema.put("required", new String[]{"path", "content"});
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        String path = arguments.get("path").getAsString();
        String content = arguments.get("content").getAsString();
        Path filePath = workDir.resolve(path);

        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            int lines = content.split("\n", -1).length;
            return "Successfully wrote " + lines + " lines to " + path;
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}
