package com.agent.cli.agent;

import com.agent.cli.agent.tool.*;
import com.agent.cli.model.ChatMessage;
import com.agent.cli.model.ChatResponse;
import com.agent.cli.provider.LLMProvider;
import com.agent.cli.rag.CodeIndexer;
import com.agent.cli.rag.CodeSearcher;
import com.agent.cli.ui.TerminalUI;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Path;
import java.util.*;

/**
 * Agent 核心 - 管理对话循环、工具调用和 RAG
 */
public class AgentCore {

    private final LLMProvider provider;
    private final TerminalUI ui;
    private final Path workDir;
    private final List<ChatMessage> conversationHistory;
    private final Map<String, AgentTool> tools;
    private final List<ChatMessage.ToolDefinition> toolDefinitions;
    private CodeIndexer codeIndexer;
    private CodeSearcher codeSearcher;
    private boolean ragEnabled = false;

    private static final String SYSTEM_PROMPT =
            "You are a powerful AI coding assistant, integrated with a command-line interface.\n" +
            "You help users with coding tasks including writing, debugging, reviewing, and understanding code.\n\n" +
            "You have access to the following tools:\n" +
            "- read_file: Read file contents\n" +
            "- write_file: Write/create files\n" +
            "- list_dir: List directory contents\n" +
            "- search_code: Search code using regex patterns\n" +
            "- code_search: Semantic code search using RAG (indexed codebase)\n" +
            "- shell_exec: Execute shell commands\n\n" +
            "Guidelines:\n" +
            "1. Always use tools to gather information before answering questions about the codebase\n" +
            "2. When modifying code, read the file first, then use write_file\n" +
            "3. Use code_search (RAG) to find relevant code when exploring unfamiliar codebases\n" +
            "4. Explain your reasoning clearly and concisely\n" +
            "5. When writing code, ensure it's complete and correct\n" +
            "6. Use search_code for precise pattern matching, code_search for conceptual searches\n\n" +
            "Current working directory: %s\n";

    public AgentCore(LLMProvider provider, TerminalUI ui, Path workDir) {
        this.provider = provider;
        this.ui = ui;
        this.workDir = workDir;
        this.conversationHistory = new ArrayList<>();
        this.tools = new LinkedHashMap<>();
        this.toolDefinitions = new ArrayList<>();

        initializeTools();
        initializeConversation();
    }

    private void initializeTools() {
        registerTool(new ReadFileTool(workDir));
        registerTool(new WriteFileTool(workDir));
        registerTool(new ListDirTool(workDir));
        registerTool(new SearchCodeTool(workDir));
        registerTool(new ShellTool(workDir));
    }

    private void registerTool(AgentTool tool) {
        tools.put(tool.getName(), tool);
        toolDefinitions.add(tool.toToolDefinition());
    }

    public void initializeRag() {
        ui.showInfo("\uD83D\uDD0D Indexing codebase for RAG search...");
        try {
            codeIndexer = new CodeIndexer(workDir);
            int fileCount = codeIndexer.indexProject();
            codeSearcher = new CodeSearcher(codeIndexer);
            registerTool(new CodeSearchRagTool(codeSearcher));
            ragEnabled = true;
            ui.showSuccess("\u2705 Indexed " + fileCount + " files for RAG search");
        } catch (Exception e) {
            ui.showError("\u26A0 Failed to initialize RAG: " + e.getMessage());
        }
    }

    private void initializeConversation() {
        conversationHistory.clear();
        conversationHistory.add(ChatMessage.system(
                String.format(SYSTEM_PROMPT, workDir.toAbsolutePath())));
    }

    public void processInput(String userInput) {
        if (userInput.startsWith("/")) {
            handleCommand(userInput);
            return;
        }

        conversationHistory.add(ChatMessage.user(userInput));
        runAgentLoop();
    }

