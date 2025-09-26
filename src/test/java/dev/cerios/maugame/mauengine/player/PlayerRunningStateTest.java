package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.exception.NotSupportedOperation;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.action.EndAction;
import dev.cerios.maugame.mauengine.game.action.PlayerShiftAction;
import dev.cerios.maugame.mauengine.game.action.RemovePlayerAction;
import dev.cerios.maugame.mauengine.game.action.SendRankAction;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerRunningStateTest {

    @Mock
    private Consumer<Collection<Player>> stateSwitcher;

    @Mock
    private ActionPublisher actionPublisher;

    @Mock
    private ActionPublisherBuilder actionPublisherBuilder;

    @Mock
    private GameEventListener eventListener1;

    @Mock
    private GameEventListener eventListener2;

    @Mock
    private GameEventListener eventListener3;

    @Mock
    private ScheduledExecutorService mockExecutor;

    @Mock
    private ScheduledFuture<?> mockFuture;

    private PlayerRunningState playerRunningState;
    private Random random;
    private ReadWriteLock globalLock;
    private List<Player> testPlayers;
    private final long turnTimeoutMs = 5000L;

    @BeforeEach
    void setUp() {
        random = new Random(42); // Fixed seed for predictable tests
        globalLock = new ReentrantReadWriteLock();

        // Create test players
        testPlayers = List.of(
                new Player("player1", "user1", eventListener1),
                new Player("player2", "user2", eventListener2),
                new Player("player3", "user3", eventListener3)
        );

        when(actionPublisherBuilder.build()).thenReturn(actionPublisher);
        when(actionPublisherBuilder.withPlayers(any())).thenReturn(actionPublisherBuilder);
    }

    @AfterEach
    void tearDown() {
        if (playerRunningState != null) {
            // Clean up any running executors
            try {
                var executor = playerRunningState.getClass().getDeclaredField("executor");
                executor.setAccessible(true);
                ScheduledExecutorService executorService = (ScheduledExecutorService) executor.get(playerRunningState);
                if (executorService != null && !executorService.isShutdown()) {
                    executorService.shutdownNow();
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    private PlayerRunningState createPlayerRunningStateWithMockExecutor() {
        doReturn(mockFuture).when(mockExecutor).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        return new PlayerRunningState(
                random, testPlayers, turnTimeoutMs, stateSwitcher, globalLock, actionPublisherBuilder, mockExecutor
        );
    }

    @Test
    void constructor_ShouldInitializeCorrectly() {
        // When
        playerRunningState = new PlayerRunningState(
                random, testPlayers, turnTimeoutMs, stateSwitcher, globalLock, actionPublisherBuilder
        );

        // Then
        assertNotNull(playerRunningState);
        assertEquals(3, playerRunningState.getPlayers().size());
        assertNotNull(playerRunningState.getCurrentPlayer());
        assertNotNull(playerRunningState.getActionPublisher());
        assertTrue(playerRunningState.getPlayerRank().isEmpty());

        verify(actionPublisher).publishActionToAll(any(PlayerShiftAction.class));
    }

    @Test
    void getPlayers_ShouldReturnUnmodifiableCollection() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();

        // When
        Collection<Player> players = playerRunningState.getPlayers();

        // Then
        assertEquals(3, players.size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> players.add(new Player("newId", "newUser", eventListener1))
        );
    }

    @Test
    void getCurrentPlayer_ShouldReturnValidPlayer() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();

        // When
        Player currentPlayer = playerRunningState.getCurrentPlayer();

        // Then
        assertNotNull(currentPlayer);
        assertTrue(testPlayers.contains(currentPlayer));
    }

    @Test
    void registerPlayer_ShouldThrowNotSupportedOperation() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();

        // When & Then
        assertThrows(
                NotSupportedOperation.class,
                () -> playerRunningState.registerPlayer("newUser", eventListener1)
        );
    }

    @Test
    void removePlayer_ShouldThrowNotSupportedOperation() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();

        // When & Then
        assertThrows(
                NotSupportedOperation.class,
                () -> playerRunningState.removePlayer("player1")
        );
    }

    @Test
    void getPlayer_ShouldReturnExistingPlayer() throws GameException {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();

        // When
        Player player = playerRunningState.getPlayer("player1");

        // Then
        assertNotNull(player);
        assertEquals("player1", player.getPlayerId());
        assertEquals("user1", player.getUsername());
    }

    @Test
    void getPlayer_ShouldThrowExceptionForNonExistentPlayer() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();

        // When & Then
        GameException exception = assertThrows(
                GameException.class,
                () -> playerRunningState.getPlayer("nonExistentPlayer")
        );

        assertEquals("No player with id `nonExistentPlayer` was found.", exception.getMessage());
    }

    @Test
    void getPlayerForPlay_ShouldWorkForCurrentPlayer() throws MauEngineBaseException {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();
        Player currentPlayer = playerRunningState.getCurrentPlayer();

        PlayerRunningState.BiFunctionChecked<ActionPublisher, Player, Boolean> playerFunction =
                (publisher, player) -> {
                    assertEquals(actionPublisher, publisher);
                    assertEquals(currentPlayer, player);
                    return false; // Don't win
                };

        // When & Then
        assertDoesNotThrow(() ->
                playerRunningState.getPlayerForPlay(currentPlayer.getPlayerId(), playerFunction));

        // Verify poke was called (future should be cancelled)
        verify(mockFuture).cancel(true);
        // Verify next player shift
        verify(actionPublisher, atLeast(2)).publishActionToAll(any(PlayerShiftAction.class));
    }

    @Test
    void getPlayerForPlay_ShouldThrowExceptionForWrongPlayer() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();
        Player currentPlayer = playerRunningState.getCurrentPlayer();

        // Find a non-current player
        String nonCurrentPlayerId = testPlayers.stream()
                .filter(p -> !p.getPlayerId().equals(currentPlayer.getPlayerId()))
                .findFirst()
                .get()
                .getPlayerId();

        PlayerRunningState.BiFunctionChecked<ActionPublisher, Player, Boolean> playerFunction =
                (publisher, player) -> false;

        // When & Then
        GameException exception = assertThrows(
                GameException.class,
                () -> playerRunningState.getPlayerForPlay(nonCurrentPlayerId, playerFunction)
        );

        assertEquals("It's not a player's turn.", exception.getMessage());
    }

    @Test
    void getPlayerForPlay_ShouldHandleWinCondition() throws MauEngineBaseException {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();
        Player currentPlayer = playerRunningState.getCurrentPlayer();

        PlayerRunningState.BiFunctionChecked<ActionPublisher, Player, Boolean> winFunction =
                (publisher, player) -> true; // Player wins

        // When
        playerRunningState.getPlayerForPlay(currentPlayer.getPlayerId(), winFunction);

        // Then
        assertTrue(currentPlayer.isFinished());
        assertEquals(1, playerRunningState.getPlayerRank().size());
        assertEquals(currentPlayer.getUsername(), playerRunningState.getPlayerRank().get(0));
        verify(actionPublisher).publishActionToAll(any(SendRankAction.class));
    }

    @Test
    void getPlayerForPlay_ShouldEndGameWhenOnlyOnePlayerLeft() throws MauEngineBaseException {
        // Given
        List<Player> twoPlayers = List.of(
                new Player("player1", "user1", eventListener1),
                new Player("player2", "user2", eventListener2)
        );

        var switcher = new Consumer<Collection<Player>>() {
            @Getter
            private final Collection<Player> players = new LinkedList<>();
            @Override
            public void accept(Collection<Player> players) {
                this.players.addAll(players);
            }
        };

        playerRunningState = new PlayerRunningState(
                random, twoPlayers, turnTimeoutMs, switcher, globalLock, actionPublisherBuilder
        );

        try {
            var executorField = PlayerRunningState.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(playerRunningState, mockExecutor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        doReturn(mockFuture).when(mockExecutor).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        Player currentPlayer = playerRunningState.getCurrentPlayer();

        PlayerRunningState.BiFunctionChecked<ActionPublisher, Player, Boolean> winFunction =
                (publisher, player) -> true; // Player wins

        // When
        playerRunningState.getPlayerForPlay(currentPlayer.getPlayerId(), winFunction);

        // Then
        verify(actionPublisher).publishActionToAll(any(EndAction.class));
        assertThat(switcher.getPlayers()).containsExactlyElementsOf(playerRunningState.getPlayers());
        assertEquals(2, playerRunningState.getPlayerRank().size()); // Both players should be in rank
    }

    @Test
    void listenTimeout_ShouldAddListener() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();
        Consumer<Player> listener = mock(Consumer.class);

        // When
        assertDoesNotThrow(() -> playerRunningState.listenTimeout(listener));

        // Then
        // Verify by triggering a timeout scenario
        try {
            var listenersField = PlayerRunningState.class.getDeclaredField("timeoutListeners");
            listenersField.setAccessible(true);
            List<Consumer<Player>> listeners = (List<Consumer<Player>>) listenersField.get(playerRunningState);
            assertEquals(1, listeners.size());
        } catch (Exception e) {
            fail("Failed to verify listeners: " + e.getMessage());
        }
    }

    @Test
    void getLastExpire_ShouldReturnExpireTime() throws MauEngineBaseException {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();
        Player currentPlayer = playerRunningState.getCurrentPlayer();

        // Simulate a turn
        PlayerRunningState.BiFunctionChecked<ActionPublisher, Player, Boolean> playerFunction =
                (publisher, player) -> false;
        playerRunningState.getPlayerForPlay(currentPlayer.getPlayerId(), playerFunction);

        Player nextCurrentPlayer = playerRunningState.getCurrentPlayer();

        // When
        long expireTime = playerRunningState.getLastExpire(nextCurrentPlayer.getPlayerId());

        // Then
        assertTrue(expireTime > 0); // Should have a valid expire time
    }

    @Test
    void getLastExpire_ShouldReturnMinusOneForNonExistentFuture() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();

        // When
        long expireTime = playerRunningState.getLastExpire("nonExistentPlayer");

        // Then
        assertEquals(-1L, expireTime);
    }

    @Test
    void getPlayerRank_ShouldReturnUnmodifiableList() throws MauEngineBaseException {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();
        Player currentPlayer = playerRunningState.getCurrentPlayer();

        // Make a player win
        PlayerRunningState.BiFunctionChecked<ActionPublisher, Player, Boolean> winFunction =
                (publisher, player) -> true;
        playerRunningState.getPlayerForPlay(currentPlayer.getPlayerId(), winFunction);

        // When
        List<String> rank = playerRunningState.getPlayerRank();

        // Then
        assertEquals(1, rank.size());
        assertEquals(currentPlayer.getUsername(), rank.get(0));
        assertThrows(
                UnsupportedOperationException.class,
                () -> rank.add("newPlayer")
        );
    }

    @Test
    void futureWithTimeout_ShouldCancelCorrectly() {
        // Given
        Future<?> mockInnerFuture = mock(Future.class);
        long expireTime = System.currentTimeMillis() + 1000;

        PlayerRunningState.FutureWithTimeout futureWithTimeout =
                new PlayerRunningState.FutureWithTimeout(mockInnerFuture, expireTime);

        // When
        futureWithTimeout.cancel();

        // Then
        verify(mockInnerFuture).cancel(true);
        assertEquals(expireTime, futureWithTimeout.expireAtMs());
    }

    @Test
    void timeoutScenario_ShouldHandlePlayerTimeout() throws Exception {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();
        Consumer<Player> timeoutListener = mock(Consumer.class);
        playerRunningState.listenTimeout(timeoutListener);

        playerRunningState.getPlayerForPlay(playerRunningState.getCurrentPlayer().getPlayerId(), (__, __1) -> false);
        var currentPlayer = playerRunningState.getCurrentPlayer();

        // Capture the timeout runnable
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockExecutor, atLeastOnce()).schedule(runnableCaptor.capture(), eq(turnTimeoutMs), eq(TimeUnit.MILLISECONDS));

        Runnable timeoutRunnable = runnableCaptor.getValue();

        // When
        timeoutRunnable.run();

        // Then
        verify(actionPublisher).publishActionToAll(any(RemovePlayerAction.class));
        verify(timeoutListener).accept(currentPlayer);
        assertThat(playerRunningState.getPlayers()).doesNotContain(currentPlayer);
    }

    @Test
    void multiplePlayersWinning_ShouldMaintainCorrectRanking() throws MauEngineBaseException {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();

        Player firstPlayer = playerRunningState.getCurrentPlayer();
        String firstName = firstPlayer.getUsername();

        // First player wins
        PlayerRunningState.BiFunctionChecked<ActionPublisher, Player, Boolean> winFunction =
                (publisher, player) -> true;
        playerRunningState.getPlayerForPlay(firstPlayer.getPlayerId(), winFunction);

        // Second player wins
        Player secondPlayer = playerRunningState.getCurrentPlayer();
        String secondName = secondPlayer.getUsername();
        playerRunningState.getPlayerForPlay(secondPlayer.getPlayerId(), winFunction);

        // Then
        List<String> rank = playerRunningState.getPlayerRank();
        assertEquals(3, rank.size()); // All players should be in final rank
        assertEquals(firstName, rank.get(0));
        assertEquals(secondName, rank.get(1));

        verify(stateSwitcher).accept(anyCollection()); // Game should end
    }

    @Test
    void biFunctionChecked_ShouldHandleExceptions() {
        // Given
        playerRunningState = createPlayerRunningStateWithMockExecutor();
        Player currentPlayer = playerRunningState.getCurrentPlayer();

        PlayerRunningState.BiFunctionChecked<ActionPublisher, Player, Boolean> throwingFunction =
                (publisher, player) -> {
                    throw new GameException("Test exception");
                };

        // When & Then
        assertThrows(
                GameException.class,
                () -> playerRunningState.getPlayerForPlay(currentPlayer.getPlayerId(), throwingFunction)
        );
    }
}