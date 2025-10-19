package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.action.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class PlayerLobbyState implements PlayerReadyStorage {
    private final int minPlayers;
    private final int maxPlayers;

    private final ListOrderedMap<String, Player> players = new ListOrderedMap<>();
    private final Set<String> usernames = new HashSet<>();
    private final Map<String, Ready> readyStates = new HashMap<>();

    private final UUID gameId;
    @Getter
    private final ActionPublisher actionPublisher;
    private final Consumer<Collection<Player>> stateSwitcher;

    private final List<Consumer<UUID>> startListeners = new LinkedList<>();

    PlayerLobbyState(UUID gameId, Consumer<Collection<Player>> stateSwitcher, ActionPublisherBuilder publisherBuilder) {
        this(2, 5, gameId, stateSwitcher, publisherBuilder);
    }

    PlayerLobbyState(
            int minPlayers,
            int maxPlayers,
            UUID gameId,
            Consumer<Collection<Player>> stateSwitcher,
            ActionPublisherBuilder builder
    ) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.actionPublisher = createActionPublisher(builder);
        this.gameId = gameId;
        this.stateSwitcher = stateSwitcher;
    }

    @Override
    public Player registerPlayer(String username, GameEventListener eventListener) throws GameException {
        if (usernames.contains(username))
            throw new GameException("Username `" + username + "` is given");
        if (players.size() >= maxPlayers) {
            throw new GameException("Too many players");
        }
        var player = new Player(PlayerIdGenerator.generatePlayerId(), username, eventListener);
        var playerId = player.getPlayerId();

        usernames.add(username);
        players.put(playerId, player);
        readyStates.put(playerId, new Ready(player));

        actionPublisher.publishActionExcludingPlayer(new RegisterAction(gameId, player, false), playerId);
        actionPublisher.publishAction(player, new RegisterAction(gameId, player, true));
        actionPublisher.publishAction(player, new PlayersAction(getPlayers()));
        for (var r : readyStates.values()) {
            if (r.set(false)) actionPublisher.publishActionExcludingPlayer(new UnreadyAction(r.getPlayer().getUsername()), playerId);
        }
        return player;
    }

    @Override
    public void removePlayer(String playerId) {
        var player = players.remove(playerId);
        if (player == null)
            return;

        usernames.remove(playerId);
        readyStates.remove(playerId);

        for (var ready : readyStates.values()) {
            if (ready.set(false))
                actionPublisher.publishActionToAll(new UnreadyAction(ready.getPlayer().getUsername()));
        }
        actionPublisher.publishActionToAll(new RemovePlayerAction(player, 0));
    }

    @Override
    public Player getPlayer(String playerId) throws GameException {
        var player = players.get(playerId);
        if (player == null)
            throw new GameException("Player `" + playerId + "` not found");
        return player;
    }

    @Override
    public List<Player> getPlayers() {
        return players.valueList();
    }

    @Override
    public void setReady(String playerId) throws GameException {
        var ready = readyStates.get(playerId);
        if (ready == null)
            throw new GameException("Player `" + playerId + "` not found");

        if (!ready.set(true)){
            log.trace("game {}: player `{}` ready status true not changed", gameId, playerId);
            return;
        }

        actionPublisher.publishActionToAll(new ReadyAction(ready.getPlayer().getUsername()));

        log.debug("{}: {} ready", gameId, ready.getPlayer());

        // at least one is not ready
        if (!hasEnoughPlayers() || readyStates.values().stream().anyMatch(r -> !r.get()))
            return;

        log.debug("game {} ready to start", gameId);

        triggerStart();
        stateSwitcher.accept(getPlayers());
    }

    public void listenStart(Consumer<UUID> listener) {
        startListeners.add(listener);
    }

    public void listenStart(List<Consumer<UUID>> listeners) {
        startListeners.addAll(listeners);
    }

    private void triggerStart() {
        System.out.println(startListeners);
        for (var listener : startListeners) {
            listener.accept(gameId);
        }
    }

    public int getFreeCapacity() {
        return maxPlayers - players.size();
    }

    private boolean hasEnoughPlayers() {
        return players.size() >= minPlayers;
    }

}
