package dev.cerios.maugame.mauengine.game.move;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardType;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.GameCore;
import dev.cerios.maugame.mauengine.game.action.Action;

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
