package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.action.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.cerios.maugame.mauengine.TestUtils.*;
import static java.util.List.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerManagerTest {

    private PlayerManager pm;
    @Mock
    private Random random;

    @BeforeEach
    void setUp() {
        pm = new PlayerManager(random, 2, 2);
    }

    @Test
    void testShouldRegisterPlayer() throws Exception {
        // setup
        List<Action> events = new LinkedList<>();
        GameEventListener eventListener = (p, event) -> events.add(event);

        // when
        var registered = pm.registerPlayer("player1", eventListener);

        // then
        assertThat(pm.getPlayers()).containsExactly(registered);
        assertThat((int) getField(pm, "activeCounter")).isOne();
        assertThat(registered.isActive()).isTrue();
        assertThat(events).containsExactly(new RegisterAction(registered, true), new PlayersAction(of(registered)));
    }

    @Test
    void whenRegisterPlayer_otherPlayersShouldGetNotified() throws Exception {
        // setup
        Map<String, List<Action>> actions = new HashMap<>();
        GameEventListener eventListener = (p, event) -> {
            var playerActions = actions.getOrDefault(p.getPlayerId(), new LinkedList<>());
            playerActions.add(event);
            actions.put(p.getPlayerId(), playerActions);
        };
        var alreadyRegistered = pm.registerPlayer("player1", eventListener);

        // when
        var registered = pm.registerPlayer("player2", eventListener);

        // then
        assertThat(pm.getPlayers()).containsExactly(alreadyRegistered, registered);
        assertThat((int) getField(pm, "activeCounter")).isEqualTo(2);
        assertThat(actions.get(alreadyRegistered.getPlayerId())).containsExactly(
                new RegisterAction(alreadyRegistered, true),
                new PlayersAction(of(alreadyRegistered)), new RegisterAction(registered, false)
        );
        assertThat(actions.get(registered.getPlayerId())).containsExactly(
                new RegisterAction(registered, true),
                new PlayersAction(of(alreadyRegistered, registered))
        );
    }

    @Test
    void whenRegisterTooMuchPlayers_thenThrow() throws Exception {
        // setup
        GameEventListener eventListener = (p, event) -> {};
        setField(pm, "MAX_PLAYERS", 1);
        pm.registerPlayer("gogo", eventListener);

        // when, then
        assertThatThrownBy(() -> pm.registerPlayer("anonymous", eventListener)).isInstanceOf(GameException.class);
    }

    @Test
    void shouldGetRegisteredPlayerById() throws GameException {
        // setup
        GameEventListener eventListener = (p, event) -> {};
        var player = pm.registerPlayer("josh", eventListener);

        // when
        var obtainedPlayer = pm.getPlayer(player.getPlayerId());

        // then
        assertThat(obtainedPlayer).isSameAs(player);
    }

    @Test
    void whenGetPlayerNotRegistered_thenThrow() {
        // setup
        assertThatThrownBy(() -> pm.getPlayer("some-nonexisting-ulid"));
    }

    @Test
    void shouldRetrieveCurrentPlayer() throws GameException {
        // setup
        when(random.nextInt(any(int.class))).thenReturn(1);
        pm.registerPlayer("joe", VOID_LISTENER);
        var playerOnTurn = pm.registerPlayer("juan", VOID_LISTENER);
        pm.initializePlayer();

        // when
        var result = pm.currentPlayer();

        // then
        assertThat(result).isSameAs(playerOnTurn);
    }

    @Test
    void whenRetrieveCurrentPlayer_andNotInitialized_thenThrowUnchecked() throws GameException {
        // setup
        pm.registerPlayer("joe", VOID_LISTENER);
        pm.registerPlayer("juan", VOID_LISTENER);

        // when, then
        assertThatThrownBy(() -> pm.currentPlayer()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldDeactivatePlayerAndNotifyAllPlayers() throws GameException {
        // when
        var playerActions1 = new LinkedList<Action>();
        var playerActions2 = new LinkedList<Action>();
        var player1 = pm.registerPlayer("joe", (p, e) -> playerActions1.add(e));
        var player2 = pm.registerPlayer("juan", (p, e) -> playerActions2.add(e));

        // when
        pm.deactivatePlayer(player1.getPlayerId());

        // then
        assertThat(pm.getActiveCounter()).isEqualTo((byte) 1);
        assertThat(player1.isActive()).isFalse();
        assertThat(player2.isActive()).isTrue();
        assertThat(playerActions1).last().isEqualTo(new DeactivateAction(player1));
        assertThat(playerActions2).last().isEqualTo(new DeactivateAction(player1));
    }

    @Test
    void whenDeactivateDeactivatedPlayer_thenIgnore() throws Exception {
        // setup
        var playerActions1 = new LinkedList<Action>();
        var playerActions2 = new LinkedList<Action>();
        var player1 = pm.registerPlayer("joe", (p, e) -> playerActions1.add(e));
        var player2 = pm.registerPlayer("juan", (p, e) -> playerActions2.add(e));
        pm.deactivatePlayer(player1.getPlayerId());
        playerActions1.clear();
        playerActions2.clear();

        // when
        pm.deactivatePlayer(player1.getPlayerId());

        // then
        assertThat(pm.getActiveCounter()).isEqualTo(1);
        assertThat(player1.isActive()).isFalse();
        assertThat(player2.isActive()).isTrue();
        assertThat(playerActions1).isEmpty();
        assertThat(playerActions2).isEmpty();
    }

    @Test
    void shouldActivateDeactivatedPlayer() throws Exception {
        // setup
        var playerActions1 = new LinkedList<Action>();
        var playerActions2 = new LinkedList<Action>();
        var player1 = pm.registerPlayer("joe", (p, e) -> playerActions1.add(e));
        var player2 = pm.registerPlayer("juan", (p, e) -> playerActions2.add(e));
        pm.deactivatePlayer(player1.getPlayerId());
        playerActions1.clear();
        playerActions2.clear();

        // when
        pm.activatePlayer(player1.getPlayerId());

        // then
        assertThat(pm.getActiveCounter()).isEqualTo(2);
        assertThat(player1.isActive()).isTrue();
        assertThat(player2.isActive()).isTrue();
        assertThat(playerActions1).containsExactly(new ActivateAction(player1));
        assertThat(playerActions2).containsExactly(new ActivateAction(player1));
    }

    @Test
    void whenActivateActivatedPlayer_thenIgnore() throws GameException {
        // setup
        var playerActions1 = new LinkedList<Action>();
        var playerActions2 = new LinkedList<Action>();
        var player1 = pm.registerPlayer("joe", (p, e) -> playerActions1.add(e));
        var player2 = pm.registerPlayer("juan", (p, e) -> playerActions2.add(e));
        playerActions1.clear();
        playerActions2.clear();

        // when
        pm.activatePlayer(player1.getPlayerId());

        // then
        assertThat(pm.getActiveCounter()).isEqualTo(2);
        assertThat(player1.isActive()).isTrue();
        assertThat(player2.isActive()).isTrue();
        assertThat(playerActions1).isEmpty();
        assertThat(playerActions2).isEmpty();
    }

    @Test
    void shouldDistributeActionToAll() throws Exception {
        // setup
        Action fooAction = () -> null;
        setField(pm, "MAX_PLAYERS", 3);

        var actions1 = new LinkedList<Action>();
        var actions2 = new LinkedList<Action>();
        var actions3 = new LinkedList<Action>();
        pm.registerPlayer("joe", (p, e) -> actions1.add(e));
        pm.registerPlayer("juan", (p, e) -> actions2.add(e));
        pm.registerPlayer("george", (p, e) -> actions3.add(e));
        actions1.clear();
        actions2.clear();
        actions3.clear();

        // when
        pm.distributeActionToAll(fooAction);

        // then
        assertThat(actions1).containsExactly(fooAction);
        assertThat(actions2).containsExactly(fooAction);
        assertThat(actions3).containsExactly(fooAction);
    }

    @Test
    void shouldDistributeAllExcludingPlayer() throws Exception {
        // setup
        Action fooAction = () -> null;
        setField(pm, "MAX_PLAYERS", 3);

        var actions1 = new LinkedList<Action>();
        var actions2 = new LinkedList<Action>();
        var actions3 = new LinkedList<Action>();
        var player1 = pm.registerPlayer("joe", (p, e) -> actions1.add(e));
        pm.registerPlayer("juan", (p, e) -> actions2.add(e));
        pm.registerPlayer("george", (p, e) -> actions3.add(e));
        actions1.clear();
        actions2.clear();
        actions3.clear();

        // when
        pm.distributeActionExcludingPlayer(fooAction, player1.getPlayerId());

        // then
        assertThat(actions1).isEmpty();
        assertThat(actions2).containsExactly(fooAction);
        assertThat(actions3).containsExactly(fooAction);
    }

    @Test
    void testPlayerWinsAndGameContinues() throws Exception {
        // setup
        var collector = createCollector();
        setField(pm, "MAX_PLAYERS", 3);
        var winner = pm.registerPlayer("joe", collector.getListener());
        var player1 = pm.registerPlayer("juan", collector.getListener());
        var player2 = pm.registerPlayer("george", collector.getListener());
        collector.clear();

        // when
        var result = pm.playerWin(winner);

        // then
        var expectedActions = of(new WinAction(winner), new SendRankAction(new LinkedHashSet<>(of("joe"))));
        assertThat(result).isTrue();
        assertThat(collector.getActions(winner)).containsExactlyElementsOf(expectedActions);
        assertThat(collector.getActions(player1)).containsExactlyElementsOf(expectedActions);
        assertThat(collector.getActions(player2)).containsExactlyElementsOf(expectedActions);
        assertThat(pm.getActiveCounter()).isEqualTo(2);
        assertThat(player1.isActive()).isTrue();
        assertThat(player2.isActive()).isTrue();
        assertThat(winner.isActive()).isFalse();
    }

    @Test
    void whenOnePlayerWinsAndOneRemains_thenDoNotContinue() throws GameException {
        // setup
        var collector = createCollector();
        var winner = pm.registerPlayer("joe", collector.getListener());
        var loser = pm.registerPlayer("juan", collector.getListener());
        when(random.nextInt(any(int.class))).thenReturn(0);
        pm.initializePlayer();
        collector.clear();

        // when
        var result = pm.playerWin(winner);

        // then
        assertThat(result).isFalse();
        assertThat(collector.getActions(winner)).containsExactly(
                new WinAction(winner),
                new SendRankAction(new LinkedHashSet<>(of("joe", "juan"))),
                new EndAction()
        );
        assertThat(collector.getActions(loser)).containsExactly(
                new WinAction(winner),
                new LoseAction(loser),
                new SendRankAction(new LinkedHashSet<>(of("joe", "juan"))),
                new EndAction()
        );
        assertThat(pm.getActiveCounter()).isEqualTo(0);
        assertThat(loser.isActive()).isFalse();
        assertThat(winner.isActive()).isFalse();
    }

    @Test
    void whenShiftPlayerAndNoOneActive_thenThrowUnchecked() throws GameException {
        // setup
        var joe = pm.registerPlayer("joe", VOID_LISTENER);
        var juan = pm.registerPlayer("juan", VOID_LISTENER);
        pm.deactivatePlayer(joe.getPlayerId());
        pm.deactivatePlayer(juan.getPlayerId());

        // when, then
        assertThatThrownBy(() -> pm.shiftPlayer()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void whenMiddlePlayerDeactivated_thenSkipHimWhenShifting() throws Exception {
        // setup
        setField(pm, "MAX_PLAYERS", 3);
        var collector = createCollector();
        var p1 = pm.registerPlayer("jose", collector.getListener());
        var p2 = pm.registerPlayer("juan", collector.getListener());
        var p3 = pm.registerPlayer("george", collector.getListener());
        pm.deactivatePlayer(p2.getPlayerId());
        when(random.nextInt(any(int.class))).thenReturn(0);
        pm.initializePlayer();
        collector.clear();

        // when
        pm.shiftPlayer();

        // then
        assertThat(pm.currentPlayer()).isEqualTo(p3);
        assertThat(collector.getActions(p1)).containsExactly(new PlayerShiftAction(p3));
        assertThat(collector.getActions(p2)).containsExactly(new PlayerShiftAction(p3));
        assertThat(collector.getActions(p3)).containsExactly(new PlayerShiftAction(p3));
    }

    @Test
    void whenFirstPlayerIsNotActive_thenInitiateToSecond() throws Exception {
        // setup
        setField(pm, "MAX_PLAYERS", 3);
        var collector = createCollector();
        var p1 = pm.registerPlayer("jose", collector.getListener());
        var p2 = pm.registerPlayer("juan", collector.getListener());
        pm.deactivatePlayer(p1.getPlayerId());
        when(random.nextInt(any(int.class))).thenReturn(0); // starting from first one
        collector.clear();

        // when
        pm.initializePlayer();

        // then
        assertThat(pm.currentPlayer()).isEqualTo(p2);
        assertThat(collector.getActions(p1)).containsExactly(new PlayerShiftAction(p2));
        assertThat(collector.getActions(p2)).containsExactly(new PlayerShiftAction(p2));
    }

    @Test
    void whenShiftingPlayer_thenShouldCycleThroughActiveOnes() throws Exception {
        // setup
        setField(pm, "MAX_PLAYERS", 3);
        var collector = createCollector();
        var p1 = pm.registerPlayer("jose", collector.getListener());
        var p2 = pm.registerPlayer("juan", collector.getListener());
        var p3 = pm.registerPlayer("george", collector.getListener());
        when(random.nextInt(any(int.class))).thenReturn(0); // starting from first player
        collector.clear();

        // when
        pm.initializePlayer(); // p1
        pm.shiftPlayer(); // p2
        pm.deactivatePlayer(p2.getPlayerId());
        pm.shiftPlayer(); // p3
        pm.shiftPlayer(); // p1
        pm.shiftPlayer(); // p3

        // then
        var expectedActions = of(
                new PlayerShiftAction(p1),
                new PlayerShiftAction(p2),
                new DeactivateAction(p2),
                new PlayerShiftAction(p3),
                new PlayerShiftAction(p1),
                new PlayerShiftAction(p3)
        );
        assertThat(pm.currentPlayer()).isEqualTo(p3);
        assertThat(collector.getActions(p1)).containsExactlyElementsOf(expectedActions);
        assertThat(collector.getActions(p2)).containsExactlyElementsOf(expectedActions);
        assertThat(collector.getActions(p3)).containsExactlyElementsOf(expectedActions);
    }

    @Test
    void shouldValidateCanStartPositively() throws GameException {
        // setup
        pm.registerPlayer("jose", VOID_LISTENER);
        pm.registerPlayer("juan", VOID_LISTENER);

        // when
        pm.validateCanStart(); // should just pass
    }

    @Test
    void whenNotEnoughPlayers_validationThrows() throws GameException {
        // setup
        pm.registerPlayer("jose", VOID_LISTENER);

        // when
        assertThatThrownBy(() -> pm.validateCanStart())
                .isInstanceOf(GameException.class);
    }


    @Test
    void whenNotEnoughActivePlayers_validationThrows() throws GameException {
        // setup
        pm.registerPlayer("jose", VOID_LISTENER);
        var juan = pm.registerPlayer("juan", VOID_LISTENER);
        pm.deactivatePlayer(juan.getPlayerId());

        // when
        assertThatThrownBy(() -> pm.validateCanStart())
                .isInstanceOf(GameException.class);
    }

    @Test
    void whenPlayerDeactivates_andOnlyOneRemains_winAndTriggerClose() throws GameException {
        // setup
        var joseActions = new LinkedList<Action>();
        var juanActions = new LinkedList<Action>();
        var jose = pm.registerPlayer("jose", (p, e) -> joseActions.add(e));
        var juan = pm.registerPlayer("juan", (p, e) -> juanActions.add(e));
        pm.initializePlayer();
        joseActions.clear();
        juanActions.clear();
        var closed = new AtomicBoolean(false);
        pm.listenClose(() -> closed.set(true));

        // when
        pm.deactivatePlayer(jose.getPlayerId());

        // then
        assertThat(closed.get()).isTrue();
        assertThat(pm.getActiveCounter()).isZero();
        var expectedActions = of(
                new DeactivateAction(jose),
                new WinAction(juan),
                new SendRankAction(new LinkedHashSet<>(of("juan"))),
                new EndAction()
        );
        assertThat(juanActions).containsExactlyElementsOf(expectedActions);
    }

    @Test
    void whenOnlyOnePlayerAndDeactivates_thenCloseTheGame() throws GameException {
        // setup
        var joseActions = new LinkedList<Action>();
        var jose = pm.registerPlayer("jose", (p, e) -> joseActions.add(e));
        pm.initializePlayer();
        var closed = new AtomicBoolean(false);
        pm.listenClose(() -> closed.set(true));
        joseActions.clear();

        // when
        pm.deactivatePlayer(jose.getPlayerId());

        // then
        assertThat(closed.get()).isTrue();
        assertThat(pm.getActiveCounter()).isZero();
        assertThat(joseActions).containsExactly(new DeactivateAction(jose));
    }

    @Test
    void whenRemovePlayerFromInitialized_thenThrowUnchecked() throws GameException {
        // setup
        var jose = pm.registerPlayer("jose", VOID_LISTENER);
        pm.registerPlayer("juan", VOID_LISTENER);
        pm.initializePlayer();

        // when, then
        assertThatThrownBy(() -> pm.removePlayer(jose.getPlayerId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void whenRemovePlayerThatIsNotInGame_thenThrow() throws GameException {
        // setup
        pm.registerPlayer("jose", VOID_LISTENER);
        pm.registerPlayer("juan", VOID_LISTENER);

        // when, then
        assertThatThrownBy(() -> pm.removePlayer("non-existing-id"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("not", "in", "game", "non-existing-id");
    }

    @Test
    void shouldRemoveActivePlayerFromLobby() throws GameException {
        // setup
        var actionCollector = createCollector();
        var jose = pm.registerPlayer("jose", actionCollector.getListener());
        var juan = pm.registerPlayer("juan", actionCollector.getListener());
        actionCollector.clear();

        // when
        pm.removePlayer(jose.getPlayerId());

        // then
        assertThat(pm.getActiveCounter()).isOne();
        assertThat(pm.getPlayers()).containsExactly(juan);
        assertThat(pm.getPlayerRank()).isEmpty();
        assertThat(actionCollector.getActions(juan)).containsExactly(new RemovePlayerAction(jose));
        assertThat(actionCollector.getActions(jose)).isEmpty();
    }

    @Test
    void shouldRemoveNonActivePlayerFromLobby() throws GameException {
        // setup
        var actionCollector = createCollector();
        var jose = pm.registerPlayer("jose", actionCollector.getListener());
        var juan = pm.registerPlayer("juan", actionCollector.getListener());
        pm.deactivatePlayer(jose.getPlayerId());
        actionCollector.clear();

        // when
        pm.removePlayer(jose.getPlayerId());

        // then
        assertThat(pm.getActiveCounter()).isOne();
        assertThat(pm.getPlayers()).containsExactly(juan);
        assertThat(pm.getPlayerRank()).isEmpty();
        assertThat(actionCollector.getActions(juan)).containsExactly(new RemovePlayerAction(jose));
        assertThat(actionCollector.getActions(jose)).isEmpty();
    }
}