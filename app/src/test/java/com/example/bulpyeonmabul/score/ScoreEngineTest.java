package com.example.bulpyeonmabul.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ScoreEngineTest {
    private final ScoreEngine engine = new ScoreEngine();

    @Test
    public void tileScoreApplied() {
        ScoreBoardState initial = engine.createInitialState(Arrays.asList(
                new ScorePlayerSeed("p1", "A"),
                new ScorePlayerSeed("p2", "B")
        ));

        ScoreApplyResult result = engine.applyTileLanded(initial, "p1", BoardTileType.SCORE_PLUS);
        PlayerScoreState p1 = findPlayer(result.getState().getPlayers(), "p1");

        assertEquals(3, p1.getScore());
        assertEquals(1, result.getLogs().size());
    }

    @Test
    public void miniGameRankScoreAndWinCountApplied() {
        ScoreBoardState initial = engine.createInitialState(Arrays.asList(
                new ScorePlayerSeed("p1", "A"),
                new ScorePlayerSeed("p2", "B"),
                new ScorePlayerSeed("p3", "C"),
                new ScorePlayerSeed("p4", "D")
        ));

        ScoreApplyResult result = engine.applyMiniGameResult(initial, Arrays.asList("p3", "p2", "p1", "p4"));

        assertEquals(10, findPlayer(result.getState().getPlayers(), "p3").getScore());
        assertEquals(7, findPlayer(result.getState().getPlayers(), "p2").getScore());
        assertEquals(5, findPlayer(result.getState().getPlayers(), "p1").getScore());
        assertEquals(3, findPlayer(result.getState().getPlayers(), "p4").getScore());
        assertEquals(1, findPlayer(result.getState().getPlayers(), "p3").getMiniGameWinCount());
    }

    @Test
    public void rankingSortsByScoreThenWinCount() {
        ScoreBoardState state = engine.createInitialState(Arrays.asList(
                new ScorePlayerSeed("p1", "A"),
                new ScorePlayerSeed("p2", "B")
        ));
        state = engine.applyDirectChange(state, "p1", 10, "X").getState();
        state = engine.applyDirectChange(state, "p2", 10, "X").getState();
        state = engine.applyMiniGameResult(state, Arrays.asList("p2", "p1")).getState();

        List<PlayerScoreState> ranking = engine.getRanking(state);
        assertEquals("p2", ranking.get(0).getPlayerId());
        assertTrue(ranking.get(0).getScore() >= ranking.get(1).getScore());
    }

    private PlayerScoreState findPlayer(List<PlayerScoreState> players, String playerId) {
        for (PlayerScoreState player : players) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }
        throw new AssertionError("Missing player: " + playerId);
    }
}
