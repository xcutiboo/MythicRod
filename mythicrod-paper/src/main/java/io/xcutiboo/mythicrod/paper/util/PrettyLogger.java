package io.xcutiboo.mythicrod.paper.util;

import java.util.logging.Logger;

/**
 * Utility class for pretty, colored console output.
 * Adds ANSI color codes to log messages for better readability.
 */
public class PrettyLogger {
    
    // ANSI Color Codes
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    // Bright/Bold Colors
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_PURPLE = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";
    
    // Styles
    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";
    
    private final Logger logger;
    private final String pluginPrefix;
    
    public PrettyLogger(Logger logger, String pluginName) {
        this.logger = logger;
        this.pluginPrefix = BOLD + BRIGHT_YELLOW + "[" + pluginName + "] " + RESET;
    }
    
    /**
     * Info message with pretty formatting
     */
    public void info(String message) {
        logger.info(pluginPrefix + BRIGHT_WHITE + message + RESET);
    }
    
    /**
     * Success message - green
     */
    public void success(String message) {
        logger.info(pluginPrefix + BRIGHT_GREEN + "✓ " + message + RESET);
    }
    
    /**
     * Warning message - yellow
     */
    public void warning(String message) {
        logger.warning(pluginPrefix + BRIGHT_YELLOW + "⚠ " + message + RESET);
    }
    
    /**
     * Error message - red
     */
    public void error(String message) {
        logger.severe(pluginPrefix + BRIGHT_RED + "✗ " + message + RESET);
    }
    
    /**
     * Debug message - cyan (only if debug mode enabled)
     */
    public void debug(String message) {
        logger.info(pluginPrefix + BRIGHT_CYAN + "[DEBUG] " + message + RESET);
    }
    
    /**
     * Startup message - bold gold
     */
    public void startup(String message) {
        logger.info(pluginPrefix + BOLD + BRIGHT_YELLOW + "▶ " + message + RESET);
    }
    
    /**
     * Shutdown message
     */
    public void shutdown(String message) {
        logger.info(pluginPrefix + BOLD + BRIGHT_PURPLE + "◀ " + message + RESET);
    }
    
    /**
     * Config reload message - blue
     */
    public void config(String message) {
        logger.info(pluginPrefix + BRIGHT_BLUE + "⚙ " + message + RESET);
    }
    
    /**
     * Drop event message - aqua
     */
    public void drop(String playerName, String itemName, int amount) {
        logger.info(pluginPrefix + BRIGHT_CYAN + "🎣 " + playerName + " caught " + amount + "x " + itemName + RESET);
    }
    
    /**
     * Legendary drop event - gold
     */
    public void legendary(String playerName, String itemName, int amount) {
        logger.info(pluginPrefix + BOLD + BRIGHT_YELLOW + "✨ LEGENDARY! " + playerName + " caught " + amount + "x " + itemName + RESET);
    }
    
    /**
     * Formats a section header
     */
    public void header(String title) {
        String line = "═".repeat(50);
        logger.info(pluginPrefix + BOLD + BRIGHT_YELLOW + line + RESET);
        logger.info(pluginPrefix + BOLD + BRIGHT_YELLOW + "  " + title + RESET);
        logger.info(pluginPrefix + BOLD + BRIGHT_YELLOW + line + RESET);
    }
    
    /**
     * Formats a stat line
     */
    public void stat(String label, String value) {
        logger.info(pluginPrefix + "  " + BRIGHT_CYAN + label + ": " + BRIGHT_WHITE + value + RESET);
    }
    
    /**
     * Raw logger access for complex messages
     */
    public Logger getLogger() {
        return logger;
    }
}
