package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.action.*;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static dev.cerios.maugame.mauengine.game.PlayerIdGenerator.generatePlayerId;

class PlayerManager {
    public final int MAX_PLAYERS;
    public final int MIN_PLAYERS;

    private final static int defaultPlayerIndex = -2;

    private final AtomicInteger currentPlayerIndex = new AtomicInteger(defaultPlayerIndex);
    @Getter
    private int activeCounter = 0;
    private final Random random;
    private final List<Player> players;
    private final SequencedSet<String> playerRank;
    private final List<Runnable> closeListeners;

    /**
     * initiates with maxPlayers = minPlayers = 2
     *
     * @param random
     */
    public PlayerManager(Random random) {
        this(random, 2, 2);
    }

    public PlayerManager(Random random, int minPlayers, int maxPlayers) {
        if (minPlayers > maxPlayers)
            throw new IllegalArgumentException("Min players must be less than max players.");
        this.random = random;
        this.MIN_PLAYERS = minPlayers;
        this.MAX_PLAYERS = maxPlayers;
        this.players = new ArrayList<>(MAX_PLAYERS);
        this.playerRank = new LinkedHashSet<>(MAX_PLAYERS);
        this.closeListeners = new LinkedList<>();
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public void listenClose(Runnable listener) {
        this.closeListeners.add(listener);
    }

    public Player registerPlayer(String username, GameEventListener eventListener) throws GameException {
        if (players.size() >= MAX_PLAYERS)
            throw new GameException(
                    String.format(
                            "The game has exceeded the maximum number of players (%s).",
                            MAX_PLAYERS
                    )
            );

        var player = new Player(generatePlayerId(), username, eventListener);
        players.add(player);
        distributeActionExcludingPlayer(new RegisterAction(player, false), player.getPlayerId());
        player.trigger(new RegisterAction(player, true));
        player.trigger(new PlayersAction(new ArrayList<>(players)));
        activeCounter++;
        return player;
    }

    public void removePlayer(String playerId) throws GameException {
        if (currentPlayerIndex.get() != defaultPlayerIndex) {
            throw new IllegalStateException("Cannot remove player from initialized game.");
        }

        var removeIndex = IntStream.range(0, players.size())
                .filter(index -> players.get(index).getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameException("Player " + playerId + "not in game."));

        var removedPlayer = players.remove(removeIndex);
        if (removedPlayer.isActive()) {
            activeCounter--;
        }

        distributeActionToAll(new RemovePlayerAction(removedPlayer));
    }

    public Player currentPlayer() {
        var currentIndex = currentPlayerIndex.get();
        if (currentIndex == -2)
            throw new RuntimeException("Player manager has not been initialized yet.");
        return players.get(currentIndex % players.size());
    }

    public Player getPlayer(String playerId) throws GameException {
        return players.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameException("No player with id `" + playerId + "` was found."));
    }

    /**
     * Make player a winner and deactivates him.
     *
     * @param player to transform to winner
     * @return whether game should continue
     */
    public boolean playerWin(Player player) {
        player.disable();
        activeCounter--;
        playerRank.add(player.getUsername());
        distributeActionToAll(new WinAction(player));

        if (activeCounter == 1) {
            loseLastActivePlayer();
            return false;
        }
        distributeActionToAll(new SendRankAction(getPlayerRank()));
        return true;
    }

    public void deactivatePlayer(final String playerId) throws GameException {
        var player = getPlayer(playerId);
        if (!player.isActive())
            return;
        distributeActionToAll(new DeactivateAction(player));

        activeCounter--;
        player.disable();

        // if pm is initialized or at least 2 active players
        if (currentPlayerIndex.get() == defaultPlayerIndex || activeCounter > 1)
            return;

        if (activeCounter == 1) {
            playerWin(findNextPlayer());
            distributeActionToAll(new EndAction());
        }
        // send end notification
        for (var closeListener : closeListeners) {
            closeListener.run();
        }
    }

    public void activatePlayer(final String playerId) throws GameException {
        var player = getPlayer(playerId);
        if (player.isActive())
            return;
        player.enable();
        activeCounter++;
        distributeActionToAll(new ActivateAction(playerId));
    }

    public void validateCanStart() throws GameException {
        if (activeCounter < MIN_PLAYERS)
            throw new GameException("The game needs at least " + MIN_PLAYERS + " active players to start.");
    }

    public void initializePlayer() {
        currentPlayerIndex.set(random.nextInt(players.size()) - 1);
        shiftPlayer();
    }

    public void shiftPlayer() {
        var nextPlayer = findNextPlayer();
        var action = new PlayerShiftAction(nextPlayer);
        distributeActionToAll(action);
    }

    public void distributeActionToAll(Action action) {
        distributeAction(action, null);
    }

    public void distributeActionExcludingPlayer(Action action, String playerId) {
        distributeAction(action, p -> !p.getPlayerId().equals(playerId));
    }

    private void distributeAction(
            Action action,
            Predicate<Player> playerPredicate
    ) {
        var s = players.stream();
        if (playerPredicate != null)
            s = s.filter(playerPredicate);
        s.forEach(player -> player.trigger(action));
    }

    public int getFreeCapacity() {
        return MAX_PLAYERS - players.size();
    }

    public SequencedSet<String> getPlayerRank() {
        return Collections.unmodifiableSequencedSet(playerRank);
    }

    private Player findNextPlayer() {
        if (activeCounter < 1)
            throw new RuntimeException("There is no next player");
        Player nextPlayer;
        do {
            nextPlayer = players.get(currentPlayerIndex.incrementAndGet() % players.size());
        } while (!nextPlayer.isActive());
        return nextPlayer;
    }

    private void loseLastActivePlayer() {
        var losingPlayer = findNextPlayer();
        losingPlayer.disable();
        activeCounter--;
        playerRank.add(losingPlayer.getUsername());
        losingPlayer.trigger(new LoseAction(losingPlayer));
        distributeActionToAll(new SendRankAction(getPlayerRank()));
        distributeActionToAll(new EndAction());
    }
}
