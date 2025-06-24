package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardManager;
import dev.cerios.maugame.mauengine.card.CardType;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.CardException;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.game.action.*;
import dev.cerios.maugame.mauengine.game.effect.DrawEffect;
import dev.cerios.maugame.mauengine.game.effect.GameEffect;
import dev.cerios.maugame.mauengine.game.effect.SkipEffect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static dev.cerios.maugame.mauengine.game.Stage.*;

@RequiredArgsConstructor
class GameCore {

    private final CardManager cardManager;
    private final PlayerManager playerManager;

    private final List<String> playerRank;
    private volatile GameEffect gameEffect = null;
    @Getter
    private volatile Stage stage = LOBBY;

    public GameCore(CardManager cardManager, PlayerManager playerManager) {
        this.cardManager = cardManager;
        this.playerManager = playerManager;
        this.playerRank = new ArrayList<>(playerManager.MAX_PLAYERS);
    }

    public void performPlayCard(final String playerId, Card card) throws MauEngineBaseException {
        performPlayCard(playerId, card, null);
    }

    public void performPlayCard(final String playerId, Card card, Color nextColor) throws MauEngineBaseException {
        var player = playerManager.getPlayer(playerId);
        validatePlayerPlay(playerId);

        List<Card> playerHand = player.getHand();
        final int cardIndex = playerHand.indexOf(card);
        if (cardIndex == -1)
            throw new PlayerMoveException("Player does not have in hand: " + card);

        List<Action> actions = new LinkedList<>();

        if (gameEffect == null) {
            if (!cardManager.playCard(card, nextColor))
                throw new PlayerMoveException("Illegal card to play");
            switch (card.type()) {
                case ACE -> gameEffect = new SkipEffect();
                case SEVEN -> gameEffect = new DrawEffect(2);
            }
            actions.add(new PlayCardAction(playerId, card));
        } else {
            switch (gameEffect) {
                case DrawEffect(int count) -> {
                    if (card.type() != CardType.SEVEN)
                        throw new PlayerMoveException("illegal card");
                    gameEffect = new DrawEffect(count + 2);
                    cardManager.playCard(card, null);
                    actions.add(new PlayCardAction(playerId, card));
                }
                case SkipEffect ignore -> {
                    if (card.type() != CardType.ACE)
                        throw new PlayerMoveException("illegal card");
                    cardManager.playCard(card, null);
                    actions.add(new PlayCardAction(playerId, card));
                }
            }
        }
        playerHand.remove(cardIndex);
        if (playerHand.isEmpty()) {
            var shouldContinue = playerManager.playerWin(player);
            if (!shouldContinue) {
                stage = FINISH;
                return;
            }
        }

        actions.forEach(playerManager::distributeActionToAll);
        playerManager.shiftPlayer();
    }

    public void performDraw(final String playerId, int cardCount) throws MauEngineBaseException {
        var player = playerManager.getPlayer(playerId);
        validatePlayerPlay(playerId);

        if (cardCount != 1) {
            throw new PlayerMoveException("illegal card draw count: " + cardCount);
        }

        if (gameEffect != null)
            throw new PlayerMoveException("cannot draw when when game effect is active");

        var drawnCard = cardManager.draw();
        player.getHand().add(drawnCard);

        playerManager.distributeActionExcludingPlayer(new HiddenDrawAction(player, (byte) 1), playerId);
        player.trigger(new DrawAction(List.of(drawnCard)));
        playerManager.shiftPlayer();
    }

    public void performPass(final String playerId) throws MauEngineBaseException {
        validatePlayerPlay(playerId);

        if (gameEffect == null)
            throw new PlayerMoveException("cannot pass without active game effect");

        var player = playerManager.getPlayer(playerId);
        switch (gameEffect) {
            case DrawEffect(int count) -> {
                var drawnCards = cardManager.draw(count);
                player.getHand().addAll(drawnCards);

                playerManager.distributeActionExcludingPlayer(
                        new HiddenDrawAction(player, (byte) drawnCards.size()),
                        playerId
                );
                player.trigger(new DrawAction(drawnCards));
            }
            case SkipEffect ignore -> playerManager.distributeActionToAll(new PassAction(player));
        }
        gameEffect = null;
        playerManager.shiftPlayer();
    }

    public GameState getCurrentState() {
        return new GameState(
                playerRank,
                playerManager.getPlayers().stream().collect(
                        HashMap::new,
                        (map, player) -> map.put(player.getUsername(), player.getHand()),
                        HashMap::putAll
                ),
                cardManager.peekPile(),
                cardManager.deckSize(),
                stage,
                playerManager.currentPlayer().getUsername(),
                gameEffect
        );
    }

    public void start() throws GameException {
        if (stage != LOBBY)
            throw new GameException("The game has already started.");

        playerManager.validateCanStart();

        for (Player player : playerManager.getPlayers()) {
            List<Card> drawnCards;
            try {
                drawnCards = cardManager.draw(4);
            } catch (CardException e) {
                throw new IllegalStateException(e);
            }
            player.getHand().addAll(drawnCards);
        }

        stage = RUNNING;

        playerManager.distributeActionToAll(new StartPileAction(cardManager.startPile()));
        playerManager.initializePlayer();

        for (Player player : playerManager.getPlayers()) {
            player.trigger(new DrawAction(player.getHand()));
            playerManager.distributeActionExcludingPlayer(
                    new HiddenDrawAction(player, (byte) player.getHand().size()),
                    player.getPlayerId()
            );
        }
    }

    private void validatePlayerPlay(String playerId) throws MauEngineBaseException {
        if (stage != RUNNING) {
            throw new GameException("The game not running.");
        }
        if (!playerId.equals(playerManager.currentPlayer().getPlayerId())) {
            throw new PlayerMoveException(playerId);
        }
    }
}
