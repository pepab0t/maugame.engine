package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.action.Action;
import dev.cerios.maugame.mauengine.game.action.RegisterAction;
import dev.cerios.maugame.mauengine.game.action.StartAction;
import dev.cerios.maugame.mauengine.game.move.DrawMove;
import dev.cerios.maugame.mauengine.game.move.PassMove;
import dev.cerios.maugame.mauengine.game.move.PlayCardMove;
import dev.cerios.maugame.mauengine.player.PlayerManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
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

    public PlayCardMove createPlayMove(final String playerId, Card cardToPlay, Optional<Color> nextColor) {
        return nextColor
                .map(color -> new PlayCardMove(core, playerId, cardToPlay, color))
                .orElseGet(() -> new PlayCardMove(core, playerId, cardToPlay));
    }

    public DrawMove createDrawMove(final String playerId, int count) {
        return new DrawMove(core, playerId, count);
    }

    public PassMove createPassMove(final String playerId) {
        return new PassMove(core, playerId);
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

    public Stage getStage() {
        return core.getStage();
    }

    public SequencedSet<String> getAllPlayers() {
        return playerManager.getPlayers();
    }
}
