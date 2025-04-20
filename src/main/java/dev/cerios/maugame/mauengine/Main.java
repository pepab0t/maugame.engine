package dev.cerios.maugame.mauengine;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardType;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.Game;
import dev.cerios.maugame.mauengine.game.GameFactory;

import java.util.Optional;

public class Main {
    public static void main(String[] args) throws MauEngineBaseException {
        GameFactory gameFactory = new GameFactory();
        Game game = gameFactory.createGame();

        game.registerPlayer("joe");
        game.registerPlayer("klarie");
        game.registerPlayer("rudolf");

        game.start();

        Card cardToPlay = new Card(CardType.JACK, Color.SPADES);
        var move = game.createPlayMove("joe", cardToPlay, Optional.empty());
        move.execute();
    }
}