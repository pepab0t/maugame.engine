package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.game.action.Action;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface GameEventListener extends BiConsumer<Player, Action> {
}
