package dev.cerios.mauengine;

import dev.cerios.mauengine.card.CardComparer;
import dev.cerios.mauengine.exception.GameException;
import dev.cerios.mauengine.game.GameCore;
import dev.cerios.mauengine.game.GameCoreImpl;

public class Main {
    public static void main(String[] args) throws GameException {
        GameCore gameCore = new GameCoreImpl(new CardComparer());

        gameCore.registerPlayer("joe");
        gameCore.registerPlayer("klarie");
        gameCore.registerPlayer("rudolf");

        gameCore.start().forEach(System.out::println);
    }
}