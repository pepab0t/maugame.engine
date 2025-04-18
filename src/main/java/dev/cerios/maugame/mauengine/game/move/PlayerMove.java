package dev.cerios.maugame.mauengine.game.move;

import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.action.Action;

import java.util.List;

public interface PlayerMove {
    String playerId();
    List<Action> execute() throws PlayerMoveException;
}
