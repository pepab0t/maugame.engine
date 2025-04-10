package dev.cerios.mauengine.game;

import dev.cerios.mauengine.card.*;
import dev.cerios.mauengine.entity.GameState;
import dev.cerios.mauengine.exception.GameException;
import dev.cerios.mauengine.exception.PlayerMoveException;
import dev.cerios.mauengine.exception.PlayerNotActiveException;
import dev.cerios.mauengine.game.action.*;
import dev.cerios.mauengine.game.move.DrawMove;
import dev.cerios.mauengine.game.move.PassMove;
import dev.cerios.mauengine.game.effect.DrawEffect;
import dev.cerios.mauengine.game.effect.GameEffect;
import dev.cerios.mauengine.game.effect.SkipEffect;
import dev.cerios.mauengine.game.move.PlayCardMove;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.cerios.mauengine.game.GameCore.Stage.*;

public class GameCoreImpl implements GameCore {
    private static final int MAX_PLAYERS = 5;
    private static final int MIN_PLAYERS = 2;

    private final Random random = new Random();
    private final CardManager cardManager;
    private final List<String> players = new ArrayList<>(5);
    private final Map<String, List<Card>> playerHands = new HashMap<>();
    private final List<String> playerRank = new ArrayList<>(5);
    private final CardComparer cardComparer;

    private volatile Stage stage;
    private final AtomicInteger currentPlayerIndex = new AtomicInteger();
    private GameEffect gameEffect = null;

    public GameCoreImpl(CardComparer cardComparer) {
        this.cardManager = CardManager.create();
        this.stage = LOBBY;
        this.cardComparer = cardComparer;
    }

    @Override
    public String currentPlayer() {
        return players.get(currentPlayerIndex.get() % players.size());
    }

    @Override
    public Action registerPlayer(String player) throws GameException {
        if (stage != LOBBY) {
            throw new GameException("The game has already started.");
        }
        if (players.size() >= MAX_PLAYERS) {
            throw new GameException("The game has exceeded the maximum number of players.");
        }

        players.add(player);
        playerHands.put(player, new ArrayList<>());
        return new RegisterAction(player);
    }

    public  List<Action> performPlayCard(final String playerId, Card card) throws  PlayerMoveException {
        return performPlayCard(playerId, card, null);
    }

    public List<Action> performPlayCard(final String playerId, Card card, Color nextColor) throws PlayerMoveException {
        validatePlayer(playerId);

        List<Card> playerHand = playerHands.get(playerId);
        final int cardIndex = playerHand.indexOf(card);
        if (cardIndex == -1) {
            throw new PlayerMoveException("Player does not have in hand: " + card);
        }

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
            cardManager.putCard(card);
            actions.add(new PlayCardAction(playerId, card));
        } else {
            switch (gameEffect) {
                case DrawEffect(int count) -> {
                    if (card.type() != CardType.SEVEN)
                        throw new PlayerMoveException("illegal card");
                    gameEffect = new DrawEffect(count + 2);
                    actions.add(new PlayCardAction(playerId, card));
                }
                case SkipEffect ignore -> {
                    if (card.type() != CardType.ACE)
                        throw new PlayerMoveException("illegal card");
                    actions.add(new PlayCardAction(playerId, card));
                }
            }
        }
        playerHand.remove(cardIndex);
        if (playerHand.isEmpty()) {
            playerHands.remove(playerId);
            playerRank.add(playerId);
            actions.add(new WinAction(playerId));

            if (playerHands.size() == 1) {
                String losingPlayer = playerHands.keySet().iterator().next();
                playerRank.add(losingPlayer);
                actions.add(new LoseAction(losingPlayer));
                actions.add(new PlayerRankAction(playerRank));
                actions.add(new EndAction());
                stage = FINISH;
            }
        }

        actions.add(shiftPlayer());
        return actions;
    }

    @Override
    public List<Action> performDraw(final String playerId, int cardCount) throws PlayerMoveException {
        validatePlayer(playerId);

        if (cardCount != 1) {
            throw new PlayerMoveException("illegal card draw count: " + cardCount);
        }

        if (gameEffect != null) {
            throw new PlayerMoveException("illegal move");
        }
        var drawnCard = cardManager.draw();
        playerHands.get(playerId).add(drawnCard);
        return List.of(
                new DrawAction(playerId, List.of(drawnCard)),
                shiftPlayer()
        );
    }

    @Override
    public List<Action> performPass(final String playerId) throws PlayerMoveException {
        validatePlayer(playerId);

        if (gameEffect == null) {
            throw new PlayerMoveException("illegal move");
        }

        List<Action> actions = new LinkedList<>();

        switch (gameEffect) {
            case DrawEffect(int count) -> {
                var drawnCards = cardManager.draw(count);
                playerHands.get(playerId).addAll(drawnCards);
                actions.add(new DrawAction(playerId, drawnCards));
            }
            case SkipEffect ignore -> actions.add(new PassAction(playerId));
        }

        gameEffect = null;

        actions.add(shiftPlayer());
        return actions;
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public GameState getCurrentState() {
        return new GameState(
                players,
                playerRank,
                playerHands,
                cardManager.peekPile(),
                cardManager.deckSize(),
                stage,
                currentPlayer(),
                gameEffect
        );
    }

    @Override
    public List<Action> start() throws GameException {
        if (stage != LOBBY) {
            throw new GameException("The game has already started.");
        }
        if (players.size() < MIN_PLAYERS) {
            throw new GameException("The game needs at least " + MIN_PLAYERS + " players to start.");
        }

        for (String player : players) {
            var drawnCards = cardManager.draw(4);
            playerHands.get(player).addAll(drawnCards);
        }


        stage = RUNNING;
        currentPlayerIndex.set(random.nextInt(players.size()));
        List<Action> actions = new LinkedList<>();

        actions.add(new StartPileAction(cardManager.startPile()));

        actions.add(new PlayerShiftAction(currentPlayer()));
        for (Map.Entry<String, List<Card>> entry : playerHands.entrySet()) {
            actions.add(new DrawAction(entry.getKey(), entry.getValue()));
        }
        actions.add(new StartAction());
        return actions;
    }

    private void validatePlayer(String playerId) throws PlayerMoveException {
        if (stage != RUNNING) {
            throw new PlayerMoveException("The game not running.");
        }
        if (!playerId.equals(currentPlayer())) {
            throw new PlayerNotActiveException();
        }
    }

    private PlayerShiftAction shiftPlayer() {
        String nextPlayerId = players.get(currentPlayerIndex.incrementAndGet() % players.size());
        return new PlayerShiftAction(nextPlayerId);
    }
}
