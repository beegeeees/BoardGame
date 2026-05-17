package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class BoardGameService {
    public static final int BOARD_SIZE = 16;
    public static final int FINAL_ROUND = 3;

    public static final String TILE_START = "START";
    public static final String TILE_NORMAL = "NORMAL";
    public static final String TILE_QUESTION_MARK = "QUESTION_MARK";
    public static final String TILE_CARD = "CARD";
    public static final String TILE_GAME = "GAME";

    private final SecureRandom random = new SecureRandom();

    public void startGame(Room room) {
        if (!room.canStart(RoomService.MIN_PLAYERS)) {
            throw new IllegalStateException("At least one ready player is required to start");
        }

        GameState gameState = new GameState(room.getCode(), FINAL_ROUND);
        List<String> turnOrder = new ArrayList<>();
        for (Player player : room.getPlayerList()) {
            turnOrder.add(player.getId());
        }
        gameState.setTurnOrder(turnOrder);
        room.setGameState(gameState);
        room.setStatus(Room.IN_GAME);
    }

    public int rollDice(Room room, String playerId) {
        GameState gameState = requireGameState(room);
        requireCurrentPlayer(gameState, playerId);
        requirePhase(gameState, GameState.WAITING_FOR_ROLL);

        Player player = requirePlayer(room, playerId);
        int diceRoll = 1 + random.nextInt(6);
        player.moveBy(diceRoll, BOARD_SIZE);
        gameState.setLastDiceRoll(diceRoll);
        gameState.setTurnPhase(GameState.TILE_EFFECT);
        room.touch();
        return diceRoll;
    }

    public String applyTileEffect(Room room, String playerId) {
        GameState gameState = requireGameState(room);
        requireCurrentPlayer(gameState, playerId);
        requirePhase(gameState, GameState.TILE_EFFECT);

        Player player = requirePlayer(room, playerId);
        String tileType = getTileType(player.getPosition());
        if (TILE_QUESTION_MARK.equals(tileType)) {
            player.addScore(random.nextBoolean() ? 10 : -5);
            gameState.advanceTurn();
        } else if (TILE_CARD.equals(tileType)) {
            player.addItemCard("DOUBLE_DICE");
            gameState.advanceTurn();
        } else if (!TILE_GAME.equals(tileType)) {
            gameState.advanceTurn();
        }
        room.touch();
        return tileType;
    }

    public GameState requireGameState(Room room) {
        GameState gameState = room.getGameState();
        if (gameState == null) {
            throw new IllegalStateException("Game has not started");
        }
        return gameState;
    }

    public Player requirePlayer(Room room, String playerId) {
        Player player = room.getPlayers().get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found in room");
        }
        return player;
    }

    public void requirePhase(GameState gameState, String expectedPhase) {
        if (!expectedPhase.equals(gameState.getTurnPhase())) {
            throw new IllegalStateException("Current phase is not " + expectedPhase);
        }
    }

    public String getTileType(int position) {
        if (position == 0) {
            return TILE_START;
        }
        if (position % 8 == 0) {
            return TILE_GAME;
        }
        if (position % 6 == 0) {
            return TILE_CARD;
        }
        if (position % 5 == 0) {
            return TILE_QUESTION_MARK;
        }
        return TILE_NORMAL;
    }

    private void requireCurrentPlayer(GameState gameState, String playerId) {
        if (!playerId.equals(gameState.getCurrentPlayerId())) {
            throw new IllegalStateException("It is not your turn");
        }
    }
}
