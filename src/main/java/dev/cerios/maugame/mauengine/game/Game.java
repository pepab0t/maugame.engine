package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.game.action.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static dev.cerios.maugame.mauengine.game.Stage.LOBBY;


@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {
    @Getter
    @EqualsAndHashCode.Include
    private final UUID uuid = UUID.randomUUID();
    private final GameCore core;
    private final PlayerManager playerManager;

    public void playCardMove(final String playerId, Card cardToPlay) throws MauEngineBaseException {
        var l = playerManager.writeLock();
        try {
            l.lock();
            playerManager.poke(playerId);
            core.performPlayCard(playerId, cardToPlay);
        } finally {
            l.unlock();
        }
    }

    public void playCardMove(final String playerId, Card cardToPlay, Color nextColor) throws MauEngineBaseException {
        var l = playerManager.writeLock();
        try {
            l.lock();
            playerManager.poke(playerId);
            core.performPlayCard(playerId, cardToPlay, nextColor);
        } finally {
            l.unlock();
        }
    }

    public void playDrawMove(final String playerId) throws MauEngineBaseException {
        var l = playerManager.writeLock();
        try {
            l.lock();
            playerManager.poke(playerId);
            core.performDraw(playerId);
        } finally {
            l.unlock();
        }
    }

    public void playPassMove(final String playerId) throws MauEngineBaseException {
        var l = playerManager.writeLock();
        try {
            l.lock();
            playerManager.poke(playerId);
            core.performPass(playerId);
        } finally {
            l.unlock();
        }
    }

    public String registerPlayer(String username, final GameEventListener eventListener) throws GameException {
        var l = playerManager.writeLock();
        try {
            l.lock();
            if (core.getStage() != LOBBY) {
                throw new GameException("The game has already started.");
            }
            return playerManager.registerPlayer(username, eventListener).getPlayerId();
        } finally {
            l.unlock();
        }
    }

    public void removePlayer(String playerId) throws GameException {
        var l = playerManager.writeLock();
        try {
            l.lock();
            playerManager.removePlayer(playerId);
        } finally {
            l.unlock();
        }
    }

    public GameState getGameState() {
        var l = playerManager.readLock();
        try {
            l.lock();
            return new GameState(
                    playerManager.getPlayerRank().stream().toList(),
                    playerManager.getPlayers().stream().collect(
                            HashMap::new,
                            (map, player) -> map.put(player.getUsername(), player.getHand()),
                            HashMap::putAll
                    ),
                    core.getPileCard(),
                    core.getDeckSize(),
                    core.getStage(),
                    playerManager.currentPlayer().getUsername(),
                    core.getGameEffect()
            );
        } finally {
            l.unlock();
        }
    }

    public int getFreeCapacity() {
        return playerManager.getFreeCapacity();
    }

    public void start() throws MauEngineBaseException {
        var l = playerManager.writeLock();
        try {
            l.lock();
            var pileCard = core.start();

            playerManager.distributeActionToAll(new StartAction(uuid.toString()));
            playerManager.distributeActionToAll(new StartPileAction(pileCard));
            playerManager.initializePlayer();

            for (Player player : playerManager.getPlayers()) {
                player.trigger(new DrawAction(player.getHand()));
                playerManager.distributeActionExcludingPlayer(
                        new HiddenDrawAction(player, (byte) player.getHand().size()),
                        player.getPlayerId()
                );
            }
        } finally {
            l.unlock();
        }
    }

    public Player getPlayer(String playerId) throws GameException {
        return playerManager.getPlayer(playerId);
    }

    public Stage getStage() {
        var l = playerManager.readLock();
        try {
            l.lock();
            return core.getStage();
        } finally {
            l.unlock();
        }
    }

    public List<Player> getAllPlayers() {
        return playerManager.getPlayers();
    }

    public void sendCurrentStateTo(String playerId, Predicate<Player> playerMatcher) throws GameException {
        var l = playerManager.readLock();
        try {
            l.lock();
            var player = playerManager.getPlayer(playerId);
            if (!playerMatcher.test(player))
                throw new GameException("No matching player.");
            List<Action> actions = new LinkedList<>();

            actions.add(new StartAction(getUuid().toString()));
            actions.add(new StartPileAction(core.getPileCard()));
            for (Player p : playerManager.getPlayers()) {
                if (p.getPlayerId().equals(playerId))
                    actions.add(new DrawAction(p.getHand()));
                else
                    actions.add(new HiddenDrawAction(p, p.getHand().size()));
            }
            actions.add(new PlayerShiftAction(playerManager.currentPlayer(), playerManager.getLastExpire(playerId)));
            actions.add(new SendRankAction(playerManager.getPlayerRank()));

            actions.forEach(player::trigger);
        } finally {
            l.unlock();
        }
    }
}
