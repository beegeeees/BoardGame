package com.example.adchaosdemo.socket.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameSnapshot {
    private final String roomCode;
    private final int currentRound;
    private final int finalRound;
    private final String currentPlayerId;
    private final int lastDiceRoll;
    private final String turnPhase;
    private final List<String> turnOrder;

    public GameSnapshot(String roomCode, int currentRound, int finalRound, String currentPlayerId, int lastDiceRoll, String turnPhase, List<String> turnOrder) {
        this.roomCode = roomCode == null ? "" : roomCode;
        this.currentRound = currentRound;
        this.finalRound = finalRound;
        this.currentPlayerId = currentPlayerId == null ? "" : currentPlayerId;
        this.lastDiceRoll = lastDiceRoll;
        this.turnPhase = turnPhase == null ? "" : turnPhase;
        this.turnOrder = new ArrayList<>(turnOrder == null ? Collections.emptyList() : turnOrder);
    }

    public String getRoomCode() { return roomCode; }
    public int getCurrentRound() { return currentRound; }
    public int getFinalRound() { return finalRound; }
    public String getCurrentPlayerId() { return currentPlayerId; }
    public int getLastDiceRoll() { return lastDiceRoll; }
    public String getTurnPhase() { return turnPhase; }
    public List<String> getTurnOrder() { return Collections.unmodifiableList(turnOrder); }
}

