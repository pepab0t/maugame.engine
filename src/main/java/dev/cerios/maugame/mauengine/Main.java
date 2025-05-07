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

        System.out.println(game.start());

        Card cardToPlay = new Card(CardType.NINE, Color.DIAMONDS);
        var move = game.createPlayMove("joe", cardToPlay, Optional.empty());

        System.out.println(move.execute());
        System.out.println(game.createPlayMove("klarie", new Card(CardType.KING, Color.DIAMONDS), Optional.empty()).execute());
        System.out.println(game.createPlayMove("joe", new Card(CardType.KING, Color.HEARTS), Optional.empty()).execute());
    }
}