package com.agent.cli.ui;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * 终端 UI 渲染器 - 美观的命令行界面
 */
public class TerminalUI {

    private static final String BANNER =
            "\n" +
            "  \u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557\n" +
            "  \u2551                                              \u2551\n" +
            "  \u2551     \u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2588\u2588\u2588\u2588\u2557 \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2557   \u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557   \u2551\n" +
            "  \u2551    \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2557\u2588\u2588\u2554\u2550\u2550\u2550\u2550\u255D \u2588\u2588\u2554\u2550\u2550\u2550\u2550\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2551\u255A\u2550\u2550\u2588\u2588\u2554\u2550\u2550\u255D   \u2551\n" +
            "  \u2551    \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2551\u2588\u2588\u2551  \u2588\u2588\u2588\u2557\u2588\u2588\u2588\u2588\u2588\u2557  \u2588\u2588\u2554\u2588\u2588\u2588 \u2588\u2588\u2551   \u2588\u2588\u2551      \u2551\n" +
            "  \u2551    \u2588\u2588\u2554\u2550\u2550\u2588\u2588\u2551\u2588\u2588\u2551   \u2588\u2588\u2551\u2588\u2588\u2554\u2550\u2550\u255D  \u2588\u2588\u2551\u255A\u2588\u2588\u2557\u2588\u2588\u2551   \u2588\u2588\u2551      \u2551\n" +
            "  \u2551    \u2588\u2588\u2551  \u2588\u2588\u2551\u255A\u2588\u2588\u2588\u2588\u2588\u2588\u2554\u255D\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2557\u2588\u2588\u2551 \u255A\u2588\u2588\u2588\u2588\u2551   \u2588\u2588\u2551      \u2551\n" +
            "  \u2551    \u255A\u2550\u255D  \u255A\u2550\u255D \u255A\u2550\u2550\u2550\u2550\u2550\u255D \u255A\u2550\u2550\u2550\u2550\u2550\u2550\u255D\u255A\u2550\u255D  \u255A\u2550\u2550\u2550\u2550\u255D   \u255A\u2550\u255D      \u2551\n" +
            "  \u2551                                              \u2551\n" +
            "  \u2551         AI Agent CLI  v1.0.0                 \u2551\n" +
            "  \u2551    Multi-Model \u00B7 RAG \u00B7 Tool-Use             \u2551\n" +
            "  \u255A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255D\n";

    private final boolean colorEnabled;
    private String providerName = "";
    private String modelName = "";

    public TerminalUI() {
        this.colorEnabled = true;
        AnsiConsole.systemInstall();
    }

    public void showBanner(String provider, String model, String workDir) {
        this.providerName = provider;
        this.modelName = model;

        System.out.println(colorize(BANNER, CYAN));

        System.out.println(colorize("  Model:    ", WHITE) + colorize(provider + " / " + model, GREEN));
        System.out.println(colorize("  Work Dir: ", WHITE) + colorize(workDir, YELLOW));
        System.out.println(colorize("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500", BLACK));
        System.out.println(colorize("  Type ", BLACK) + colorize("/help", GREEN) +
                colorize(" for commands, ", BLACK) + colorize("/exit", GREEN) +
                colorize(" to quit", BLACK));
        System.out.println();
    }

    public void showHelp() {
        String help =
                "\n" +
                "  \u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557\n" +
                "  \u2551               Available Commands              \u2551\n" +
                "  \u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563\n" +
                "  \u2551                                               \u2551\n" +
                "  \u2551  /help     Show this help message             \u2551\n" +
                "  \u2551  /clear    Clear conversation history         \u2551\n" +
                "  \u2551  /model    Show current model info            \u2551\n" +
                "  \u2551  /tools    List available tools               \u2551\n" +
                "  \u2551  /rag      Initialize RAG code search         \u2551\n" +
                "  \u2551  /reindex  Re-index codebase for RAG          \u2551\n" +
                "  \u2551  /exit     Exit the application               \u2551\n" +
                "  \u2551                                               \u2551\n" +
                "  \u2551  Tips:                                        \u2551\n" +
                "  \u2551  * Ask questions about your codebase          \u2551\n" +
                "  \u2551  * Request code modifications                 \u2551\n" +
                "  \u2551  * Use /rag to enable semantic code search    \u2551\n" +
                "  \u2551  * Agent can read/write files and run cmds    \u2551\n" +
                "  \u2551                                               \u2551\n" +
                "  \u255A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255D\n";
        System.out.println(colorize(help, CYAN));
    }

