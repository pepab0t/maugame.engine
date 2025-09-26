package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.exception.GameException;

public interface PlayerReadyStorage extends PlayerStorage {
    void setReady(String playerId) throws GameException;
}
