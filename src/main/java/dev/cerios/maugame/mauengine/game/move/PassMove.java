package dev.cerios.maugame.mauengine.game.move;

import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.GameCore;
import dev.cerios.maugame.mauengine.game.action.Action;

import java.util.List;

public record PassMove(GameCore core, String playerId) implements PlayerMove {

    @Override
    public List<Action> execute() throws PlayerMoveException {
        return core.performPass(playerId());
    }
}
