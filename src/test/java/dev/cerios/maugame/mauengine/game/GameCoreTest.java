package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardManager;
import dev.cerios.maugame.mauengine.exception.CardException;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.action.*;
import dev.cerios.maugame.mauengine.game.effect.DrawEffect;
import dev.cerios.maugame.mauengine.game.effect.SkipEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedList;
import java.util.List;

import static dev.cerios.maugame.mauengine.TestUtils.*;
import static dev.cerios.maugame.mauengine.card.CardType.*;
import static dev.cerios.maugame.mauengine.card.Color.*;
import static dev.cerios.maugame.mauengine.game.Stage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameCoreTest {
    private final static String GAME_EFFECT_FIELD = "gameEffect";
    private final static String STAGE_FIELD = "stage";

    private GameCore gameCore;
    @Mock
    private PlayerManager playerManager;
    @Mock
    private CardManager cardManager;

    @BeforeEach
    void setUp() {
        this.gameCore = new GameCore(cardManager, playerManager);
    }

    @Test
    void shouldStartCorrectly() throws GameException, CardException {
        // setup
        var joe = new Player("1", "joe", VOID_LISTENER);
        var juan = new Player("2", "juan", VOID_LISTENER);
        doNothing().when(playerManager).validateCanStart();
        var joeCardsExpected = List.of(
                new Card(SEVEN, SPADES),
                new Card(EIGHT, SPADES),
                new Card(NINE, SPADES),
                new Card(TEN, SPADES)
        );
        var juanCardsExpected = List.of(
                new Card(SEVEN, HEARTS),
                new Card(EIGHT, HEARTS),
                new Card(NINE, HEARTS),
                new Card(TEN, HEARTS)
        );
        when(cardManager.draw(4)).thenReturn(joeCardsExpected, juanCardsExpected);
        when(cardManager.startPile()).thenReturn(new Card(KING, SPADES));
        when(playerManager.getPlayers()).thenReturn(List.of(joe, juan));

        // when
        var pileCard = gameCore.start();

        // then
        assertThat(pileCard).isEqualTo(new Card(KING, SPADES));
        verify(cardManager, times(2)).draw(4);
        verify(cardManager).startPile();
        assertThat(joe.getHand()).containsExactlyElementsOf(joeCardsExpected);
        assertThat(juan.getHand()).containsExactlyElementsOf(juanCardsExpected);
        assertThat(gameCore.getStage()).isEqualTo(RUNNING);
    }

    @Test
    void whenCardManagerThrowsExceptionWhenDrawing_thenTranslateToUnchecked() throws GameException, CardException {
        /*
        Point of translation is that this scenario happens only due to wrong game setup.
        At the start of the game, there should be always enough cards for each player to draw four cards.
        Game counts with maximum five players present.
         */
        // setup
        var jose = new Player("1", "jose", VOID_LISTENER);
        var juan = new Player("2", "juan", VOID_LISTENER);
        doNothing().when(playerManager).validateCanStart();
        when(playerManager.getPlayers()).thenReturn(List.of(jose, juan));
        when(cardManager.draw(4)).thenThrow(new CardException("draw exceeded"));

        // when, then
        assertThatThrownBy(() -> gameCore.start())
                .isInstanceOf(IllegalStateException.class)
                .hasCause(new CardException("draw exceeded"));
    }

    @Test
    void whenGameStartedInNotLobbyMode_thenThrow() throws Exception {
        // setup
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when, then
        assertThatThrownBy(() -> gameCore.start())
                .isInstanceOf(GameException.class);
    }

    @Test
    void whenPerformPass_andPlayerNotInGame_thenThrow() throws Exception {
        // setup
        final String playerId = "1";
        var exception = new GameException();
        when(playerManager.getPlayer(playerId)).thenThrow(exception);
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when
        assertThatThrownBy(() -> gameCore.performPass(playerId))
                .isSameAs(exception);
    }

    @Test
    void whenPerformPass_andPlayerNotOnTurn_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var currentPlayer = new Player("2", "juan", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(currentPlayer);
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when, then
        assertThatThrownBy(() -> gameCore.performPass(jose.getPlayerId()))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("turn", jose.getPlayerId());
    }

    @Test
    void whenPerformPass_andGameNotRunning_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        setField(gameCore, STAGE_FIELD, LOBBY);

        // when, then
        assertThatThrownBy(() -> gameCore.performPass(jose.getPlayerId()))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("not running");
    }

    @Test
    void whenPerformPass_andNoGameEffect_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, null);

        // when, then
        assertThatThrownBy(() -> gameCore.performPass(jose.getPlayerId()))
                .isInstanceOf(PlayerMoveException.class);
    }

    @Test
    void shouldPerformPassOnSkipEffect() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, new SkipEffect());

        // when
        gameCore.performPass(jose.getPlayerId());

        // then
        verify(playerManager).distributeActionToAll(new PassAction(jose));
        verify(playerManager).shiftPlayer();
        assertThat(getField(gameCore, GAME_EFFECT_FIELD)).isNull();
    }

    @Test
    void shouldPerformPassOnDrawEffect() throws Exception {
        // setup
        var playerActions = new LinkedList<>();
        final var jose = new Player("1", "jose", (p, e) -> playerActions.add(e));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.draw(2)).thenReturn(List.of(new Card(SEVEN, CLUBS), new Card(EIGHT, CLUBS)));
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, new DrawEffect(2));

        // when
        gameCore.performPass(jose.getPlayerId());

        // then
        assertThat(playerActions).containsExactly(new DrawAction(List.of(new Card(SEVEN, CLUBS), new Card(EIGHT, CLUBS))));
        assertThat(jose.getHand()).containsExactly(new Card(SEVEN, CLUBS), new Card(EIGHT, CLUBS));
        verify(playerManager).distributeActionExcludingPlayer(new HiddenDrawAction(jose, 2), jose.getPlayerId());
        verify(playerManager).shiftPlayer();
        assertThat(getField(gameCore, GAME_EFFECT_FIELD)).isNull();
    }

    @Test
    void whenPerformDraw_andPlayerNotOnTurn_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var currentPlayer = new Player("2", "juan", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(currentPlayer);
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when, then
        assertThatThrownBy(() -> gameCore.performDraw(jose.getPlayerId()))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("turn", jose.getPlayerId());
    }

    @Test
    void whenPerformDraw_andPlayerNotInGame_thenThrow() throws Exception {
        // setup
        final String playerId = "1";
        var exception = new GameException();
        when(playerManager.getPlayer(playerId)).thenThrow(exception);
    setField(gameCore, STAGE_FIELD, RUNNING);

        // when
        assertThatThrownBy(() -> gameCore.performDraw(playerId))
                .isSameAs(exception);
    }

    @Test
    void whenPerformDraw_andGameNotRunning_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        setField(gameCore, STAGE_FIELD, LOBBY);

        // when, then
        assertThatThrownBy(() -> gameCore.performDraw(jose.getPlayerId()))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("not running");
    }

    @Test
    void whenPerformDraw_andThereIsGameEffect_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        setField(gameCore, GAME_EFFECT_FIELD, new SkipEffect());
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when, then
        assertThatThrownBy(() -> gameCore.performDraw(jose.getPlayerId()))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("game", "effect", "active");
    }

    @Test
    void shouldPerformDrawSuccessfully() throws Exception {
        // setup
        var actionCollector = createCollector();
        final var jose = new Player("1", "jose", actionCollector.getListener());
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.draw()).thenReturn(new Card(SEVEN, CLUBS));
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when
        gameCore.performDraw(jose.getPlayerId());

        // then
        verify(playerManager).distributeActionExcludingPlayer(new HiddenDrawAction(jose, 1), jose.getPlayerId());
        assertThat(actionCollector.getActions(jose)).containsExactly(new DrawAction(List.of(new Card(SEVEN, CLUBS))));
        verify(playerManager).shiftPlayer();
    }

    @Test
    void whenPerformPlayCard_andPlayerNotOnTurn_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var currentPlayer = new Player("2", "juan", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(currentPlayer);
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when, then
        assertThatThrownBy(() -> gameCore.performPlayCard(jose.getPlayerId(), new Card(ACE, HEARTS)))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("turn", jose.getPlayerId());
    }

    @Test
    void whenPerformPlayCard_andPlayerNotInGame_thenThrow() throws Exception {
        // setup
        final String playerId = "1";
        var exception = new GameException();
        when(playerManager.getPlayer(playerId)).thenThrow(exception);
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when
        assertThatThrownBy(() -> gameCore.performPlayCard(playerId, new Card(ACE, HEARTS)))
                .isSameAs(exception);
    }

    @Test
    void whenPerformPlayCard_andGameNotRunning_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        setField(gameCore, STAGE_FIELD, LOBBY);

        // when, then
        assertThatThrownBy(() -> gameCore.performPlayCard(jose.getPlayerId(), new Card(ACE, HEARTS)))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("not running");
    }

    @Test
    void whenPerformPlayCard_andCardNotInHand_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        jose.getHand().add(new Card(SEVEN, CLUBS));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        setField(gameCore, STAGE_FIELD, RUNNING);

        // when
        assertThatThrownBy(() -> gameCore.performPlayCard(jose.getPlayerId(), new Card(ACE, HEARTS)))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("not", "in", "hand");
    }

    @Test
    void whenPerformPlayValidCardWithoutGameEffect_thatIsNotLast_andNoEffectActive_thenProceed() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var noEffectCard = new Card(EIGHT, CLUBS);
        final var anotherCard = new Card(TEN, CLUBS);
        jose.getHand().addAll(List.of(noEffectCard, anotherCard));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(noEffectCard, null)).thenReturn(true);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, null);

        // when
        gameCore.performPlayCard(jose.getPlayerId(), noEffectCard);

        // then
        assertThat(jose.getHand()).containsExactly(anotherCard);
        verify(playerManager, never()).playerWin(any(Player.class));
        verify(playerManager).distributeActionToAll(new PlayCardAction(jose, noEffectCard));
        verify(playerManager).shiftPlayer();
    }

    @Test
    void whenPlayCardInvalidCardWithoutEffect_andNoEffectActive_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var noEffectCard = new Card(EIGHT, CLUBS);
        jose.getHand().add(noEffectCard);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(noEffectCard, null)).thenReturn(false);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, null);

        // when, then
        assertThatThrownBy(() -> gameCore.performPlayCard(jose.getPlayerId(), noEffectCard))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("Illegal", "card");
        assertThat(jose.getHand()).containsExactly(noEffectCard);
        verify(playerManager, never()).distributeActionToAll(any(Action.class));
        verify(playerManager, never()).playerWin(any(Player.class));
    }

    @Test
    void whenPlayValidCardWithDrawEffectNonLast_andNoEffectActive_thenProceedAndStore() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var drawEffectCard = new Card(SEVEN, CLUBS);
        final var anotherCard = new Card(TEN, CLUBS);
        jose.getHand().addAll(List.of(drawEffectCard, anotherCard));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(drawEffectCard, null)).thenReturn(true);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, null);

        // when
        gameCore.performPlayCard(jose.getPlayerId(), drawEffectCard);

        // then
        assertThat(jose.getHand()).containsExactly(anotherCard);
        assertThat(gameCore.getGameEffect()).isEqualTo(new DrawEffect(2));
        verify(playerManager, never()).playerWin(any(Player.class));
        verify(playerManager).distributeActionToAll(new PlayCardAction(jose, drawEffectCard));
        verify(playerManager).shiftPlayer();
    }

    @Test
    void whenPlayValidCardWithSkipEffectNonLast_andNoEffectActive_thenProceedAndStore() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var drawEffectCard = new Card(ACE, CLUBS);
        final var anotherCard = new Card(TEN, CLUBS);
        jose.getHand().addAll(List.of(drawEffectCard, anotherCard));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(drawEffectCard, null)).thenReturn(true);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, null);

        // when
        gameCore.performPlayCard(jose.getPlayerId(), drawEffectCard);

        // then
        assertThat(jose.getHand()).containsExactly(anotherCard);
        assertThat(gameCore.getGameEffect()).isEqualTo(new SkipEffect());
        verify(playerManager, never()).playerWin(any(Player.class));
        verify(playerManager).distributeActionToAll(new PlayCardAction(jose, drawEffectCard));
        verify(playerManager).shiftPlayer();
    }

    @Test
    void whenPlayValidCard_thatIsLast_andGameShouldContinue_thenProceedAndWin() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var lastCard = new Card(SEVEN, CLUBS);
        jose.getHand().add(lastCard);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(lastCard, null)).thenReturn(true);
        when(playerManager.playerWin(jose)).thenReturn(true);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, null);

        // when
        gameCore.performPlayCard(jose.getPlayerId(), lastCard);

        // then
        assertThat(jose.getHand()).isEmpty();
        assertThat(gameCore.getStage()).isSameAs(RUNNING);
        verify(playerManager).distributeActionToAll(new PlayCardAction(jose, lastCard));
        verify(playerManager).shiftPlayer();
    }

    @Test
    void whenPlayValidCard_thatIsLast_andGameShouldNotContinue_thenProceedAndWin() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var lastCard = new Card(SEVEN, CLUBS);
        jose.getHand().add(lastCard);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(lastCard, null)).thenReturn(true);
        when(playerManager.playerWin(jose)).thenReturn(false);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, null);

        // when
        gameCore.performPlayCard(jose.getPlayerId(), lastCard);

        // then
        assertThat(jose.getHand()).isEmpty();
        assertThat(gameCore.getStage()).isSameAs(FINISH);
        verify(playerManager).distributeActionToAll(any(PlayCardAction.class));
        verify(playerManager, never()).shiftPlayer();
    }

    @Test
    void whenPlaySeven_andDrawEffectActive_thenEmpowerEffect() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var effectCard = new Card(SEVEN, CLUBS);
        final var anotherCard = new Card(TEN, CLUBS);
        jose.getHand().addAll(List.of(effectCard, anotherCard));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(effectCard, null)).thenReturn(true);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, new DrawEffect(2));

        // when
        gameCore.performPlayCard(jose.getPlayerId(), effectCard);

        // then
        assertThat(jose.getHand()).containsExactly(anotherCard);
        assertThat(gameCore.getGameEffect()).isEqualTo(new DrawEffect(4));
        verify(playerManager, never()).playerWin(any(Player.class));
        verify(playerManager).distributeActionToAll(new PlayCardAction(jose, effectCard));
        verify(playerManager).shiftPlayer();
    }

    @Test
    void whenPlayRandomCard_whenDrawEffectActive_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var playCard = new Card(EIGHT, CLUBS);
        final var anotherCard = new Card(TEN, CLUBS);
        jose.getHand().addAll(List.of(playCard, anotherCard));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, new DrawEffect(2));

        // when, then
        assertThatThrownBy(() -> gameCore.performPlayCard(jose.getPlayerId(), playCard))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("Illegal", "card");

        assertThat(jose.getHand()).containsExactly(playCard, anotherCard);
        assertThat(gameCore.getGameEffect()).isEqualTo(new DrawEffect(2));
    }

    @Test
    void whenPlayInvalidSeven_whenDrawEffectActive_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var playCard = new Card(SEVEN, CLUBS);
        jose.getHand().add(playCard);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(playCard, null)).thenReturn(false); // invalid from here
        setField(gameCore, STAGE_FIELD, RUNNING);
        setField(gameCore, GAME_EFFECT_FIELD, new DrawEffect(2));

        // when, then
        assertThatThrownBy(() -> gameCore.performPlayCard(jose.getPlayerId(), playCard))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("Illegal", "card");
        assertThat(jose.getHand()).containsExactly(playCard);
        assertThat(gameCore.getGameEffect()).isEqualTo(new DrawEffect(2));
    }

    @Test
    void whenPlayAce_andSkipEffectActive_thenKeepEffect() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var effectCard = new Card(ACE, CLUBS);
        final var anotherCard = new Card(TEN, CLUBS);
        jose.getHand().addAll(List.of(effectCard, anotherCard));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(effectCard, null)).thenReturn(true);
        setField(gameCore, STAGE_FIELD, RUNNING);
        var skipEffect = new SkipEffect();
        setField(gameCore, GAME_EFFECT_FIELD, skipEffect);

        // when
        gameCore.performPlayCard(jose.getPlayerId(), effectCard);

        // then
        assertThat(jose.getHand()).containsExactly(anotherCard);
        assertThat(gameCore.getGameEffect()).isSameAs(skipEffect);
        verify(playerManager, never()).playerWin(any(Player.class));
        verify(playerManager).distributeActionToAll(new PlayCardAction(jose, effectCard));
        verify(playerManager).shiftPlayer();
    }

    @Test
    void whenPlayRandomCard_whenSkipEffectActive_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var playCard = new Card(EIGHT, CLUBS);
        final var anotherCard = new Card(TEN, CLUBS);
        jose.getHand().addAll(List.of(playCard, anotherCard));
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        setField(gameCore, STAGE_FIELD, RUNNING);
        var skipEffect = new SkipEffect();
        setField(gameCore, GAME_EFFECT_FIELD, skipEffect);

        // when, then
        assertThatThrownBy(() -> gameCore.performPlayCard(jose.getPlayerId(), playCard))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("Illegal", "card");

        assertThat(jose.getHand()).containsExactly(playCard, anotherCard);
        assertThat(gameCore.getGameEffect()).isSameAs(skipEffect);
    }

    @Test
    void whenPlayInvalidAce_whenSkipEffectActive_thenThrow() throws Exception {
        // setup
        final var jose = new Player("1", "jose", VOID_LISTENER);
        final var playCard = new Card(ACE, CLUBS);
        jose.getHand().add(playCard);
        when(playerManager.getPlayer(jose.getPlayerId())).thenReturn(jose);
        when(playerManager.currentPlayer()).thenReturn(jose);
        when(cardManager.playCard(playCard, null)).thenReturn(false); // invalid from here
        setField(gameCore, STAGE_FIELD, RUNNING);
        var skipEffect = new SkipEffect();
        setField(gameCore, GAME_EFFECT_FIELD, skipEffect);

        // when, then
        assertThatThrownBy(() -> gameCore.performPlayCard(jose.getPlayerId(), playCard))
                .isInstanceOf(PlayerMoveException.class)
                .hasMessageContainingAll("Illegal", "card");
        assertThat(jose.getHand()).containsExactly(playCard);
        assertThat(gameCore.getGameEffect()).isEqualTo(skipEffect);
    }
}
