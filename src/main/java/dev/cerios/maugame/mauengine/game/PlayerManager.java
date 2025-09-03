package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.CardManager;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.action.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static dev.cerios.maugame.mauengine.game.PlayerIdGenerator.generatePlayerId;

@Slf4j
class PlayerManager {
    private final UUID gameId;
    public final int MAX_PLAYERS;
    public final int MIN_PLAYERS;

    private final static int defaultPlayerIndex = -2;

    private final AtomicInteger currentPlayerIndex = new AtomicInteger(defaultPlayerIndex);
    private final AtomicInteger activeCounter = new AtomicInteger(0);

    private final List<Player> players;
    private final List<String> playerRank = new LinkedList<>();
    private final List<String> removedPlayers = new LinkedList<>();
    private final Map<String, FutureWithTimeout> futures = new HashMap<>();

    private final Random random;
    private final ScheduledExecutorService executor;
    private final AtomicReference<Stage> stage;
    private final CardManager cardManager;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final long turnTimeoutMs = 61_000;

    /**
     * initiates with maxPlayers = minPlayers = 2
     */
    public PlayerManager(UUID gameId, Random random, AtomicReference<Stage> stage, CardManager cardManager) {
        this(gameId, random, 2, 2,  stage, cardManager);
    }

    public PlayerManager(
            UUID gameId,
            Random random,
            int minPlayers,
            int maxPlayers,
            AtomicReference<Stage> stage,
            CardManager cardManager
    ) {
        if (minPlayers > maxPlayers)
            throw new IllegalArgumentException("Min players must be less than max players.");
        this.gameId = gameId;
        this.random = random;
        this.MIN_PLAYERS = minPlayers;
        this.MAX_PLAYERS = maxPlayers;
        this.players = new ArrayList<>(MAX_PLAYERS);
        this.executor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
        this.stage = stage;
        this.cardManager = cardManager;
    }

    record FutureWithTimeout(Future<?> future, long expireAtMs) {
        public void cancel() {
            future.cancel(true);
        }
    }

    public List<Player> getPlayers() {
        var l = lock.readLock();
        try {
            l.lock();
            return Collections.unmodifiableList(players);
        } finally {
            l.unlock();
        }
    }

    public Player registerPlayer(String username, GameEventListener eventListener) throws GameException {
        var l = lock.writeLock();
        try {
            l.lock();
            if (players.size() >= MAX_PLAYERS)
                throw new GameException(
                        String.format(
                                "The game has exceeded the maximum number of players (%s).",
                                MAX_PLAYERS
                        )
                );

            if (players.stream().anyMatch(p -> p.getUsername().equals(username)))
                throw new GameException(String.format("Player %s is already registered.", username));

            var player = new Player(generatePlayerId(), username, eventListener, activeCounter::decrementAndGet);
            players.add(player);
            activeCounter.incrementAndGet();
            distributeActionExcludingPlayer(new RegisterAction(gameId, player, false), player.getPlayerId());
            player.trigger(new RegisterAction(gameId, player, true));
            player.trigger(new PlayersAction(new ArrayList<>(players)));
            return player;
        } finally {
            l.unlock();
        }
    }

    public void removePlayer(String playerId) throws GameException {
        Player removedPlayer;
        var l = lock.writeLock();
        try {
            l.lock();
            var removeIndex = IntStream.range(0, players.size())
                    .filter(index -> players.get(index).getPlayerId().equals(playerId))
                    .findFirst()
                    .orElseThrow(() -> new GameException("Player " + playerId + "not in game."));

            removedPlayer = players.remove(removeIndex);
            removedPlayer.deactivate();
            var hand = removedPlayer.getHand();
            cardManager.addToDeck(hand);
            distributeActionToAll(new RemovePlayerAction(removedPlayer, hand.size()));
            hand.clear();
            if (stage.get() != Stage.RUNNING)
                return;
            removedPlayers.addFirst(removedPlayer.getUsername());
            removedPlayer.trigger(new DisqualifiedAction());

            // if pm has at least 2 active players
            if (activeCounter.get() > 1) {
                shiftPlayer(false);
                return;
            }

            if (activeCounter.get() == 1)
                playerWin(findNextPlayer());
        } finally {
            l.unlock();
        }
    }

    private boolean isInitialized() {
        return currentPlayerIndex.get() > defaultPlayerIndex;
    }

    public Player currentPlayer() {
        var l = lock.readLock();
        try {
            l.lock();
            var currentIndex = currentPlayerIndex.get();
            if (currentIndex == defaultPlayerIndex)
                throw new RuntimeException("Player manager has not been initialized yet.");
            return players.get(currentIndex % players.size());
        } finally {
            l.unlock();
        }
    }

    public Player getPlayer(String playerId) throws GameException {
        var l = lock.readLock();
        try {
            l.lock();
            return players.stream()
                    .filter(p -> p.getPlayerId().equals(playerId))
                    .findFirst()
                    .orElseThrow(() -> new GameException("No player with id `" + playerId + "` was found."));

        } finally {
            l.unlock();
        }
    }

