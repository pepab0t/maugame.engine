package dev.cerios.maugame.mauengine.exception;

public class PlayerNotActiveException extends PlayerMoveException {
    public PlayerNotActiveException(final String playerId) {
        super(String.format("It's not %s's turn.", playerId));
    }
}
