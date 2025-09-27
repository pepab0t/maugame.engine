package dev.cerios.maugame.mauengine.player;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.GameEventListener;
import dev.cerios.maugame.mauengine.game.action.PlayersAction;
import dev.cerios.maugame.mauengine.game.action.ReadyAction;
import dev.cerios.maugame.mauengine.game.action.RegisterAction;
import dev.cerios.maugame.mauengine.game.action.UnreadyAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerLobbyStateTest {

    @Mock
    private Consumer<Collection<Player>> stateSwitcher;

    @Mock
    private GameEventListener eventListener1;

    @Mock
    private GameEventListener eventListener2;

    @Mock
    private ActionPublisherBuilder builder;

    @Mock
    private ActionPublisher actionPublisher;

    private PlayerLobbyState playerLobbyState;
    private UUID gameId;

    @BeforeEach
    void setUp() {
        doReturn(builder).when(builder).withPlayers(any());
        doReturn(actionPublisher).when(builder).build();

        gameId = UUID.randomUUID();
        playerLobbyState = new PlayerLobbyState(gameId, stateSwitcher, builder);
    }

    @Test
    void constructor_ShouldInitializeCorrectly() {
        // Given & When
        PlayerLobbyState state = new PlayerLobbyState(gameId, stateSwitcher, builder);

        // Then
        assertNotNull(state);
        assertEquals(0, state.getPlayers().size());
    }

    @Test
    void registerPlayer_ShouldAddPlayerSuccessfully() throws GameException {
        // Given
        String username = "testUser";
        String playerId = "player123";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId).thenReturn(playerId);

            // When
            Player result = playerLobbyState.registerPlayer(username, eventListener1);

            // Then
            assertNotNull(result);
            assertEquals(playerId, result.getPlayerId());
            assertEquals(username, result.getUsername());
            assertEquals(eventListener1, result.getEventListener());
            assertEquals(1, playerLobbyState.getPlayers().size());

            // Verify published actions
            verify(actionPublisher).publishActionExcludingPlayer(any(RegisterAction.class), anyString());
            verify(actionPublisher).publishAction(eq(result), any(RegisterAction.class));
            verify(actionPublisher).publishAction(eq(result), any(PlayersAction.class));
        }
    }

    @Test
    void registerPlayer_ShouldThrowExceptionForDuplicateUsername() throws GameException {
        // Given
        String username = "testUser";
        String playerId1 = "player1";
        String playerId2 = "player2";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId)
                    .thenReturn(playerId1)
                    .thenReturn(playerId2);

            playerLobbyState.registerPlayer(username, eventListener1);

            // When & Then
            GameException exception = assertThrows(
                    GameException.class,
                    () -> playerLobbyState.registerPlayer(username, eventListener2)
            );

            assertEquals("Username `" + username + "` is given", exception.getMessage());
            assertEquals(1, playerLobbyState.getPlayers().size());
        }
    }

    @Test
    void registerPlayer_ShouldUnreadyAllExistingPlayers() throws GameException {
        // Given
        String playerId1 = "player1";
        String playerId2 = "player2";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId)
                    .thenReturn(playerId1)
                    .thenReturn(playerId2);

            Player player1 = playerLobbyState.registerPlayer("user1", eventListener1);
            playerLobbyState.setReady(playerId1);
            reset(actionPublisher); // Clear previous interactions

            // When
            playerLobbyState.registerPlayer("user2", eventListener2);

            // Then
            verify(actionPublisher).publishActionExcludingPlayer(any(UnreadyAction.class), anyString());
        }
    }

    @Test
    void removePlayer_ShouldRemoveExistingPlayer() throws GameException {
        // Given
        String playerId = "player123";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId).thenReturn(playerId);

            playerLobbyState.registerPlayer("testUser", eventListener1);
            assertEquals(1, playerLobbyState.getPlayers().size());

            // When
            playerLobbyState.removePlayer(playerId);

            // Then
            assertEquals(0, playerLobbyState.getPlayers().size());
        }
    }

    @Test
    void removePlayer_ShouldHandleNonExistentPlayer() {
        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> playerLobbyState.removePlayer("nonExistentPlayer"));
        assertEquals(0, playerLobbyState.getPlayers().size());
    }

    @Test
    void removePlayer_ShouldUnreadyRemainingPlayers() throws GameException {
        // Given
        String playerId1 = "player1";
        String playerId2 = "player2";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId)
                    .thenReturn(playerId1)
                    .thenReturn(playerId2);

            playerLobbyState.registerPlayer("user1", eventListener1);
            playerLobbyState.registerPlayer("user2", eventListener2);
            playerLobbyState.setReady(playerId1);
            playerLobbyState.setReady(playerId2);
            reset(actionPublisher);

            // When
            playerLobbyState.removePlayer(playerId1);

            // Then
            verify(actionPublisher).publishActionToAll(any(UnreadyAction.class));
        }
    }

    @Test
    void getPlayer_ShouldReturnExistingPlayer() throws GameException {
        // Given
        String playerId = "player123";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId).thenReturn(playerId);

            Player registered = playerLobbyState.registerPlayer("testUser", eventListener1);

            // When
            Player result = playerLobbyState.getPlayer(playerId);

            // Then
            assertEquals(registered, result);
            assertEquals(playerId, result.getPlayerId());
        }
    }

    @Test
    void getPlayer_ShouldThrowExceptionForNonExistentPlayer() {
        // When & Then
        GameException exception = assertThrows(
                GameException.class,
                () -> playerLobbyState.getPlayer("nonExistentPlayer")
        );

        assertEquals("Player `nonExistentPlayer` not found", exception.getMessage());
    }

    @Test
    void getPlayers_ShouldReturnAllPlayers() throws GameException {
        // Given
        String playerId1 = "player1";
        String playerId2 = "player2";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId)
                    .thenReturn(playerId1)
                    .thenReturn(playerId2);

            Player player1 = playerLobbyState.registerPlayer("user1", eventListener1);
            Player player2 = playerLobbyState.registerPlayer("user2", eventListener2);

            // When
            var players = playerLobbyState.getPlayers();

            // Then
            assertEquals(2, players.size());
            assertTrue(players.contains(player1));
            assertTrue(players.contains(player2));
        }
    }

    @Test
    void setReady_ShouldMarkPlayerAsReady() throws GameException {
        // Given
        String playerId = "player123";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId).thenReturn(playerId);

            Player player = playerLobbyState.registerPlayer("testUser", eventListener1);
            reset(actionPublisher);

            // When
            playerLobbyState.setReady(playerId);

            // Then
            verify(actionPublisher).publishActionToAll(any(ReadyAction.class));
            verify(stateSwitcher, never()).accept(any()); // Should not switch with only 1 player
        }
    }

    @Test
    void setReady_ShouldThrowExceptionForNonExistentPlayer() {
        // When & Then
        GameException exception = assertThrows(
                GameException.class,
                () -> playerLobbyState.setReady("nonExistentPlayer")
        );

        assertEquals("Player `nonExistentPlayer` not found", exception.getMessage());
    }

    @Test
    void setReady_ShouldTriggerStateSwitchWhenAllPlayersReady() throws GameException {
        // Given
        String playerId1 = "player1";
        String playerId2 = "player2";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId)
                    .thenReturn(playerId1)
                    .thenReturn(playerId2);

            playerLobbyState.registerPlayer("user1", eventListener1);
            playerLobbyState.registerPlayer("user2", eventListener2);

            // When
            playerLobbyState.setReady(playerId1);
            verify(stateSwitcher, never()).accept(any()); // Not all ready yet

            playerLobbyState.setReady(playerId2);

            // Then
            verify(stateSwitcher).accept(playerLobbyState.getPlayers()); // Should trigger state switch
        }
    }

    @Test
    void setReady_ShouldNotTriggerStateSwitchWithInsufficientPlayers() throws GameException {
        // Given
        String playerId = "player123";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId).thenReturn(playerId);

            playerLobbyState.registerPlayer("testUser", eventListener1);

            // When
            playerLobbyState.setReady(playerId);

            // Then
            verify(stateSwitcher, never()).accept(any()); // Should not switch with insufficient players
        }
    }

    @Test
    void setReady_ShouldNotTriggerStateSwitchWhenNotAllReady() throws GameException {
        // Given
        String playerId1 = "player1";
        String playerId2 = "player2";
        String playerId3 = "player3";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId)
                    .thenReturn(playerId1)
                    .thenReturn(playerId2)
                    .thenReturn(playerId3);

            playerLobbyState.registerPlayer("user1", eventListener1);
            playerLobbyState.registerPlayer("user2", eventListener2);
            playerLobbyState.registerPlayer("user3", eventListener1);

            // When
            playerLobbyState.setReady(playerId1);
            playerLobbyState.setReady(playerId2);
            // Don't set player3 ready

            // Then
            verify(stateSwitcher, never()).accept(any()); // Should not switch when not all ready
        }
    }

    @Test
    void readyInnerClass_ShouldWorkCorrectly() throws GameException {
        // Given
        String playerId = "player123";

        try (MockedStatic<PlayerIdGenerator> mockedGenerator = mockStatic(PlayerIdGenerator.class)) {
            mockedGenerator.when(PlayerIdGenerator::generatePlayerId).thenReturn(playerId);

            playerLobbyState.registerPlayer("testUser", eventListener1);

            // Test Ready inner class functionality through public methods
            playerLobbyState.setReady(playerId);

            // Verify the ready action was published (indirect test of Ready class)
            verify(actionPublisher, atLeastOnce()).publishActionToAll(any(ReadyAction.class));
        }
    }
}