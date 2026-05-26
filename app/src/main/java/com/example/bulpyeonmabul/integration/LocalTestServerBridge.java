package com.example.bulpyeonmabul.integration;

public final class LocalTestServerBridge implements MainGameServerBridge {
    @Override
    public void submitDiceResult(String playerId, int diceValue, int targetPosition) {
    }

    @Override
    public void submitTileResolved(String playerId, String tileType) {
    }

    @Override
    public void submitAdResult(String playerId, boolean success) {
    }

    @Override
    public void submitRoundMiniGameProgress(String playerId, int round, int progress, boolean clear) {
    }

    @Override
    public void submitRoundMiniGameFinished(String playerId, int round, int progress, boolean clear) {
    }
}
