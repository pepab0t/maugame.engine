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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static dev.cerios.maugame.mauengine.game.Stage.*;

@RequiredArgsConstructor
class GameCore {

    private final CardManager cardManager;
    private final PlayerManager playerManager;

    @Getter
    private volatile GameEffect gameEffect = null;
    @Getter
    private volatile Stage stage = LOBBY;

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
            actions.add(new PlayCardAction(player, card));
        } else {
            switch (gameEffect) {
                case DrawEffect(int count) -> {
                    if (card.type() != CardType.SEVEN)
                        throw new PlayerMoveException("Illegal card to play.");
                    if (!cardManager.playCard(card, null))
                        throw new PlayerMoveException("Illegal card to play.");
                    gameEffect = new DrawEffect(count + 2);
                    actions.add(new PlayCardAction(player, card));
                }
                case SkipEffect ignore -> {
                    if (card.type() != CardType.ACE)
                        throw new PlayerMoveException("Illegal card to play.");
                    if (!cardManager.playCard(card, null))
                        throw new PlayerMoveException("Illegal card to play.");
                    actions.add(new PlayCardAction(player, card));
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

    public void performDraw(final String playerId) throws MauEngineBaseException {
        var player = playerManager.getPlayer(playerId);
        validatePlayerPlay(playerId);

        if (gameEffect != null)
            throw new PlayerMoveException("Cannot draw when when game effect is active.");

        var drawnCard = cardManager.draw();
        player.getHand().add(drawnCard);

        playerManager.distributeActionExcludingPlayer(new HiddenDrawAction(player, 1), playerId);
        player.trigger(new DrawAction(List.of(drawnCard)));
        playerManager.shiftPlayer();
    }

    public void performPass(final String playerId) throws MauEngineBaseException {
        var player = playerManager.getPlayer(playerId);
        validatePlayerPlay(playerId);

        switch (gameEffect) {
            case DrawEffect(int count) -> {
                var drawnCards = cardManager.draw(count);
                player.getHand().addAll(drawnCards);
                playerManager.distributeActionExcludingPlayer(
                        new HiddenDrawAction(player, drawnCards.size()),
                        playerId
                );
                player.trigger(new DrawAction(drawnCards));
            }
            case SkipEffect ignore -> playerManager.distributeActionToAll(new PassAction(player));
            case null -> throw new PlayerMoveException("cannot pass without active game effect");
        }
        gameEffect = null;
        playerManager.shiftPlayer();
    }


    public Card start() throws GameException {
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
        return cardManager.startPile();
    }

    private void validatePlayerPlay(String playerId) throws MauEngineBaseException {
        if (stage != RUNNING) {
            throw new GameException("The game not running.");
        }
        if (!playerId.equals(playerManager.currentPlayer().getPlayerId())) {
            throw new PlayerMoveException("Not " + playerId + "'s turn.");
        }
    }

    public int getDeckSize() {
        return cardManager.deckSize();
    }

    public Card getPileCard() {
        return cardManager.peekPile();
    }
}
