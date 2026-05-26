package com.example.bulpyeonmabul.score;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;

public class BoardScoreControllerTest {
    @Test
    public void controllerFlowUpdatesScoreAndRound() {
        BoardScoreController controller = new BoardScoreController(Arrays.asList(
                new ScorePlayerSeed("p1", "A"),
                new ScorePlayerSeed("p2", "B"),
                new ScorePlayerSeed("p3", "C"),
                new ScorePlayerSeed("p4", "D")
        ));

        controller.onTileLanded("p1", BoardTileType.SCORE_PLUS);
        controller.onAdResult("p2", false);
        controller.onMiniGameFinished(Arrays.asList("p3", "p1", "p2", "p4"));
        ScoreSnapshot snapshot = controller.nextRound();

        assertEquals(2, snapshot.getRound());
        ScoreViewRow top = snapshot.getRanking().get(0);
        assertEquals("p3", top.getPlayerId());
        assertEquals(10, top.getScore());
    }
}
