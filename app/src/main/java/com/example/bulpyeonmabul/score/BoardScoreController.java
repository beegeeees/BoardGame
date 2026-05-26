package com.example.bulpyeonmabul.score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BoardScoreController {
    private final ScoreEngine engine;
    private ScoreBoardState state;

    public BoardScoreController(List<ScorePlayerSeed> players) {
        this(players, new ScoreEngine());
    }

    public BoardScoreController(List<ScorePlayerSeed> players, ScoreEngine engine) {
        this.engine = engine;
        state = engine.createInitialState(players);
    }

    public ScoreSnapshot getSnapshot() {
        return makeSnapshot(Collections.emptyList());
    }

    public ScoreSnapshot onMoveCompleted(String playerId, int movedSpaces) {
        ScoreApplyResult result = engine.applyMoveScore(state, playerId, movedSpaces);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public ScoreSnapshot onTileLanded(String playerId, BoardTileType tileType) {
        ScoreApplyResult result = engine.applyTileLanded(state, playerId, tileType);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public ScoreSnapshot onAdResult(String playerId, boolean success) {
        ScoreApplyResult result = engine.applyAdResolved(state, playerId, success);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public ScoreSnapshot onMiniGameFinished(List<String> rankingPlayerIds) {
        ScoreApplyResult result = engine.applyMiniGameResult(state, rankingPlayerIds);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public ScoreSnapshot onChanceOrCardScoreChanged(String playerId, int delta, String reason) {
        ScoreApplyResult result = engine.applyDirectChange(state, playerId, delta, reason);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public ScoreSnapshot swapScores(String firstPlayerId, String secondPlayerId, String reason) {
        ScoreApplyResult result = engine.swapScores(state, firstPlayerId, secondPlayerId, reason);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public ScoreSnapshot multiplyAllScores(int multiplier, String reason) {
        ScoreApplyResult result = engine.multiplyAllScores(state, multiplier, reason);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public ScoreSnapshot halveAllScoresRoundUp(String reason) {
        ScoreApplyResult result = engine.halveAllScoresRoundUp(state, reason);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public ScoreSnapshot nextRound() {
        ScoreApplyResult result = engine.nextRound(state);
        state = result.getState();
        return makeSnapshot(result.getLogs());
    }

    public int getScore(String playerId) {
        for (PlayerScoreState player : state.getPlayers()) {
            if (player.getPlayerId().equals(playerId)) {
                return player.getScore();
            }
        }
        return 0;
    }

    public String getTopPlayerId() {
        List<PlayerScoreState> ranking = engine.getRanking(state);
        return ranking.isEmpty() ? null : ranking.get(0).getPlayerId();
    }

    public String getLastPlayerId() {
        List<PlayerScoreState> ranking = engine.getRanking(state);
        return ranking.isEmpty() ? null : ranking.get(ranking.size() - 1).getPlayerId();
    }

    private ScoreSnapshot makeSnapshot(List<ScoreLog> logs) {
        List<ScoreViewRow> ranking = new ArrayList<>();
        for (PlayerScoreState player : engine.getRanking(state)) {
            ranking.add(new ScoreViewRow(
                    player.getPlayerId(),
                    player.getNickname(),
                    player.getScore(),
                    player.getMiniGameWinCount()
            ));
        }
        return new ScoreSnapshot(state.getRound(), ranking, logs);
    }
}
