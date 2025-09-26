package dev.cerios.maugame.mauengine.exception;

import dev.cerios.maugame.mauengine.player.PlayerStorage;

public class NotSupportedOperation extends RuntimeException {
    public NotSupportedOperation(String message) {
        super(message);
    }

    public NotSupportedOperation(String operation, Class<? extends PlayerStorage> cls) {
        super(String.format("Operation `%s` not supported with the current state (%s).", operation, cls));
    }
}
