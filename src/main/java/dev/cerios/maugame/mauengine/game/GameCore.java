package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.action.Action;

import java.util.List;

public interface GameCore {

    String currentPlayer();

    Action registerPlayer(String playerId) throws GameException;

    List<Action> performPlayCard(String playerId, Card card) throws PlayerMoveException;

    List<Action> performPlayCard(String playerId, Card card, Color nextColor) throws PlayerMoveException;

    List<Action> performDraw(String playerId, int cardCount) throws PlayerMoveException;

    List<Action> performPass(String playerId) throws PlayerMoveException;

    Stage getStage();

    GameState getCurrentState();

    List<Action> start() throws GameException;

    enum Stage {
        LOBBY, RUNNING, FINISH;
    }
}