    public String getPrompt() {
        return colorize("\u276F ", GREEN, true);
    }

    public void startAssistantMessage() {
        System.out.println();
        System.out.print(colorize("  \u25CF ", MAGENTA));
    }

    public void printStreaming(String text) {
        System.out.print(text);
        System.out.flush();
    }

    public void endAssistantMessage() {
        System.out.println();
        System.out.println();
    }

    public void showThinking() {
        System.out.print(colorize("  \u23F3 Thinking...", BLACK));
        System.out.flush();
    }

    public void showToolCall(String toolName, String arguments) {
        System.out.print("\r" + repeat(" ", 50) + "\r");
        System.out.println(colorize("  \uD83D\uDD27 ", YELLOW) + colorize(toolName, YELLOW, true) +
                colorize("(", BLACK) + truncate(arguments, 80) + colorize(")", BLACK));
    }

    public void showToolResult(String toolName, String result) {
        String truncated = result.length() > 500 ? result.substring(0, 497) + "..." : result;
        String[] lines = truncated.split("\n");
        int maxLines = Math.min(lines.length, 10);

        System.out.println(colorize("  \u250C\u2500", BLACK) + colorize(" Result ", BLACK) + colorize(repeat("\u2500", 40), BLACK));
        for (int i = 0; i < maxLines; i++) {
            String line = lines[i].length() > 90 ? lines[i].substring(0, 87) + "..." : lines[i];
            System.out.println(colorize("  \u2502 ", BLACK) + colorize(line, BLACK));
        }
        if (lines.length > maxLines) {
            System.out.println(colorize("  \u2502 ", BLACK) + colorize("... (" + (lines.length - maxLines) + " more lines)", BLACK));
        }
        System.out.println(colorize("  \u2514" + repeat("\u2500", 49), BLACK));
    }

    public void showInfo(String message) {
        System.out.println(colorize("  \u2139 ", BLUE) + message);
    }

    public void showSuccess(String message) {
        System.out.println(colorize("  \u2713 ", GREEN) + message);
    }

    public void showError(String message) {
        System.out.println(colorize("  \u2717 ", RED, true) + colorize(message, RED));
    }

    public void showWarning(String message) {
        System.out.println(colorize("  \u26A0 ", YELLOW) + colorize(message, YELLOW));
    }

    public void showProgress(String message) {
        System.out.print("\r" + colorize("  \u27F3 ", CYAN) + message);
        System.out.flush();
    }

    public void showGoodbye() {
        System.out.println();
        System.out.println(colorize("  \u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557", CYAN));
        System.out.println(colorize("  \u2551   Goodbye! Happy coding! \uD83D\uDE80         \u2551", CYAN));
        System.out.println(colorize("  \u255A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255D", CYAN));
        System.out.println();
    }

    public void showModelInfo(String provider, String model) {
        System.out.println(colorize("  \u250C\u2500", BLACK) + colorize(" Model Info ", BLACK) + colorize(repeat("\u2500", 36), BLACK));
        System.out.println(colorize("  \u2502 ", BLACK) + "Provider:  " + colorize(provider, GREEN));
        System.out.println(colorize("  \u2502 ", BLACK) + "Model:     " + colorize(model, GREEN));
        System.out.println(colorize("  \u2514" + repeat("\u2500", 49), BLACK));
    }

    private String colorize(String text, Ansi.Color color) {
        return colorize(text, color, false);
    }

    private String colorize(String text, Ansi.Color color, boolean bold) {
        if (!colorEnabled) return text;
        Ansi ansi = ansi().fg(color);
        if (bold) ansi = ansi.a(Ansi.Attribute.INTENSITY_BOLD);
        return ansi.a(text).reset().toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }

    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(str);
        return sb.toString();
    }

    public void cleanup() {
        AnsiConsole.systemUninstall();
    }
}
