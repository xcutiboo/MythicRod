package io.xcutiboo.mythicrod.exceptions;

public class DropGenerationException extends RuntimeException {
    public DropGenerationException(String message) {
        super(message);
    }

    public DropGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
