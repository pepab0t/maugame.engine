package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardManager;
import dev.cerios.maugame.mauengine.card.CardType;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.game.action.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static dev.cerios.maugame.mauengine.TestUtils.*;
import static dev.cerios.maugame.mauengine.game.Stage.LOBBY;
import static dev.cerios.maugame.mauengine.game.Stage.RUNNING;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerManagerTest {

    private PlayerManager pm;
    private AtomicReference<Stage> stage;
    @Mock
    private Random random;
    @Mock
    private CardManager cardManager;

    @BeforeEach
    void setUp() {
        stage = new AtomicReference<>(LOBBY);
        pm = new PlayerManager(random, 2, 2, Executors.newVirtualThreadPerTaskExecutor(), stage, cardManager);
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
        assertThat(pm.getActiveCounter()).isOne();
        assertThat(registered.isFinished()).isFalse();
        assertThat(events).containsExactly(new RegisterAction(registered, true), new PlayersAction(of(registered)));
    }

    @Test
    void whenRegisterPlayerWithNonUniqueUsername_thenThrow() throws GameException {
        // setup
        final String nonUniqueName = "john";
        pm.registerPlayer(nonUniqueName, VOID_LISTENER);

        // when, then
        assertThatThrownBy(() -> pm.registerPlayer(nonUniqueName, VOID_LISTENER))
                .isInstanceOf(GameException.class)
                .hasMessageContainingAll(nonUniqueName, "already", "registered");
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
        assertThat(pm.getActiveCounter()).isEqualTo(2);
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
        var expectedActions = of(new SendRankAction(new LinkedList<>(of("joe"))));
        assertThat(result).isTrue();
        assertThat(collector.getActions(winner)).containsExactlyElementsOf(expectedActions);
        assertThat(collector.getActions(player1)).containsExactlyElementsOf(expectedActions);
        assertThat(collector.getActions(player2)).containsExactlyElementsOf(expectedActions);
        assertThat(pm.getActiveCounter()).isEqualTo(2);
        assertThat(player1.isFinished()).isFalse();
        assertThat(player2.isFinished()).isFalse();
        assertThat(winner.isFinished()).isTrue();
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
                new EndAction(new LinkedList<>(of("joe", "juan")))
        );
        assertThat(collector.getActions(loser)).containsExactly(
                new EndAction(new LinkedList<>(of("joe", "juan")))
        );
        assertThat(pm.getActiveCounter()).isEqualTo(0);
        assertThat(loser.isFinished()).isTrue();
        assertThat(winner.isFinished()).isTrue();
    }

    @Test
    void whenShiftPlayerAndNoOnePlaying_thenThrowUnchecked() throws GameException {
        // setup
        var joe = pm.registerPlayer("joe", VOID_LISTENER);
        var juan = pm.registerPlayer("juan", VOID_LISTENER);
        pm.removePlayer(joe.getPlayerId());
        pm.removePlayer(juan.getPlayerId());

        // when, then
        assertThatThrownBy(() -> pm.shiftPlayer()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void whenMiddlePlayerFinished_thenSkipHimWhenShifting() throws Exception {
        // setup
        setField(pm, "MAX_PLAYERS", 3);
        long turnTimeoutMs = 30_000;
        setField(pm, "turnTimeoutMs", turnTimeoutMs);
        var collector = createCollector();
        var p1 = pm.registerPlayer("jose", collector.getListener());
        var p2 = pm.registerPlayer("juan", collector.getListener());
        var p3 = pm.registerPlayer("george", collector.getListener());
        p2.deactivate();
        when(random.nextInt(any(int.class))).thenReturn(0);
        pm.initializePlayer();
        collector.clear();

        // when
        pm.shiftPlayer();

        // then
        assertThat(pm.currentPlayer()).isEqualTo(p3);
        assertThat(collector.getActions(p1)).containsExactly(new PlayerShiftAction(p3, turnTimeoutMs));
        assertThat(collector.getActions(p2)).containsExactly(new PlayerShiftAction(p3, turnTimeoutMs));
        assertThat(collector.getActions(p3)).containsExactly(new PlayerShiftAction(p3, turnTimeoutMs));
    }

    @Test
    void whenFirstPlayerIsFinished_thenInitiateToSecond() throws Exception {
        // setup
        setField(pm, "MAX_PLAYERS", 3);
        var collector = createCollector();
        var p1 = pm.registerPlayer("jose", collector.getListener());
        var p2 = pm.registerPlayer("juan", collector.getListener());
        p1.deactivate();
        when(random.nextInt(any(int.class))).thenReturn(0); // starting from first one
        collector.clear();

        // when
        pm.initializePlayer();

        // then
        assertThat(pm.currentPlayer()).isEqualTo(p2);
        assertThat(collector.getActions(p1)).containsExactly(new PlayerShiftAction(p2, 0));
        assertThat(collector.getActions(p2)).containsExactly(new PlayerShiftAction(p2, 0));
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
        long turnTimeoutMs = 30_000;
        setField(pm, "turnTimeoutMs", turnTimeoutMs);

        // when
        pm.initializePlayer(); // p1
        pm.shiftPlayer(); // p2
        p2.deactivate();
        pm.shiftPlayer(); // p3
        pm.shiftPlayer(); // p1
        pm.shiftPlayer(); // p3

        // then
        var expectedActions = of(
                new PlayerShiftAction(p1, turnTimeoutMs),
                new PlayerShiftAction(p2, turnTimeoutMs),
                new PlayerShiftAction(p3, turnTimeoutMs),
                new PlayerShiftAction(p1, turnTimeoutMs),
                new PlayerShiftAction(p3, turnTimeoutMs)
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
        juan.deactivate();

        // when
        assertThatThrownBy(() -> pm.validateCanStart())
                .isInstanceOf(GameException.class);
    }

    @Test
    void whenPlayerRemoved_andOnlyOneRemains_winAndTriggerClose() throws GameException {
        // setup
        var joseActions = new LinkedList<Action>();
        var juanActions = new LinkedList<Action>();
        var jose = pm.registerPlayer("jose", (p, e) -> joseActions.add(e));
        var juan = pm.registerPlayer("juan", (p, e) -> juanActions.add(e));
        pm.initializePlayer();
        joseActions.clear();
        juanActions.clear();
        stage.set(RUNNING);

        // when
        pm.removePlayer(jose.getPlayerId());

        // then
        assertThat(pm.getActiveCounter()).isZero();
        var expectedActions = of(
                new RemovePlayerAction(jose, 0),
                new EndAction(new LinkedList<>(of("juan", "jose")))
        );
        assertThat(juanActions).containsExactlyElementsOf(expectedActions);
    }

    @Test
    void whenOnlyOnePlayerAndRemoved_thenCloseTheGame() throws GameException {
        // setup
        var joseActions = new LinkedList<Action>();
        var jose = pm.registerPlayer("jose", (p, e) -> joseActions.add(e));
        pm.initializePlayer();
        joseActions.clear();

        // when
        pm.removePlayer(jose.getPlayerId());

        // then
        assertThat(pm.getActiveCounter()).isZero();
        assertThat(joseActions).isEmpty();
    }

    @Test
    void whenRemovePlayerFromInitialized_thenDisqualifyAndRemoveRecycleCards() throws Exception {
        // setup
        setField(pm, "MAX_PLAYERS", 3);
        var jose = pm.registerPlayer("jose", VOID_LISTENER);
        pm.registerPlayer("juan", VOID_LISTENER);
        pm.registerPlayer("Ki Hun", VOID_LISTENER);
        jose.getHand().addAll(List.of(new Card(CardType.SEVEN, Color.HEARTS), new Card(CardType.SEVEN, Color.SPADES)));
        pm.initializePlayer();
        stage.set(RUNNING);

        // when, then
        pm.removePlayer(jose.getPlayerId());

        assertThat(pm.getActiveCounter()).isEqualTo(2);
        assertThat(jose.getHand()).isEmpty();
        verify(cardManager).addToDeck(jose.getHand());
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
}