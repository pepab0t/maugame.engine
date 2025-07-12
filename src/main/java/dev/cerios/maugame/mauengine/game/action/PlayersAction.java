package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.game.Player;

import java.util.List;

public record PlayersAction(List<Player> players) implements Action {

    @Override
    public ActionType getType() {
        return ActionType.PLAYERS;
    }
}
