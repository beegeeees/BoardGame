package com.example.bulpyeonmabul.integration;

public interface MainGameServerBridge {
    void submitDiceResult(String playerId, int diceValue, int targetPosition);

    void submitTileResolved(String playerId, String tileType);

    void submitAdResult(String playerId, boolean success);

    void submitRoundMiniGameProgress(String playerId, int round, int progress, boolean clear);

    void submitRoundMiniGameFinished(String playerId, int round, int progress, boolean clear);
}