    @SuppressWarnings("unchecked")
    private void runAgentLoop() {
        int maxIterations = 20;
        int iteration = 0;

        while (iteration < maxIterations) {
            iteration++;

            ui.showThinking();

            final StringBuilder fullContent = new StringBuilder();
            final List<ChatMessage.ToolCall>[] pendingToolCalls = new List[]{null};
            final boolean[] completed = {false};
            final Throwable[] error = {null};

            synchronized (completed) {
                provider.chatStream(conversationHistory, toolDefinitions,
                        content -> {
                            if (fullContent.length() == 0) {
                                ui.startAssistantMessage();
                            }
                            fullContent.append(content);
                            ui.printStreaming(content);
                        },
                        response -> {
                            pendingToolCalls[0] = response.getToolCalls();
                            completed[0] = true;
                            synchronized (completed) {
                                completed.notifyAll();
                            }
                        },
                        err -> {
                            error[0] = err;
                            completed[0] = true;
                            synchronized (completed) {
                                completed.notifyAll();
                            }
                        }
                );

                while (!completed[0]) {
                    try {
                        completed.wait(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            if (error[0] != null) {
                ui.showError("Error: " + error[0].getMessage());
                return;
            }

            if (fullContent.length() > 0) {
                ui.endAssistantMessage();
            }

            if (pendingToolCalls[0] != null && !pendingToolCalls[0].isEmpty()) {
                ChatMessage assistantMsg = ChatMessage.assistantWithToolCalls(pendingToolCalls[0]);
                if (fullContent.length() > 0) {
                    assistantMsg.setContent(fullContent.toString());
                }
                conversationHistory.add(assistantMsg);

                for (ChatMessage.ToolCall toolCall : pendingToolCalls[0]) {
                    executeToolCall(toolCall);
                }
            } else {
                conversationHistory.add(ChatMessage.assistant(fullContent.toString()));
                break;
            }
        }

        if (iteration >= maxIterations) {
            ui.showWarning("\u26A0 Reached maximum iterations (" + maxIterations + ")");
        }
    }

    private void executeToolCall(ChatMessage.ToolCall toolCall) {
        String toolName = toolCall.getFunction().getName();
        String argsJson = toolCall.getFunction().getArguments();

        ui.showToolCall(toolName, argsJson);

        AgentTool tool = tools.get(toolName);
        String result;

        if (tool == null) {
            result = "Error: Unknown tool: " + toolName;
        } else {
            try {
                JsonObject args = JsonParser.parseString(argsJson).getAsJsonObject();
                result = tool.execute(args);
            } catch (Exception e) {
                result = "Error executing tool: " + e.getMessage();
            }
        }

        ui.showToolResult(toolName, result);
        conversationHistory.add(ChatMessage.tool(toolCall.getId(), result));
    }

    private void handleCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/help":
                showHelp();
                break;
            case "/clear":
                initializeConversation();
                ui.showSuccess("\uD83D\uDDD1 Conversation cleared");
                break;
            case "/model":
                ui.showInfo("\uD83D\uDCCC Current model: " + provider.getName() + " / " + provider.getModelName());
                break;
            case "/tools":
                showTools();
                break;
            case "/rag":
                if (ragEnabled) {
                    ui.showInfo("\uD83D\uDD0D RAG is enabled (" + tools.size() + " tools available)");
                } else {
                    initializeRag();
                }
                break;
            case "/reindex":
                if (codeIndexer != null) {
                    try { codeIndexer.close(); } catch (Exception e) { /* ignore */ }
                }
                initializeRag();
                break;
            case "/exit":
            case "/quit":
                ui.showGoodbye();
                System.exit(0);
                break;
            default:
                ui.showWarning("Unknown command: " + cmd + ". Type /help for available commands.");
        }
    }

    private void showHelp() {
        ui.showHelp();
    }

    private void showTools() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  Available Tools:\n");
        sb.append("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n");
        for (AgentTool tool : tools.values()) {
            String desc = tool.getDescription();
            int dot = desc.indexOf('.');
            String shortDesc = dot > 0 ? desc.substring(0, dot) : desc;
            sb.append(String.format("  \uD83D\uDD27 %-15s %s%n", tool.getName(), shortDesc));
        }
        ui.showInfo(sb.toString());
    }

    public boolean isRagEnabled() {
        return ragEnabled;
    }

    public int getConversationSize() {
        return conversationHistory.size();
    }
}
