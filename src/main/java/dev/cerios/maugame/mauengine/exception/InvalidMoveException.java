package dev.cerios.maugame.mauengine.exception;

public class InvalidMoveException extends MauEngineBaseException {
    public InvalidMoveException() {
    }

    public InvalidMoveException(String message) {
        super(message);
    }

    public InvalidMoveException(String message, Throwable cause) {
        super(message, cause);
    }
}
