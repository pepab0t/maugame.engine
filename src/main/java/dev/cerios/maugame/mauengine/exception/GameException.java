package dev.cerios.maugame.mauengine.exception;

public class GameException extends MauEngineBaseException {
    public GameException() {
    }

    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
