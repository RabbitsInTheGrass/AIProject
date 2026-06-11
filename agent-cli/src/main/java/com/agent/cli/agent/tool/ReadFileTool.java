package com.agent.cli.agent.tool;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取文件内容工具
 */
public class ReadFileTool implements AgentTool {

    private final Path workDir;

    public ReadFileTool(Path workDir) {
        this.workDir = workDir;
    }

    @Override
    public String getName() { return "read_file"; }

    @Override
    public String getDescription() {
        return "Read the content of a file. Returns the file content as text. " +
               "Supports optional start_line and end_line to read a specific range.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> pathProp = new LinkedHashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "Path to the file to read (relative to working directory)");
        properties.put("path", pathProp);

        Map<String, Object> startLine = new LinkedHashMap<>();
        startLine.put("type", "integer");
        startLine.put("description", "Start line number (1-based, optional)");
        properties.put("start_line", startLine);

        Map<String, Object> endLine = new LinkedHashMap<>();
        endLine.put("type", "integer");
        endLine.put("description", "End line number (1-based, optional)");
        properties.put("end_line", endLine);

        schema.put("properties", properties);
        schema.put("required", new String[]{"path"});
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        String path = arguments.get("path").getAsString();
        Path filePath = workDir.resolve(path);

        if (!Files.exists(filePath)) {
            return "Error: File not found: " + path;
        }
        if (!Files.isRegularFile(filePath)) {
            return "Error: Not a regular file: " + path;
        }

        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            int start = 1;
            int end = lines.size();

            if (arguments.has("start_line") && !arguments.get("start_line").isJsonNull()) {
                start = arguments.get("start_line").getAsInt();
            }
            if (arguments.has("end_line") && !arguments.get("end_line").isJsonNull()) {
                end = Math.min(arguments.get("end_line").getAsInt(), lines.size());
            }

            StringBuilder sb = new StringBuilder();
            for (int i = start - 1; i < end && i < lines.size(); i++) {
                sb.append(String.format("%4d | %s%n", i + 1, lines.get(i)));
            }
            return sb.toString();
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}
