package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.game.action.Action;
import dev.cerios.maugame.mauengine.player.Player;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface GameEventListener extends BiConsumer<Player, Action> {
}
