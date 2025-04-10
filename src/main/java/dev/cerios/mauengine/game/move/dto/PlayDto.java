package dev.cerios.mauengine.game.move.dto;

import dev.cerios.mauengine.card.Card;
import dev.cerios.mauengine.card.CardType;
import dev.cerios.mauengine.card.Color;
import dev.cerios.mauengine.exception.InvalidMoveException;
import lombok.Data;

@Data
public final class PlayDto implements MoveDto {
    private final String playerId;
    private final Card card;
    private Color nextColor;

    @Override
    public String playerId() {
        return playerId;
    }

    public void validate() throws InvalidMoveException {
        if (card.type() == CardType.QUEEN && nextColor == null)
            throw new InvalidMoveException("When playing QUEEN, need to specify next color.");
    }
}
