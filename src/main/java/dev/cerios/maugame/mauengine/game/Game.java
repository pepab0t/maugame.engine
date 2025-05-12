package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.action.Action;
import dev.cerios.maugame.mauengine.game.action.RegisterAction;
import dev.cerios.maugame.mauengine.game.action.StartAction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.cerios.maugame.mauengine.game.Stage.LOBBY;


@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {
    @Getter
    @EqualsAndHashCode.Include
    private final UUID uuid = UUID.randomUUID();
    private final GameCore core;
    private final PlayerManager playerManager;

    public PlayerMove createPlayMove(final String playerId, Card cardToPlay, Optional<Color> nextColor) {
        return () -> {
            if (nextColor.isPresent()) {
                return core.performPlayCard(playerId, cardToPlay, nextColor.get());
            }
            return core.performPlayCard(playerId, cardToPlay);
        };
    }

    public PlayerMove createDrawMove(final String playerId, int count) {
        return () -> core.performDraw(playerId, count);
    }

    public PlayerMove createPassMove(final String playerId) {
        return () -> core.performPass(playerId);
    }

    public RegisterAction registerPlayer(final String playerId) throws GameException {
        if (core.getStage() != LOBBY) {
            throw new GameException("The game has already started.");
        }
        return playerManager.registerPlayer(playerId);
    }

    public int getFreeCapacity() {
        return playerManager.getFreeCapacity();
    }

    public List<Action> start() throws MauEngineBaseException {
        var actions = core.start();
        actions.add(new StartAction(uuid.toString()));
        return actions;
    }

    public void activatePlayer(String playerId) {
        playerManager.activatePlayer(playerId);
    }

    public void deactivatePlayer(String playerId) {
        playerManager.deactivatePlayer(playerId);
    }

    public Stage getStage() {
        return core.getStage();
    }

    public List<String> getAllPlayers() {
        return playerManager.getPlayers().stream()
                .map(Player::getPlayerId)
                .toList();
    }
}
