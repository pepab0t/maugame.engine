package dev.cerios.maugame.mauengine;

import dev.cerios.maugame.mauengine.card.*;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.Game;
import dev.cerios.maugame.mauengine.game.GameCore;
import dev.cerios.maugame.mauengine.game.GameCoreImpl;

import java.util.Optional;

public class Main {
    public static void main(String[] args) throws GameException, PlayerMoveException {
        GameCore gameCore = new GameCoreImpl(CardManager.create(), new CardComparer());
        Game game = new Game(gameCore);

        gameCore.registerPlayer("joe");
        gameCore.registerPlayer("klarie");
        gameCore.registerPlayer("rudolf");

        gameCore.start().forEach(System.out::println);
        Card cardToPlay = new Card(CardType.JACK, Color.SPADES);
        var move = game.createPlayMove("joe", cardToPlay, Optional.empty());
        move.execute();
    }
}