    /**
     * Make player a winner and deactivates him.
     *
     * @param player to transform to winner
     * @return whether game should continue
     */
    public boolean playerWin(Player player) {
        var l = lock.writeLock();
        try {
            l.lock();
            player.deactivate();
            playerRank.add(player.getUsername());

            var gameContinues = activeCounter.get() > 1;
            if (gameContinues)
                distributeActionToAll(new SendRankAction(getPlayerRank()));
            else {
                if (activeCounter.get() == 1) {
                    loseLastActivePlayer();
                }
                stage.set(Stage.FINISH);
                distributeActionToAll(new EndAction(getPlayerRank()));
            }
            return gameContinues;
        } finally {
            l.unlock();
        }
    }

    public void validateCanStart() throws GameException {
        var l = lock.readLock();
        try {
            l.lock();
            if (activeCounter.get() < MIN_PLAYERS)
                throw new GameException("The game needs at least " + MIN_PLAYERS + " active players to start.");
        } finally {
            l.unlock();
        }
    }

    public void initializePlayer() {
        var l = lock.writeLock();
        try {
            l.lock();
            currentPlayerIndex.set(random.nextInt(players.size()) - 1);
            shiftPlayer();
        } finally {
            l.unlock();
        }
    }

    public void shiftPlayer() {
        shiftPlayer(true);
    }

    private void shiftPlayer(boolean increment) {
        var l = lock.writeLock();
        try {
            l.lock();
            if (increment)
                currentPlayerIndex.incrementAndGet();
            var nextPlayer = findNextPlayer();
            var expireTime = System.currentTimeMillis() + turnTimeoutMs;
            var timeoutFuture = executor.schedule(() -> {
                try {
                    removePlayer(nextPlayer.getPlayerId());
                } catch (GameException e) {
                    log.warn(e.getMessage(), e);
                }
            }, turnTimeoutMs, TimeUnit.MILLISECONDS);
            futures.put(nextPlayer.getPlayerId(), new FutureWithTimeout(timeoutFuture, expireTime));
            var action = new PlayerShiftAction(nextPlayer, expireTime);
            distributeActionToAll(action);
        } finally {
            l.unlock();
        }
    }

    public void distributeActionToAll(Action action) {
        distributeAction(action, null);
    }

    public void poke(String playerId) {
        Optional.ofNullable(futures.remove(playerId)).ifPresent(FutureWithTimeout::cancel);
    }

    /**
     * retrieves last turn expire time if present, or {@code -1}
     *
     * @param playerId
     * @return last expire time if possible
     */
    public long getLastExpire(String playerId) {
        var l = lock.readLock();
        try {
            l.lock();
            return Optional.ofNullable(futures.get(playerId))
                    .map(FutureWithTimeout::expireAtMs)
                    .orElse(-1L);
        } finally {
            l.unlock();
        }
    }

    public Lock writeLock() {
        return lock.writeLock();
    }

    public Lock readLock() {
        return lock.readLock();
    }

    public void distributeActionExcludingPlayer(Action action, String playerId) {
        distributeAction(action, p -> !p.getPlayerId().equals(playerId));
    }

    private void distributeAction(
            Action action,
            Predicate<Player> playerPredicate
    ) {
        var l = lock.readLock();
        try {
            l.lock();
            var s = players.stream();
            if (playerPredicate != null)
                s = s.filter(playerPredicate);
            s.forEach(player -> player.trigger(action));
        } finally {
            l.unlock();
        }
    }

    public int getFreeCapacity() {
        var l = lock.readLock();
        try {
            l.lock();
            return MAX_PLAYERS - players.size();
        } finally {
            l.unlock();
        }
    }

    public boolean hasEnoughPlayers() {
        var l = lock.readLock();
        try {
            l.lock();
            return isInitialized() || players.size() >= MIN_PLAYERS;
        } finally {
            l.unlock();
        }
    }

    public List<String> getPlayerRank() {
        var l = lock.readLock();
        try {
            l.lock();
            var out = new LinkedList<>(playerRank);
            if (!out.isEmpty())
                out.addAll(removedPlayers);
            return Collections.unmodifiableList(out);
        } finally {
            l.unlock();
        }
    }

    public int getActiveCounter() {
        return activeCounter.get();
    }

    private Player findNextPlayer() {
        if (activeCounter.get() < 1)
            throw new RuntimeException("There is no next player");
        Player nextPlayer = players.get(currentPlayerIndex.get() % players.size());
        while (nextPlayer.isFinished()) {
            nextPlayer = players.get(currentPlayerIndex.incrementAndGet() % players.size());
        }
        return nextPlayer;
    }

    private void loseLastActivePlayer() {
        var losingPlayer = findNextPlayer();
        losingPlayer.deactivate();
        playerRank.add(losingPlayer.getUsername());
    }
}