package dev.cerios.mauengine.game.move;

import dev.cerios.mauengine.exception.PlayerMoveException;
import dev.cerios.mauengine.game.action.Action;

import java.util.List;

public interface PlayerMove {
    String playerId();
    List<Action> execute() throws PlayerMoveException;
}
