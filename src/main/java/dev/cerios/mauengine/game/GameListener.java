package dev.cerios.mauengine.game;

import dev.cerios.mauengine.game.action.Action;

@FunctionalInterface
public interface GameListener {
    void handle(Action[] playerActions);
}
