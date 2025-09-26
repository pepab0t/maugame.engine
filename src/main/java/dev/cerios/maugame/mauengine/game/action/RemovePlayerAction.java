package dev.cerios.maugame.mauengine.game.action;

import dev.cerios.maugame.mauengine.player.Player;

public record RemovePlayerAction(Player player, int recycledCards) implements Action {

    public RemovePlayerAction(Player player) {
        this(player, player.getHand().size());
    }

    @Override
    public ActionType getType() {
        return ActionType.REMOVE_PLAYER;
    }
}
