package io.xcutiboo.mythicrod.exceptions;

public class DropGenerationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public DropGenerationException(String message) {
        super(message);
    }

    public DropGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
