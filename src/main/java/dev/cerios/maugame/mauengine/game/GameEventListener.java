package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.game.action.Action;

import java.util.function.BiConsumer;

public interface GameEventListener extends BiConsumer<String, Action> {
}
