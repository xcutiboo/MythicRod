package io.xcutiboo.mythicrod.exceptions;

public class MythicRodConfigurationException extends RuntimeException {
    public MythicRodConfigurationException(String message) {
        super(message);
    }

    public MythicRodConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
