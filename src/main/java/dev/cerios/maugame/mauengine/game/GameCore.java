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
import dev.cerios.maugame.mauengine.player.Player;
import dev.cerios.maugame.mauengine.player.PlayerManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

import static dev.cerios.maugame.mauengine.game.Stage.*;

@RequiredArgsConstructor
public class GameCore {

    private final CardManager cardManager;
    private final CardComparer cardComparer;
    private final PlayerManager playerManager;

    private final List<String> playerRank = new ArrayList<>(5);
    private GameEffect gameEffect = null;
    @Getter
    private volatile Stage stage = LOBBY;

    public  List<Action> performPlayCard(final String playerId, Card card) throws MauEngineBaseException {
        return performPlayCard(playerId, card, null);
    }

    public List<Action> performPlayCard(final String playerId, Card card, Color nextColor) throws MauEngineBaseException {
        validateActivePlayer(playerId);

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
                return actions;
            }
        }

        actions.add(playerManager.shiftPlayer());
        return actions;
    }

    public List<Action> performDraw(final String playerId, int cardCount) throws MauEngineBaseException {
        validateActivePlayer(playerId);

        if (cardCount != 1) {
            throw new PlayerMoveException("illegal card draw count: " + cardCount);
        }

        if (gameEffect != null) {
            throw new PlayerMoveException("illegal move");
        }
        var drawnCard = cardManager.draw();
        playerManager.getPlayer(playerId).getHand().add(drawnCard);
        return List.of(
                new DrawAction(playerId, List.of(drawnCard)),
                playerManager.shiftPlayer()
        );
    }

    public List<Action> performPass(final String playerId) throws MauEngineBaseException {
        validateActivePlayer(playerId);

        if (gameEffect == null) {
            throw new PlayerMoveException("illegal move");
        }

        List<Action> actions = new LinkedList<>();

        switch (gameEffect) {
            case DrawEffect(int count) -> {
                var drawnCards = cardManager.draw(count);
                playerManager.getPlayer(playerId).getHand().addAll(drawnCards);
                actions.add(new DrawAction(playerId, drawnCards));
            }
            case SkipEffect ignore -> actions.add(new PassAction(playerId));
        }

        gameEffect = null;

        actions.add(playerManager.shiftPlayer());
        return actions;
    }

//    public GameState getCurrentState() throws GameException {
//        return new GameState(
//                playerRank,
//                playerManager.getPlayersById(),
//                cardManager.peekPile(),
//                cardManager.deckSize(),
//                stage,
//                playerManager.currentPlayer(),
//                gameEffect
//        );
//    }

    public List<Action> start() throws GameException, PlayerMoveException {
        if (stage != LOBBY)
            throw new GameException("The game has already started.");
        playerManager.validateCanStart();

        for (Player player : playerManager.getPlayers()) {
            var drawnCards = cardManager.draw(4);
            player.getHand().addAll(drawnCards);
        }

        stage = RUNNING;
        List<Action> actions = new LinkedList<>();
        actions.add(new StartPileAction(cardManager.startPile()));
        actions.add(playerManager.initializePlayer());

        for (Player player : playerManager.getPlayersById().values())
            actions.add(new DrawAction(player.getPlayerId(), player.getHand()));
        return actions;
    }

    private void validateActivePlayer(String playerId) throws PlayerMoveException, GameException {
        if (stage != RUNNING) {
            throw new PlayerMoveException("The game not running.");
        }
        if (!playerId.equals(playerManager.currentPlayer())) {
            throw new PlayerNotActiveException(playerId);
        }
    }
}
