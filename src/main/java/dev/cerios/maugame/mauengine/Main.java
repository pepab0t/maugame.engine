package dev.cerios.maugame.mauengine;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardType;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.GameFactory;

import java.util.Random;

import static dev.cerios.maugame.mauengine.card.CardType.*;
import static dev.cerios.maugame.mauengine.card.Color.*;

public class Main {
    public static void main(String[] args) throws MauEngineBaseException {
        Random random = new Random(1);
        var gameFactory = new GameFactory(random);
        var game = gameFactory.createGame();

        GameEventListener playerListener = (player, event) -> {
            System.out.println(player.getUsername() + ": got event " + event);
        };

        var player1 = game.registerPlayer("P1", playerListener);
        var player2 = game.registerPlayer("P2", playerListener);

        game.start();

        System.out.println("---");
        game.playCardMove(player2.getPlayerId(), new Card(TEN, SPADES));
        System.out.println("---");
        game.playCardMove(player1.getPlayerId(), new Card(TEN, CLUBS));
        System.out.println("---");
        game.playDrawMove(player2.getPlayerId(), 1);
        System.out.println("---");
    }
}