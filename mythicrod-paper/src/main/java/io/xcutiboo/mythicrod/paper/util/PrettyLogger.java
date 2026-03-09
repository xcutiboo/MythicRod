package io.xcutiboo.mythicrod.paper.util;

import java.util.logging.Logger;

/// Small wrapper for MythicRod's startup/status console lines.
public class PrettyLogger {

    public static final String RESET = "\u001B[0m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_WHITE = "\u001B[97m";
    public static final String BOLD = "\u001B[1m";

    private final Logger logger;
    public PrettyLogger(Logger logger) {
        this.logger = logger;
    }

    public void info(String message) {
        logger.info(() -> BRIGHT_WHITE + message + RESET);
    }

    public void startup(String message) {
        logger.info(() -> BOLD + BRIGHT_YELLOW + "▶ " + message + RESET);
    }
}
