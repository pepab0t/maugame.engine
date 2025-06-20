package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.*;
import dev.cerios.maugame.mauengine.exception.GameException;
import dev.cerios.maugame.mauengine.exception.MauEngineBaseException;
import dev.cerios.maugame.mauengine.exception.PlayerMoveException;
import dev.cerios.maugame.mauengine.exception.PlayerNotActiveException;
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
    private final CardComparer cardComparer;
    private final PlayerManager playerManager;

    private final List<String> playerRank = new ArrayList<>(PlayerManager.MAX_PLAYERS);
    private GameEffect gameEffect = null;
    @Getter
    private volatile Stage stage = LOBBY;

    public void performPlayCard(final String playerId, Card card) throws MauEngineBaseException {
        performPlayCard(playerId, card, null);
    }

    public void performPlayCard(final String playerId, Card card, Color nextColor) throws MauEngineBaseException {
        validatePlayerPlay(playerId);

        List<Card> playerHand = playerManager.getPlayer(playerId).getHand();
        final int cardIndex = playerHand.indexOf(card);
        if (cardIndex == -1)
            throw new PlayerMoveException("Player does not have in hand: " + card);

        List<Action> actions = new LinkedList<>();

        if (gameEffect == null) {
            Card pileCard = cardManager.peekPile();
            if (!cardComparer.compare(pileCard, card))
                throw new PlayerMoveException("Illegal card to play");
            cardComparer.clear();
            switch (card.type()) {
                case QUEEN -> {
                    if (nextColor == null)
                        throw new PlayerMoveException("No next color specified, when played QUEEN.");
                    cardComparer.setNextColor(nextColor);
                }
                case ACE -> gameEffect = new SkipEffect();
                case SEVEN -> gameEffect = new DrawEffect(2);
            }
            cardManager.playCard(card);
            actions.add(new PlayCardAction(playerId, card));
        } else {
            switch (gameEffect) {
                case DrawEffect(int count) -> {
                    if (card.type() != CardType.SEVEN)
                        throw new PlayerMoveException("illegal card");
                    gameEffect = new DrawEffect(count + 2);
                    cardManager.playCard(card);
                    actions.add(new PlayCardAction(playerId, card));
                }
                case SkipEffect ignore -> {
                    if (card.type() != CardType.ACE)
                        throw new PlayerMoveException("illegal card");
                    cardManager.playCard(card);
                    actions.add(new PlayCardAction(playerId, card));
                }
            }
        }
        playerHand.remove(cardIndex);
        if (playerHand.isEmpty()) {
            playerManager.deactivatePlayer(playerId);
            playerRank.add(playerId);
            actions.add(new WinAction(playerId));

            if (playerManager.getActiveCounter() == 1) {
                String losingPlayer = playerManager.getPlayers().stream().filter(Player::isActive).findFirst().map(Player::getPlayerId).get();
                playerRank.add(losingPlayer);
                actions.add(new LoseAction(losingPlayer)); // redundant
                actions.add(new SendRankAction(playerRank));
                actions.add(new EndAction());
                stage = FINISH;
                actions.forEach(playerManager::distributeActionToAll);
                return;
            }
        }

        actions.forEach(playerManager::distributeActionToAll);
        playerManager.shiftPlayer();
    }

    public void performDraw(final String playerId, int cardCount) throws MauEngineBaseException {
        validatePlayerPlay(playerId);

        if (cardCount != 1) {
            throw new PlayerMoveException("illegal card draw count: " + cardCount);
        }

        if (gameEffect != null) {
            throw new PlayerMoveException("illegal move");
        }
        var drawnCard = cardManager.draw();
        var player = playerManager.getPlayer(playerId);
        player.getHand().add(drawnCard);

        playerManager.distributeActionExcludingPlayer(new HiddenDrawAction(player, (byte) 1), playerId);
        player.trigger(new DrawAction(List.of(drawnCard)));
        playerManager.shiftPlayer();
    }

    public void performPass(final String playerId) throws MauEngineBaseException {
        validatePlayerPlay(playerId);

        if (gameEffect == null) {
            throw new PlayerMoveException("illegal move");
        }

        switch (gameEffect) {
            case DrawEffect(int count) -> {
                var player = playerManager.getPlayer(playerId);
                var drawnCards = cardManager.draw(count);
                player.getHand().addAll(drawnCards);

                playerManager.distributeActionExcludingPlayer(
                        new HiddenDrawAction(player, (byte) drawnCards.size()),
                        playerId
                );
                player.trigger(new DrawAction(drawnCards));
            }
            case SkipEffect ignore -> playerManager.distributeActionToAll(new PassAction(playerId));
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
            var drawnCards = cardManager.draw(4);
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

    private void validatePlayerPlay(String playerId) throws PlayerMoveException {
        if (stage != RUNNING) {
            throw new PlayerMoveException("The game not running.");
        }
        if (!playerId.equals(playerManager.currentPlayer().getPlayerId())) {
            throw new PlayerNotActiveException(playerId);
        }
    }
}
