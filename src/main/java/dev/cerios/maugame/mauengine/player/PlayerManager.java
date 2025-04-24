package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.action.PlayerShiftAction;
import dev.cerios.maugame.mauengine.game.action.RegisterAction;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerManager {
    private static final int MAX_PLAYERS = 5;
    private static final int MIN_PLAYERS = 2;

    @Getter
    private final List<String> players = new ArrayList<>(5);
    private final LinkedHashMap<String, List<Card>> playerHands = new LinkedHashMap<>();
    private final AtomicInteger currentPlayerIndex = new AtomicInteger(-1);
    private final Random random = new Random();

    public RegisterAction registerPlayer(String player) throws GameException {
        if (players.size() >= MAX_PLAYERS) {
            throw new GameException("The game has exceeded the maximum number of players.");
        }

        players.add(player);
        playerHands.put(player, new ArrayList<>());
        return new RegisterAction(player);
    }

    public String currentPlayer() throws GameException {
        return getActivePlayerByIndex(currentPlayerIndex.get());
    }

    public List<Card> getPlayerHand(String playerId) throws PlayerMoveException {
        var hand = this.playerHands.get(playerId);
        if (hand == null)
            throw new PlayerMoveException("player not in game: " + playerId);
        return hand;
    }

    public Set<String> removePlayer(final String playerId) {
        var players = playerHands.keySet();
        players.remove(playerId);
        return players;
    }

    public void validateCanStart() throws GameException {
        if (players.size() < MIN_PLAYERS)
            throw new GameException("The game needs at least " + MIN_PLAYERS + " players to start.");
    }

    public Set<String> getActivePlayers() {
        return playerHands.keySet();
    }

    public Map<String, List<Card>> getPlayerHands() {
        return Collections.unmodifiableMap(playerHands);
    }

    public PlayerShiftAction initializePlayer() throws GameException {
        var initValue = random.nextInt(playerHands.size());
        currentPlayerIndex.set(initValue+1);
        return new PlayerShiftAction(getActivePlayerByIndex(initValue));
    }

    public PlayerShiftAction shiftPlayer() throws GameException {
        String nextPlayerId = getActivePlayerByIndex(currentPlayerIndex.getAndIncrement());
        return new PlayerShiftAction(nextPlayerId);
    }

    public String getActivePlayerByIndex(int index) throws GameException {
        if (index == -1)
            throw new GameException("player manager not initialized");
        var keys = playerHands.sequencedKeySet().iterator();
        int indexToPick = index % playerHands.size();
        String key = null;
        for (int i = 0; i <= indexToPick; i++) {
            key = keys.next();
        }
        return key;
    }

    public int getFreeCapacity() {
        return MAX_PLAYERS - players.size();
    }
}
