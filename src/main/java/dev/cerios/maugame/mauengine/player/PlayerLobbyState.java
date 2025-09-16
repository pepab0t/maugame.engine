package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.action.PlayersAction;
import dev.cerios.maugame.mauengine.game.action.ReadyAction;
import dev.cerios.maugame.mauengine.game.action.RegisterAction;
import dev.cerios.maugame.mauengine.game.action.UnreadyAction;
import lombok.Getter;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.*;
import java.util.function.Consumer;

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

        for (var r : readyStates.values()) {
            r.set(false);
            actionPublisher.publishActionToAll(new UnreadyAction(r.getPlayer().getUsername()));
        }
        actionPublisher.publishActionToAll(new RegisterAction(gameId, player, false));

        usernames.add(username);
        players.put(player.getPlayerId(), player);
        readyStates.put(player.getPlayerId(), new Ready(player));

        actionPublisher.publishAction(player, new RegisterAction(gameId, player, true));
        actionPublisher.publishAction(player, new PlayersAction(getPlayers()));
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
            ready.set(false);
            actionPublisher.publishActionToAll(new UnreadyAction(ready.getPlayer().getUsername()));
        }
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

        ready.set(true);
        actionPublisher.publishActionToAll(new ReadyAction(ready.getPlayer().getUsername()));

        // at least one is not ready
        if (!hasEnoughPlayers() || readyStates.values().stream().anyMatch(r -> !r.get()))
            return;

        triggerStart();
        stateSwitcher.accept(getPlayers());
    }

    public void listenStart(Consumer<UUID> listener) {
        startListeners.add(listener);
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
