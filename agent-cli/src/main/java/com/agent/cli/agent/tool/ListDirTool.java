package com.agent.cli.agent.tool;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * 列出目录工具
 */
public class ListDirTool implements AgentTool {

    private final Path workDir;
    private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
            ".git", "node_modules", "target", "build", ".idea", "__pycache__", ".vscode"));

    public ListDirTool(Path workDir) {
        this.workDir = workDir;
    }

    @Override
    public String getName() { return "list_dir"; }

    @Override
    public String getDescription() {
        return "List files and directories in a given path. Shows file sizes and types. " +
               "Use recursive=true to list subdirectories (max depth 3).";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> pathProp = new LinkedHashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "Directory path to list (relative to working directory, use '.' for root)");
        properties.put("path", pathProp);

        Map<String, Object> recursiveProp = new LinkedHashMap<>();
        recursiveProp.put("type", "boolean");
        recursiveProp.put("description", "Whether to list recursively (default: false, max depth 3)");
        properties.put("recursive", recursiveProp);

        schema.put("properties", properties);
        schema.put("required", new String[]{"path"});
        return schema;
    }

    @Override
    public String execute(JsonObject arguments) {
        String path = arguments.get("path").getAsString();
        boolean recursive = arguments.has("recursive") && arguments.get("recursive").getAsBoolean();
        Path dirPath = workDir.resolve(path);

        if (!Files.exists(dirPath)) {
            return "Error: Directory not found: " + path;
        }
        if (!Files.isDirectory(dirPath)) {
            return "Error: Not a directory: " + path;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Directory: ").append(path).append("\n\n");

            int maxDepth = recursive ? 3 : 1;
            listDir(dirPath, dirPath, sb, 0, maxDepth);
            return sb.toString();
        } catch (Exception e) {
            return "Error listing directory: " + e.getMessage();
        }
    }

    private void listDir(Path root, Path current, StringBuilder sb, int depth, int maxDepth) throws IOException {
        if (depth > maxDepth) return;

        String indent = repeat("  ", depth);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
            List<Path> dirs = new ArrayList<>();
            List<Path> files = new ArrayList<>();

            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (SKIP_DIRS.contains(name) && Files.isDirectory(entry)) continue;
                if (name.startsWith(".")) continue;

                if (Files.isDirectory(entry)) {
                    dirs.add(entry);
                } else {
                    files.add(entry);
                }
            }

            Collections.sort(dirs, Comparator.comparing(p -> p.getFileName().toString()));
            Collections.sort(files, Comparator.comparing(p -> p.getFileName().toString()));

            for (Path dir : dirs) {
                sb.append(indent).append("\uD83D\uDCC1 ").append(dir.getFileName()).append("/\n");
                listDir(root, dir, sb, depth + 1, maxDepth);
            }

            for (Path file : files) {
                long size = Files.size(file);
                String sizeStr = formatSize(size);
                sb.append(indent).append("\uD83D\uDCC4 ").append(file.getFileName())
                  .append(" (").append(sizeStr).append(")\n");
            }
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(str);
        return sb.toString();
    }
}
