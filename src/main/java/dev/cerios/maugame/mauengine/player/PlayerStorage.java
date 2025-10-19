package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.GamePlayer;

import java.util.Collection;

public interface PlayerStorage {
    Player registerPlayer(String username, GameEventListener eventListener) throws GameException;

    void removePlayer(String playerId);

    Player getPlayer(String playerId) throws GameException;

    Collection<Player> getPlayers();

    ActionPublisher getActionPublisher();

    default ActionPublisher createActionPublisher(ActionPublisherBuilder builder) {
        return builder.withPlayers(() -> getPlayers().stream()).build();
    }
}
