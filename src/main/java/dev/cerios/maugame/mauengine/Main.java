package dev.cerios.maugame.mauengine;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardType;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.GameFactory;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.cerios.maugame.mauengine.card.CardType.*;
import static dev.cerios.maugame.mauengine.card.Color.*;

public class Main {
    public static void main(String[] args) throws MauEngineBaseException {
        Random random = new Random(111);
        var gameFactory = new GameFactory(random, Executors.newVirtualThreadPerTaskExecutor());
        var game = gameFactory.createGame();

        GameEventListener playerListener = (player, event) -> {
            System.out.println(player.getUsername() + ": got event " + event);
        };

        var player1 = game.registerPlayer("P1", playerListener).getPlayerId();
        var player2 = game.registerPlayer("P2", playerListener).getPlayerId();

        game.start();

        System.out.println("---");
        game.playCardMove(player2, new Card(SEVEN, DIAMONDS));
        System.out.println("---");
        game.playPassMove(player1);
        System.out.println("---");
        game.playCardMove(player2, new Card(SEVEN, SPADES));
        System.out.println("---");
        game.playPassMove(player1);
        System.out.println("---");
        game.playCardMove(player2, new Card(NINE, SPADES));
        System.out.println("---");
        game.playCardMove(player1, new Card(JACK, SPADES));
        System.out.println("---");
        game.playCardMove(player2, new Card(JACK, DIAMONDS));
        System.out.println("---");
        System.out.println(game.getGameState());
    }
}