package com.example.bulpyeonmabul.score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScoreEngine {
    private final ScoreRuleSet ruleSet;

    public ScoreEngine() {
        this(new ScoreRuleSet());
    }

    public ScoreEngine(ScoreRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public ScoreBoardState createInitialState(List<ScorePlayerSeed> players) {
        List<PlayerScoreState> states = new ArrayList<>();
        for (ScorePlayerSeed player : players) {
            states.add(new PlayerScoreState(player.getPlayerId(), player.getNickname()));
        }
        return new ScoreBoardState(states, 1);
    }

    public ScoreApplyResult applyTileLanded(ScoreBoardState state, String playerId, BoardTileType tileType) {
        return applyDelta(state, playerId, ruleSet.scoreForTile(tileType), "TILE_" + tileType.name());
    }

    public ScoreApplyResult applyMoveScore(ScoreBoardState state, String playerId, int movedSpaces) {
        return applyDelta(state, playerId, movedSpaces, "MOVE");
    }

    public ScoreApplyResult applyAdResolved(ScoreBoardState state, String playerId, boolean success) {
        int delta = success ? ruleSet.getAdTileSuccessScore() : ruleSet.getAdTileFailScore();
        return applyDelta(state, playerId, delta, success ? "AD_SUCCESS" : "AD_FAIL");
    }

    public ScoreApplyResult applyMiniGameResult(ScoreBoardState state, List<String> rankingPlayerIds) {
        if (rankingPlayerIds.isEmpty()) {
            return new ScoreApplyResult(state, Collections.emptyList());
        }

        Map<String, Integer> rankByPlayerId = new HashMap<>();
        for (int i = 0; i < rankingPlayerIds.size(); i++) {
            rankByPlayerId.put(rankingPlayerIds.get(i), i + 1);
        }

        String firstPlayerId = rankingPlayerIds.get(0);
        List<ScoreLog> logs = new ArrayList<>();
        List<PlayerScoreState> updated = new ArrayList<>();
        for (PlayerScoreState player : state.getPlayers()) {
            Integer rank = rankByPlayerId.get(player.getPlayerId());
            if (rank == null) {
                updated.add(player);
                continue;
            }

            int delta = ruleSet.scoreForMiniGameRank(rank);
            if (delta != 0) {
                logs.add(new ScoreLog(player.getPlayerId(), delta, "MINIGAME_RANK_" + rank));
            }
            int nextWinCount = player.getMiniGameWinCount() + (player.getPlayerId().equals(firstPlayerId) ? 1 : 0);
            updated.add(player.withScoreAndWinCount(player.getScore() + delta, nextWinCount));
        }
        return new ScoreApplyResult(state.withPlayers(updated), logs);
    }

    public ScoreApplyResult applyDirectChange(ScoreBoardState state, String playerId, int delta, String reason) {
        return applyDelta(state, playerId, delta, reason);
    }

    public ScoreApplyResult nextRound(ScoreBoardState state) {
        return new ScoreApplyResult(state.withRound(state.getRound() + 1), Collections.emptyList());
    }

    public List<PlayerScoreState> getRanking(ScoreBoardState state) {
        List<PlayerScoreState> ranking = new ArrayList<>(state.getPlayers());
        ranking.sort(
                Comparator.comparingInt(PlayerScoreState::getScore).reversed()
                        .thenComparing(Comparator.comparingInt(PlayerScoreState::getMiniGameWinCount).reversed())
                        .thenComparing(PlayerScoreState::getNickname)
        );
        return ranking;
    }

    public ScoreApplyResult swapScores(ScoreBoardState state, String firstPlayerId, String secondPlayerId, String reason) {
        if (firstPlayerId.equals(secondPlayerId)) {
            return new ScoreApplyResult(state, Collections.emptyList());
        }

        PlayerScoreState first = findPlayer(state, firstPlayerId);
        PlayerScoreState second = findPlayer(state, secondPlayerId);
        if (first == null || second == null) {
            return new ScoreApplyResult(state, Collections.emptyList());
        }

        List<PlayerScoreState> updated = new ArrayList<>();
        for (PlayerScoreState player : state.getPlayers()) {
            if (player.getPlayerId().equals(firstPlayerId)) {
                updated.add(player.withScore(second.getScore()));
            } else if (player.getPlayerId().equals(secondPlayerId)) {
                updated.add(player.withScore(first.getScore()));
            } else {
                updated.add(player);
            }
        }

        List<ScoreLog> logs = new ArrayList<>();
        logs.add(new ScoreLog(firstPlayerId, second.getScore() - first.getScore(), reason));
        logs.add(new ScoreLog(secondPlayerId, first.getScore() - second.getScore(), reason));
        return new ScoreApplyResult(state.withPlayers(updated), logs);
    }

    public ScoreApplyResult multiplyAllScores(ScoreBoardState state, int multiplier, String reason) {
        List<PlayerScoreState> updated = new ArrayList<>();
        List<ScoreLog> logs = new ArrayList<>();
        for (PlayerScoreState player : state.getPlayers()) {
            int nextScore = player.getScore() * multiplier;
            updated.add(player.withScore(nextScore));
            logs.add(new ScoreLog(player.getPlayerId(), nextScore - player.getScore(), reason));
        }
        return new ScoreApplyResult(state.withPlayers(updated), logs);
    }

    public ScoreApplyResult halveAllScoresRoundUp(ScoreBoardState state, String reason) {
        List<PlayerScoreState> updated = new ArrayList<>();
        List<ScoreLog> logs = new ArrayList<>();
        for (PlayerScoreState player : state.getPlayers()) {
            int nextScore = (int) Math.ceil(player.getScore() / 2.0);
            updated.add(player.withScore(nextScore));
            logs.add(new ScoreLog(player.getPlayerId(), nextScore - player.getScore(), reason));
        }
        return new ScoreApplyResult(state.withPlayers(updated), logs);
    }

    private ScoreApplyResult applyDelta(ScoreBoardState state, String playerId, int delta, String reason) {
        if (delta == 0) {
            return new ScoreApplyResult(state, Collections.emptyList());
        }

        boolean changed = false;
        List<PlayerScoreState> updated = new ArrayList<>();
        for (PlayerScoreState player : state.getPlayers()) {
            if (player.getPlayerId().equals(playerId)) {
                changed = true;
                updated.add(player.withScore(player.getScore() + delta));
            } else {
                updated.add(player);
            }
        }

        if (!changed) {
            return new ScoreApplyResult(state, Collections.emptyList());
        }
        return new ScoreApplyResult(state.withPlayers(updated), Collections.singletonList(new ScoreLog(playerId, delta, reason)));
    }

    private PlayerScoreState findPlayer(ScoreBoardState state, String playerId) {
        for (PlayerScoreState player : state.getPlayers()) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }
}
