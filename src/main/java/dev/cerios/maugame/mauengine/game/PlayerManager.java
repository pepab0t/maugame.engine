package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.action.ActivateAction;
import dev.cerios.maugame.mauengine.game.action.DeactivateAction;
import dev.cerios.maugame.mauengine.game.action.PlayerShiftAction;
import dev.cerios.maugame.mauengine.game.action.RegisterAction;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class PlayerManager {
    private static final int MAX_PLAYERS = 2;
    private static final int MIN_PLAYERS = 2;

    private final List<Player> players = new ArrayList<>(5);
    @Getter
    private final Map<String, Player> playersById = new HashMap<>();
    private final AtomicInteger currentPlayerIndex = new AtomicInteger(-1);
    @Getter
    private byte activeCounter = 0;
    private final Random random = new Random(111);

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public RegisterAction registerPlayer(String playerId) throws GameException {
        if (playersById.size() >= MAX_PLAYERS)
            throw new GameException("The game has exceeded the maximum number of players.");

        Player player = new Player(playerId);
        players.add(player);
        playersById.put(playerId, player);
        activeCounter++;
        return new RegisterAction(playerId);
    }

    public Player currentPlayer() {
        return players.get(currentPlayerIndex.get() % players.size());
    }

    public Player getPlayer(String playerId) throws PlayerMoveException {
        var player = this.playersById.get(playerId);
        if (player == null)
            throw new PlayerMoveException("player not in game: " + playerId);
        return player;
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
        if (players.size() < MIN_PLAYERS)
            throw new GameException("The game needs at least " + MIN_PLAYERS + " players to start.");
    }

    public PlayerShiftAction initializePlayer() throws GameException {
        var initValue = random.nextInt(playersById.size() + 1);
        currentPlayerIndex.set(initValue);
        return shiftPlayer();
    }

    public PlayerShiftAction shiftPlayer() {
        if (activeCounter < 2)
            throw new RuntimeException("There is no next player");
        Player nextPlayer;
        do {
            nextPlayer = players.get(currentPlayerIndex.incrementAndGet() % players.size());
        } while (!nextPlayer.isActive());
        return new PlayerShiftAction(nextPlayer.getPlayerId());
    }

    public int getFreeCapacity() {
        return MAX_PLAYERS - players.size();
    }
}
