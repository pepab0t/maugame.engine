package dev.cerios.maugame.mauengine.game;

import dev.cerios.maugame.mauengine.card.Card;
import dev.cerios.maugame.mauengine.card.CardManager;
import dev.cerios.maugame.mauengine.card.CardType;
import dev.cerios.maugame.mauengine.card.Color;
import dev.cerios.maugame.mauengine.exception.*;
import dev.cerios.maugame.mauengine.game.action.*;
import dev.cerios.maugame.mauengine.game.effect.DrawEffect;
import dev.cerios.maugame.mauengine.game.effect.GameEffect;
import dev.cerios.maugame.mauengine.game.effect.SkipEffect;
import dev.cerios.maugame.mauengine.player.ActionPublisher;
import dev.cerios.maugame.mauengine.player.Player;
import dev.cerios.maugame.mauengine.player.PlayerContext;
import dev.cerios.maugame.mauengine.player.PlayerRunningState;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

class GameCore {
    private final CardManager cardManager;
    private final PlayerContext playerContext;

    @Getter
    private volatile GameEffect gameEffect = null;
    @Getter
    private final UUID id = UUID.randomUUID();

    public GameCore(
            CardManager cardManager,
            PlayerContext playerContext
    ) {
        this.cardManager = cardManager;
        this.playerContext = playerContext;

        this.playerContext.listenPlayerTimeout(this::restorePlayerCards);
        this.playerContext.listenStartGame(this::start);
    }

    private void restorePlayerCards(Player player) {
        var hand = player.getHand();
        cardManager.addToDeck(hand);
        hand.clear();
    }

    public void performPlayCard(final String playerId, Card card) throws MauEngineBaseException {
        performPlayCard(playerId, card, null);
    }

    public void performPlayCard(final String playerId, Card card, Color nextColor) throws MauEngineBaseException {
        var players = getRunningState();
        players.getPlayerForPlay(playerId, (publisher, player) -> playCardInternal(publisher, player, card, nextColor));
    }

    private boolean playCardInternal(ActionPublisher publisher, Player player, Card card, Color nextColor) throws PlayerMoveException, CardException {
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
            actions.add(new PlayCardAction(player, card, nextColor));
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
        actions.forEach(publisher::publishActionToAll);
        playerHand.remove(cardIndex);
        return playerHand.isEmpty();
    }

    public void performDraw(final String playerId) throws MauEngineBaseException {
        var players = getRunningState();
        players.getPlayerForPlay(playerId, this::drawInternal);
    }

    private boolean drawInternal(ActionPublisher publisher, Player player) throws PlayerMoveException, CardException {
        if (gameEffect != null)
            throw new PlayerMoveException("Cannot draw when when game effect is active.");

        var drawnCard = cardManager.draw();
        player.getHand().add(drawnCard);

        publisher.publishActionExcludingPlayer(new HiddenDrawAction(player, 1), player.getPlayerId());

        publisher.publishAction(player, new DrawAction(List.of(drawnCard)));
        return false;
    }

    public void performPass(final String playerId) throws MauEngineBaseException {
        var players = getRunningState();
        players.getPlayerForPlay(playerId, this::passInternal);
    }

    private boolean passInternal(ActionPublisher publisher, Player player) throws PlayerMoveException, CardException {
        switch (gameEffect) {
            case DrawEffect(int count) -> {
                var drawnCards = cardManager.draw(count);
                player.getHand().addAll(drawnCards);
                publisher.publishActionExcludingPlayer(
                        new HiddenDrawAction(player, drawnCards.size()),
                        player.getPlayerId()
                );
                publisher.publishAction(player, new DrawAction(drawnCards));
            }
            case SkipEffect ignore -> publisher.publishActionToAll(new PassAction(player));
            case null -> throw new PlayerMoveException("cannot pass without active game effect");
        }
        gameEffect = null;
        return false;
    }


    private void start(UUID gameId) {
        System.out.println("start called");
        var players = playerContext.getPlayers();

        for (Player player : players.getPlayers()) {
            List<Card> drawnCards;
            try {
                drawnCards = cardManager.draw(4);
            } catch (CardException e) {
                throw new IllegalStateException(e);
            }
            player.getHand().addAll(drawnCards);
        }
        var pileCard = cardManager.startPile();

        var publisher = players.getActionPublisher();

        publisher.publishActionToAll(new StartAction(gameId.toString()));
        publisher.publishActionToAll(new StartPileAction(pileCard));
        for (Player player : players.getPlayers()) {
            publisher.publishActionExcludingPlayer(new HiddenDrawAction(player, player.getHand().size()), player.getPlayerId());
            publisher.publishAction(player, new DrawAction(player.getHand()));
        }
    }

    public Card getPileCard() {
        return cardManager.peekPile();
    }

    public void sendCurrentStateTo(String playerId, Predicate<GamePlayer> playerMatcher) throws GameException {
        if (playerContext.getPlayers() instanceof PlayerRunningState players) {
            var player = players.getPlayer(playerId);
            if (!playerMatcher.test(player))
                throw new GameException("No matching player.");
            List<Action> actions = new LinkedList<>();

            actions.add(new StartAction(id.toString()));
            actions.add(new StartPileAction(getPileCard()));
            for (Player p : players.getPlayers()) {
                if (p.getPlayerId().equals(playerId))
                    actions.add(new DrawAction(p.getHand()));
                else
                    actions.add(new HiddenDrawAction(p, p.getHand().size()));
            }
            actions.add(new PlayerShiftAction(players.getCurrentPlayer(), players.getLastExpire(players.getCurrentPlayer().getPlayerId())));
            actions.add(new SendRankAction(players.getPlayerRank()));
            final var publisher = players.getActionPublisher();
            actions.forEach(a -> publisher.publishAction(player, a));
        }
    }

    private PlayerRunningState getRunningState() throws NotSupportedOperation {
        if (playerContext.getPlayers() instanceof PlayerRunningState players) {
            return players;
        }
        throw new NotSupportedOperation("No RUNNING state.");
    }
}
