package dev.cerios.mauengine;

import dev.cerios.mauengine.card.CardComparer;
import dev.cerios.mauengine.exception.GameException;
import dev.cerios.mauengine.game.Game;
import dev.cerios.mauengine.game.GameImpl;

public class Main {
    public static void main(String[] args) throws GameException {
        Game game = new GameImpl(new CardComparer());

        game.registerPlayer("joe");
        game.registerPlayer("klarie");
        game.registerPlayer("rudolf");

        game.start().forEach(System.out::println);
    }
}