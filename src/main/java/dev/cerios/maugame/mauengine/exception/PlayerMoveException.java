package dev.cerios.maugame.mauengine.exception;

public class PlayerMoveException extends MauEngineBaseException {
    public PlayerMoveException() {
        super();
    }

    public PlayerMoveException(String message) {
        super(message);
    }

    public PlayerMoveException(String message, Throwable cause) {
        super(message, cause);
    }
}
