package dev.cerios.maugame.websocket.service;

import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.Game;
import dev.cerios.maugame.mauengine.game.GameFactory;
import dev.cerios.maugame.mauengine.game.action.Action;
import dev.cerios.maugame.websocket.storage.GameStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameService {

    private final GameStorage gameStorage;
    private final GameFactory gameFactory;
    private final Map<String, WebSocketSession> playerToSession = new HashMap<>();

    public void registerPlayer(String player, WebSocketSession session) throws IOException {
        List<Action> actions = new LinkedList<>();
        try {
            actions.add(lobbyGame.registerPlayer(player));
        } catch (MauEngineBaseException e) {
            throw new IllegalStateException(e);
        }

        var game = gameStorage.addPlayerToLatestGame(player);
        playerToSession.put(player, session);

        if (game.getFreeCapacity() == 0) {
            try {
                actions.addAll(lobbyGame.start());
            } catch (MauEngineBaseException e) {
                throw new RuntimeException(e);
            }
        }
        for (var gamePlayer : lobbyGame.getAllPlayers()) {
            var playerSession = playerToSession.get(gamePlayer);
            if (playerSession == null)
                continue;

            for (var action : actions) {
                playerSession.sendMessage(new TextMessage(action.toString()));
            }
        }
    }
}
