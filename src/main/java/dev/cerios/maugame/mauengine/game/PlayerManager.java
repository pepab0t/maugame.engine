package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.action.*;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static dev.cerios.maugame.mauengine.game.PlayerIdGenerator.generatePlayerId;

class PlayerManager {
    public static final int MAX_PLAYERS = 2;
    public static final int MIN_PLAYERS = 2;

    @Getter
    private final Map<String, Player> playersById = new HashMap<>();
    private final AtomicInteger currentPlayerIndex = new AtomicInteger(-1);
    @Getter
    private byte activeCounter = 0;
    private final Random random;
    private final List<Player> _players = new ArrayList<>(MAX_PLAYERS);

    public PlayerManager(Random random) {
        this.random = random;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(_players);
    }

    public Player registerPlayer(String username, GameEventListener eventListener) throws GameException {
        if (_players.size() >= MAX_PLAYERS)
            throw new GameException(
                    String.format(
                            "The game has exceeded the maximum number of players (%s).",
                            MAX_PLAYERS
                    )
            );

        var player = new Player(generatePlayerId(), username, eventListener);
        var action = new RegisterAction(player, false);
        _players.stream()
                .filter(Player::isActive)
                .forEach(p -> p.trigger(action));
        _players.add(player);
        player.trigger(new RegisterAction(player, true));
        activeCounter++;
        return player;
    }

    public Player currentPlayer() {
        return _players.get(currentPlayerIndex.get() % _players.size());
    }

    public Player getPlayer(String playerId) throws PlayerMoveException {
        return _players.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new PlayerMoveException(playerId));
    }

    public DeactivateAction deactivatePlayer(final String playerId) {
        var player = playersById.get(playerId);
        if (player == null)
            throw new RuntimeException("player not in game: " + playerId);
        player.disable();
        activeCounter--;
        return new DeactivateAction(playerId);
    }

    public ActivateAction activatePlayer(final String playerId) {
        var player = playersById.get(playerId);
        if (player == null)
            throw new RuntimeException("player not in game: " + playerId);
        player.enable();
        activeCounter++;
        return new ActivateAction(player.getPlayerId());
    }

    public void validateCanStart() throws GameException {
        if (_players.size() < MIN_PLAYERS)
            throw new GameException("The game needs at least " + MIN_PLAYERS + " players to start.");
    }

    public void initializePlayer() throws GameException {
        var initValue = random.nextInt(playersById.size() + 1);
        currentPlayerIndex.set(initValue);
        shiftPlayer();
    }

    public void shiftPlayer() {
        if (activeCounter < 2)
            throw new RuntimeException("There is no next player");
        Player nextPlayer;
        do {
            nextPlayer = _players.get(currentPlayerIndex.incrementAndGet() % _players.size());
        } while (!nextPlayer.isActive());
        var action = new PlayerShiftAction(nextPlayer);
        distributeActionToAll(action);
    }

    public void distributeActionToAll(Action action) {
        distributeAction(action, Player::isActive);
    }

    public void distributeActionExcludingPlayer(Action action, String playerId) {
        distributeAction(action, p -> p.isActive() && !p.getPlayerId().equals(playerId));
    }

    private void distributeAction(
            Action action,
            Predicate<Player> playerPredicate
    ) {
        _players.stream()
                .filter(playerPredicate)
                .forEach(player -> player.trigger(action));
    }

    public int getFreeCapacity() {
        return MAX_PLAYERS - _players.size();
    }
}
