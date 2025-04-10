package dev.cerios.mauengine.game;

import dev.cerios.mauengine.entity.GameState;
import dev.cerios.mauengine.exception.GameException;
import dev.cerios.mauengine.exception.PlayerMoveException;
import dev.cerios.mauengine.game.action.Action;
import dev.cerios.mauengine.game.move.DrawMove;
import dev.cerios.mauengine.game.move.PassMove;
import dev.cerios.mauengine.game.move.PlayCardMove;

import java.util.List;

public interface Game {

    String currentPlayer();

    Action registerPlayer(String playerId) throws GameException;

    List<Action> performMove(PlayCardMove move) throws PlayerMoveException;

    List<Action> performMove(DrawMove move) throws PlayerMoveException;

    List<Action> performMove(PassMove move) throws PlayerMoveException;

    Stage getStage();

    GameState getCurrentState();

    List<Action> start() throws GameException;

    enum Stage {
        LOBBY, RUNNING, FINISH;
    }
}
