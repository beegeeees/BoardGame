package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.MiniGameState;
import com.example.boardgame.server.model.Room;

import java.util.Map;
import java.util.UUID;

public class MiniGameService {
    // 임시 제한 시간 30초 (기획에 맞게 추후 수정 가능)
    public static final int MINI_GAME_DURATION_MILLIS = 30_000;
    public static final int[] MINI_GAME_SCORE_BY_RANK = {30, 20, 10, 5};

    // 상태값 상수 (안드로이드에서 보내는 state 구분용)
    public static final String STATE_CLEAR = "CLEAR";
    public static final String STATE_PROGRESS = "PROGRESS";

    private final BoardGameService boardGameService;
    private final ScoreService scoreService;

    public MiniGameService(BoardGameService boardGameService, ScoreService scoreService) {
        this.boardGameService = boardGameService;
        this.scoreService = scoreService;
    }

    public MiniGameState startMiniGame(Room room) {
        GameState gameState = boardGameService.requireGameState(room);
        boardGameService.requirePhase(gameState, GameState.MINI_GAME_PHASE);

        // 1~3라운드에 맞춰 미니게임 종류 자동 할당
        int currentRound = gameState.getCurrentRound();
        String type;
        if (currentRound == 1) type = "COLOR_CAPTCHA";
        else if (currentRound == 2) type = "PASSWORD_GEN";
        else type = "VOLUME_MAZE";

        MiniGameState miniGameState = new MiniGameState(
                UUID.randomUUID().toString(),
                type,
                System.currentTimeMillis(),
                MINI_GAME_DURATION_MILLIS
        );
        room.setMiniGameState(miniGameState);
        gameState.setTurnPhase(GameState.MINI_GAME_PHASE);
        room.touch();
        return miniGameState;
    }

    // ⚠️ [핵심] 클라이언트의 복잡한 데이터를 여기서 받아서 단일 점수로 '압축'합니다.
    public void submitMiniGameResult(Room room, String playerId, String state, int progress, long completionTime) {
        boardGameService.requirePlayer(room, playerId);
        MiniGameState miniGameState = requireMiniGame(room);

        int calcScore = 0; // 이 점수가 높을수록 1등

        // 1. 상태에 따라 환산 점수(가짜 점수) 계산
        if (STATE_CLEAR.equals(state)) {
            // 클리어 시: 100만 점에서 걸린 시간을 뺌 (빨리 깰수록 점수 높음)
            calcScore = 1000000 - (int) completionTime;
        } else if (STATE_PROGRESS.equals(state)) {
            // 타임오버 시: 클리어 못 했으니 진행도(stage)만 점수로 인정
            calcScore = progress;
        }

        // 2. 모델(MiniGameState)에는 오직 계산이 끝난 '숫자 하나'만 깔끔하게 저장!
        miniGameState.submitScore(playerId, calcScore);
        room.touch();
    }

    public Map<String, Integer> finishMiniGame(Room room) {
        MiniGameState miniGameState = requireMiniGame(room);
        miniGameState.setStatus(MiniGameState.FINISHED);

        // 모델에 이미 예쁘게 '환산된 점수'가 들어있으므로, 바로 꺼내서 1~4등을 매깁니다.
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
}
