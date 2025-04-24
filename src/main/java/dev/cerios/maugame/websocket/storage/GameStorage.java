package dev.cerios.maugame.websocket.storage;

import dev.cerios.maugame.mauengine.game.Game;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GameStorage {
    private final Map<String, Game> playerToGame = new HashMap<>();

    public void addPlayerToGame(String player, Game game) {
        if (playerToGame.containsKey(player)) {
            throw new IllegalArgumentException("Player " + player + " already exists");
        }
        playerToGame.put(player, game);
    }

    public Game getPlayersGame(String player) {
        Game game = playerToGame.get(player);
        if (game == null)
            throw new IllegalArgumentException("Player " + player + " not found");
        return game;
    }

}
