package dev.cerios.maugame.mauengine.exception;

public class FullGameCapacityException extends GameException {
    public FullGameCapacityException() {
    }

    public FullGameCapacityException(String message) {
        super(message);
    }

    public FullGameCapacityException(String message, Throwable cause) {
        super(message, cause);
    }
}
