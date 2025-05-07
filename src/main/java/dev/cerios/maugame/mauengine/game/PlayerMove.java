package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.action.Action;

import java.util.List;

@FunctionalInterface
public interface PlayerMove {
    List<Action> execute() throws MauEngineBaseException;
}
