package com.agent.server.skill;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FileTools {

    @Value("${app.work-dir:./workspace}")
    private String workDir;

    @Tool(description = "Read file contents. Supports reading specific line ranges. Returns the file content as text.")
    public String readFile(
            @ToolParam(description = "File path (absolute or relative to workspace)") String path,
            @ToolParam(description = "Start line number (1-based, optional)") Integer startLine,
            @ToolParam(description = "End line number (inclusive, optional)") Integer endLine) {
        try {
            Path filePath = resolvePath(path);
            if (!Files.exists(filePath)) {
                return "Error: File not found: " + path;
            }
            var lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

            if (startLine != null && endLine != null) {
                int start = Math.max(0, startLine - 1);
                int end = Math.min(lines.size(), endLine);
                lines = lines.subList(start, end);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                sb.append(String.format("%4d | %s\n", i + 1, lines.get(i)));
            }
            return sb.toString();
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file. Creates the file if it doesn't exist, overwrites if it does.")
    public String writeFile(
            @ToolParam(description = "File path (absolute or relative to workspace)") String path,
            @ToolParam(description = "Content to write") String content) {
        try {
            Path filePath = resolvePath(path);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return "Successfully wrote " + content.length() + " characters to " + path;
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    @Tool(description = "List files and directories at the given path. Shows file type and size.")
    public String listDirectory(
            @ToolParam(description = "Directory path (absolute or relative to workspace)") String path) {
        try {
            Path dirPath = resolvePath(path);
            if (!Files.isDirectory(dirPath)) {
                return "Error: Not a directory: " + path;
            }

            try (Stream<Path> stream = Files.list(dirPath)) {
                return stream.map(p -> {
                    try {
                        String type = Files.isDirectory(p) ? "DIR " : "FILE";
                        long size = Files.isDirectory(p) ? 0 : Files.size(p);
                        return String.format("%s %8s %s", type, formatSize(size), p.getFileName());
                    } catch (IOException e) {
                        return "? " + p.getFileName();
                    }
                }).collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            return "Error listing directory: " + e.getMessage();
        }
    }

    @Tool(description = "Search for files matching a glob pattern in the workspace directory.")
    public String findFiles(
            @ToolParam(description = "Glob pattern (e.g., '*.java', '**/*.py')") String pattern) {
        try {
            Path dirPath = Paths.get(workDir);
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

            try (Stream<Path> stream = Files.walk(dirPath, 10)) {
                return stream
                        .filter(p -> matcher.matches(dirPath.relativize(p)))
                        .limit(50)
                        .map(p -> dirPath.relativize(p).toString())
                        .collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            return "Error searching files: " + e.getMessage();
        }
    }

    private Path resolvePath(String path) {
        Path p = Paths.get(path);
        return p.isAbsolute() ? p : Paths.get(workDir).resolve(p);
    }

    private String formatSize(long size) {
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return (size / 1024) + "KB";
        return (size / (1024 * 1024)) + "MB";
    }
}
