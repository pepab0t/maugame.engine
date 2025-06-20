package dev.cerios.maugame.mauengine;

import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.GameFactory;

public class Main {
    public static void main(String[] args) throws MauEngineBaseException {
        var gameFactory = new GameFactory();
        var game = gameFactory.createGame();

        GameEventListener playerListener = (player, event) -> {
            System.out.println(player.getUsername() + ": got event " + event);
        };

        game.registerPlayer("JOE", playerListener);
        game.registerPlayer("AAA", playerListener);

        game.start();
    }
}