package dev.cerios.mauengine.game.move;

import dev.cerios.mauengine.exception.PlayerMoveException;
import dev.cerios.mauengine.game.GameCore;
import dev.cerios.mauengine.game.action.Action;

import java.util.List;

public record PassMove(GameCore core, String playerId) implements PlayerMove {

    @Override
    public List<Action> execute() throws PlayerMoveException {
        return core.performPass(playerId());
    }
}
