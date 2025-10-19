package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.NotSupportedOperation;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.action.DestroyAction;
import dev.cerios.maugame.mauengine.game.action.ReadyAction;
import dev.cerios.maugame.mauengine.game.action.RemovePlayerAction;
import dev.cerios.maugame.mauengine.game.action.UnreadyAction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class PlayerFinishState implements PlayerReadyStorage {
    private final UUID gameId;
    private final int minPlayers;

    private final ListOrderedMap<String, Player> players = new ListOrderedMap<>();
    private final Map<String, Ready> readyStates = new HashMap<>();

    private final List<Consumer<UUID>> startListeners = new LinkedList<>();

    @Getter
    private final ActionPublisher actionPublisher;
    private final Consumer<Collection<Player>> stateSwitcher;


    PlayerFinishState(Collection<Player> players, int minPlayers, UUID gameId, ActionPublisherBuilder builder, Consumer<Collection<Player>> stateSwitcher) {
        this.minPlayers = minPlayers;
        this.gameId = gameId;
        this.actionPublisher = createActionPublisher(builder);
        this.stateSwitcher = stateSwitcher;
        for (Player player : players) {
            this.players.put(player.getPlayerId(), player);
            this.readyStates.put(player.getPlayerId(), new Ready(player));
        }
    }

    @Override
    public Player registerPlayer(String username, GameEventListener eventListener) {
        throw new NotSupportedOperation("Cannot register during finish state.");
    }

    @Override
    public void removePlayer(String playerId) {
        var player = players.remove(playerId);
        if (player == null) return;

        readyStates.remove(playerId);
        actionPublisher.publishActionToAll(new RemovePlayerAction(player));

        if (players.size() <= minPlayers) {
            actionPublisher.publishActionToAll(new DestroyAction(gameId));
            players.clear();
            readyStates.clear();
        } else {
            for (var ready : readyStates.values()) {
                if (ready.set(false))
                    actionPublisher.publishActionToAll(new UnreadyAction(ready.getPlayer().getUsername()));
            }
        }
    }

    @Override
    public Player getPlayer(String playerId) throws GameException {
        var p = players.get(playerId);
        if (p == null) throw new GameException("Player " + playerId + " not found.");
        return p;
    }

    @Override
    public List<Player> getPlayers() {
        return players.valueList();
    }

    @Override
    public void setReady(String playerId) throws GameException {
        var ready = readyStates.get(playerId);
        if (ready == null) {
            throw new GameException("Player " + playerId + " not found.");
        }

        if (!ready.set(true)) {
            log.trace("game {}: players {} ready status true not changed", gameId, playerId);
            return;
        }
        actionPublisher.publishActionToAll(new ReadyAction(ready.getPlayer().getUsername()));

        if (!hasEnoughPlayers() || readyStates.values().stream().anyMatch(r -> !r.get())) return;

        triggerStart();
        stateSwitcher.accept(getPlayers());
    }

    public void listenStart(Consumer<UUID> startListener) {
        startListeners.add(startListener);
    }

    public void listenStart(List<Consumer<UUID>> startListeners) {
        this.startListeners.addAll(startListeners);
    }

    private void triggerStart() {
        for (var listener : startListeners) {
            listener.accept(gameId);
        }
    }

    private boolean hasEnoughPlayers() {
        return players.size() >= minPlayers;
    }
}
