package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.MicroGameState;
import com.example.boardgame.server.model.Room;

import java.util.Map;
import java.util.UUID;

public class MicroGameService {
    public static final int MICRO_GAME_DURATION_MILLIS = 10_000;
    public static final int[] MICRO_GAME_SCORE_BY_RANK = {8, 5, 3, 1};

    private final BoardGameService boardGameService;
    private final ScoreService scoreService;

    public MicroGameService(BoardGameService boardGameService, ScoreService scoreService) {
        this.boardGameService = boardGameService;
        this.scoreService = scoreService;
    }

    public MicroGameState startMicroGame(Room room, String triggerPlayerId, String type) {
        boardGameService.requirePlayer(room, triggerPlayerId);
        GameState gameState = boardGameService.requireGameState(room);
        boardGameService.requirePhase(gameState, GameState.TILE_EFFECT);

        MicroGameState microGameState = new MicroGameState(
                UUID.randomUUID().toString(),
                emptyToDefault(type, "QUICK_TAP"),
                triggerPlayerId,
                System.currentTimeMillis(),
                MICRO_GAME_DURATION_MILLIS
        );
        room.setMicroGameState(microGameState);
        gameState.setTurnPhase(GameState.MICRO_GAME);
        room.touch();
        return microGameState;
    }

    public void submitMicroGameScore(Room room, String playerId, int score) {
        boardGameService.requirePlayer(room, playerId);
        requireMicroGame(room).submitScore(playerId, score);
        room.touch();
    }

    public Map<String, Integer> finishMicroGame(Room room) {
        MicroGameState microGameState = requireMicroGame(room);
        microGameState.setStatus(MicroGameState.FINISHED);
        Map<String, Integer> rewards = scoreService.rankScores(
                microGameState.getScoresByPlayerId(),
                MICRO_GAME_SCORE_BY_RANK
        );
        scoreService.applyRewards(room, rewards);
        boardGameService.requireGameState(room).advanceTurn();
        room.touch();
        return rewards;
    }

    private MicroGameState requireMicroGame(Room room) {
        MicroGameState microGameState = room.getMicroGameState();
        if (microGameState == null) {
            throw new IllegalStateException("Micro game has not started");
        }
        return microGameState;
    }

    private String emptyToDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
