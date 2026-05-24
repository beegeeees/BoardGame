package com.example.adchaosdemo.score

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardScoreControllerTest {
    @Test
    fun controllerFlowUpdatesScoreAndRound() {
        val controller = BoardScoreController(
            players = listOf(
                "p1" to "A",
                "p2" to "B",
                "p3" to "C",
                "p4" to "D"
            )
        )

        controller.onTileLanded("p1", BoardTileType.SCORE_PLUS)
        controller.onAdResult("p2", success = false)
        controller.onMiniGameFinished(listOf("p3", "p1", "p2", "p4"))
        val snapshot = controller.nextRound()

        assertEquals(2, snapshot.round)
        val top = snapshot.ranking.first()
        assertEquals("p3", top.playerId)
        assertEquals(10, top.score)
    }
}

