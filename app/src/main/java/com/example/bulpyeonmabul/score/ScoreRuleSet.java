package com.example.bulpyeonmabul.score;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ScoreRuleSet {
    private final Map<BoardTileType, Integer> tileScoreMap;
    private final Map<Integer, Integer> miniGameRankScoreMap;
    private final int adTileSuccessScore;
    private final int adTileFailScore;

    public ScoreRuleSet() {
        EnumMap<BoardTileType, Integer> tileScores = new EnumMap<>(BoardTileType.class);
        tileScores.put(BoardTileType.START, 5);
        tileScores.put(BoardTileType.SCORE_PLUS, 3);
        tileScores.put(BoardTileType.SCORE_MINUS, -3);
        tileScores.put(BoardTileType.CHANCE, 0);
        tileScores.put(BoardTileType.CARD, 0);
        tileScores.put(BoardTileType.AD, 0);
        tileScoreMap = Collections.unmodifiableMap(tileScores);

        Map<Integer, Integer> rankScores = new LinkedHashMap<>();
        rankScores.put(1, 10);
        rankScores.put(2, 7);
        rankScores.put(3, 5);
        rankScores.put(4, 3);
        miniGameRankScoreMap = Collections.unmodifiableMap(rankScores);

        adTileSuccessScore = 5;
        adTileFailScore = -5;
    }

    public int scoreForTile(BoardTileType tileType) {
        Integer score = tileScoreMap.get(tileType);
        return score == null ? 0 : score;
    }

    public int scoreForMiniGameRank(int rank) {
        Integer score = miniGameRankScoreMap.get(rank);
        return score == null ? 0 : score;
    }

    public int getAdTileSuccessScore() {
        return adTileSuccessScore;
    }

    public int getAdTileFailScore() {
        return adTileFailScore;
    }
}
