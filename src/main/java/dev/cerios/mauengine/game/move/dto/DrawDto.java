package dev.cerios.mauengine.game.move.dto;

import dev.cerios.mauengine.exception.InvalidMoveException;

public record DrawDto(String playerId, int count) implements MoveDto {

    public void validate() throws InvalidMoveException {
        if (count != 1)
            throw new InvalidMoveException(String.format("Invalid draw count: %d", count));
    }
}
