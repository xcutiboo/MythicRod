package io.xcutiboo.mythicrod.exceptions;

public class MythicRodConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public MythicRodConfigurationException(String message) {
        super(message);
    }

    public MythicRodConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
