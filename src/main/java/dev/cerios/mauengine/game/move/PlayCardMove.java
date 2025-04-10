package dev.cerios.mauengine.game.move;

import dev.cerios.mauengine.card.Card;
import dev.cerios.mauengine.card.CardType;
import dev.cerios.mauengine.card.Color;
import dev.cerios.mauengine.exception.PlayerMoveException;
import dev.cerios.mauengine.game.GameCore;
import dev.cerios.mauengine.game.action.Action;

import java.util.List;

public record PlayCardMove(GameCore core, String playerId, Card card, Color nextColor) implements PlayerMove {

    public PlayCardMove(GameCore core, String playerId, Card card) {
        this(core, playerId, card, null);
    }

    @Override
    public List<Action> execute() throws PlayerMoveException {
        return card.type() == CardType.QUEEN ?
                core.performPlayCard(playerId, card, nextColor) :
                core.performPlayCard(playerId, card);
    }
}
