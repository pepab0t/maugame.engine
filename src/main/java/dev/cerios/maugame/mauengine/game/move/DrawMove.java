package dev.cerios.maugame.mauengine.game.move;

import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.GameCore;
import dev.cerios.maugame.mauengine.game.action.Action;

import java.util.List;

public record DrawMove(GameCore core, String playerId, int count) implements PlayerMove {

    @Override
    public List<Action> execute() throws PlayerMoveException {
        return core.performDraw(playerId(), count());
    }
}