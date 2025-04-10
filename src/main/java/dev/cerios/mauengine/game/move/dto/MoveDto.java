package dev.cerios.mauengine.game.move.dto;

import dev.cerios.mauengine.exception.InvalidMoveException;

public sealed interface MoveDto permits DrawDto, PlayDto, PassDto {
    String playerId();

    default void validate() throws InvalidMoveException {
    }
}
