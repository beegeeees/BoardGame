package com.example.boardgame.server.model;

import com.example.boardgame.socket.protocol.GameSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {
    public static final String WAITING_FOR_ROLL = "WAITING_FOR_ROLL";
    public static final String TILE_EFFECT = "TILE_EFFECT";
    public static final String MINI_GAME = "MINI_GAME";
    public static final String MICRO_GAME = "MICRO_GAME";
    public static final String ROUND_END = "ROUND_END";
    public static final String FINISHED = "FINISHED";

    private final String roomCode;
    private final int finalRound;
    private int currentRound = 1;
    private int currentPlayerIndex;
    private int lastDiceRoll;
    private String turnPhase = WAITING_FOR_ROLL;
    private final List<String> turnOrder = new ArrayList<>();

    public GameState(String roomCode, int finalRound) {
        this.roomCode = roomCode;
        this.finalRound = finalRound;
    }

    public void setTurnOrder(List<String> playerIds) {
        turnOrder.clear();
        if (playerIds != null) {
            turnOrder.addAll(playerIds);
        }
        currentPlayerIndex = 0;
    }

    public String getCurrentPlayerId() {
        if (turnOrder.isEmpty()) {
            return "";
        }
        return turnOrder.get(currentPlayerIndex);
    }

    public void advanceTurn() {
        if (turnOrder.isEmpty()) {
            return;
        }
        if (currentPlayerIndex == turnOrder.size() - 1) {
            currentPlayerIndex = 0;
            turnPhase = ROUND_END;
        } else {
            currentPlayerIndex++;
            turnPhase = WAITING_FOR_ROLL;
        }
    }

    public void advanceRound() {
        currentRound++;
        currentPlayerIndex = 0;
        turnPhase = currentRound > finalRound ? FINISHED : WAITING_FOR_ROLL;
    }

    public GameSnapshot toSnapshot() {
        return new GameSnapshot(
                roomCode,
                currentRound,
                finalRound,
                getCurrentPlayerId(),
                lastDiceRoll,
                turnPhase,
                turnOrder
        );
    }

    public String getRoomCode() {
        return roomCode;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getFinalRound() {
        return finalRound;
    }

    public int getLastDiceRoll() {
        return lastDiceRoll;
    }

    public void setLastDiceRoll(int lastDiceRoll) {
        this.lastDiceRoll = lastDiceRoll;
    }

    public String getTurnPhase() {
        return turnPhase;
    }

    public void setTurnPhase(String turnPhase) {
        this.turnPhase = turnPhase;
    }

    public List<String> getTurnOrder() {
        return Collections.unmodifiableList(turnOrder);
    }
}
