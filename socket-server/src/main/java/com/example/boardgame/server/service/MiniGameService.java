package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.MiniGameState;
import com.example.boardgame.server.model.Room;

import java.util.Map;
import java.util.UUID;

public class MiniGameService {
    public static final int MINI_GAME_DURATION_MILLIS = 45_000;
    public static final int[] MINI_GAME_SCORE_BY_RANK = {30, 20, 10, 5};

    private final BoardGameService boardGameService;
    private final ScoreService scoreService;

    public MiniGameService(BoardGameService boardGameService, ScoreService scoreService) {
        this.boardGameService = boardGameService;
        this.scoreService = scoreService;
    }

    public MiniGameState startMiniGame(Room room, String type) {
        GameState gameState = boardGameService.requireGameState(room);
        boardGameService.requirePhase(gameState, GameState.ROUND_END);

        MiniGameState miniGameState = new MiniGameState(
                UUID.randomUUID().toString(),
                emptyToDefault(type, "COLOR_GUESSING"),
                System.currentTimeMillis(),
                MINI_GAME_DURATION_MILLIS
        );
        room.setMiniGameState(miniGameState);
        gameState.setTurnPhase(GameState.MINI_GAME);
        return miniGameState;
    }

    public void submitMiniGameScore(Room room, String playerId, int score) {
        boardGameService.requirePlayer(room, playerId);
        requireMiniGame(room).submitScore(playerId, score);
        room.touch();
    }

    public Map<String, Integer> finishMiniGame(Room room) {
        MiniGameState miniGameState = requireMiniGame(room);
        miniGameState.setStatus(MiniGameState.FINISHED);
        Map<String, Integer> rewards = scoreService.rankScores(
                miniGameState.getScoresByPlayerId(),
                MINI_GAME_SCORE_BY_RANK
        );
        scoreService.applyRewards(room, rewards);

        GameState gameState = boardGameService.requireGameState(room);
        gameState.advanceRound();
        if (GameState.FINISHED.equals(gameState.getTurnPhase())) {
            room.setStatus(Room.FINISHED);
        }
        room.touch();
        return rewards;
    }

    private MiniGameState requireMiniGame(Room room) {
        MiniGameState miniGameState = room.getMiniGameState();
        if (miniGameState == null) {
            throw new IllegalStateException("Mini game has not started");
        }
        return miniGameState;
    }

    private String emptyToDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
