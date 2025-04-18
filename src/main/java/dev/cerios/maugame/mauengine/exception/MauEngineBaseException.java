package dev.cerios.maugame.mauengine.exception;

public class MauEngineBaseException extends Exception {
    public MauEngineBaseException() {
        super();
    }
    public MauEngineBaseException(String message) {
        super(message);
    }
    public MauEngineBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
