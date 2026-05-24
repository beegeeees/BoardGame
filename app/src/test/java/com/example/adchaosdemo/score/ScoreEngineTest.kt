package com.example.adchaosdemo.score

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreEngineTest {
    private val engine = ScoreEngine()

    @Test
    fun tileScoreApplied() {
        val initial = engine.createInitialState(listOf("p1" to "A", "p2" to "B"))
        val result = engine.applyEvent(
            initial,
            ScoreEvent.TileLanded(playerId = "p1", tileType = BoardTileType.SCORE_PLUS)
        )

        val p1 = result.state.players.first { it.playerId == "p1" }
        assertEquals(3, p1.score)
        assertEquals(1, result.logs.size)
    }

    @Test
    fun miniGameRankScoreAndWinCountApplied() {
        val initial = engine.createInitialState(
            listOf("p1" to "A", "p2" to "B", "p3" to "C", "p4" to "D")
        )
        val result = engine.applyEvent(
            initial,
            ScoreEvent.MiniGameResult(rankingPlayerIds = listOf("p3", "p2", "p1", "p4"))
        )

        val p3 = result.state.players.first { it.playerId == "p3" }
        val p2 = result.state.players.first { it.playerId == "p2" }
        val p1 = result.state.players.first { it.playerId == "p1" }
        val p4 = result.state.players.first { it.playerId == "p4" }

        assertEquals(10, p3.score)
        assertEquals(7, p2.score)
        assertEquals(5, p1.score)
        assertEquals(3, p4.score)
        assertEquals(1, p3.miniGameWinCount)
    }

    @Test
    fun rankingSortsByScoreThenWinCount() {
        var state = engine.createInitialState(listOf("p1" to "A", "p2" to "B"))
        state = engine.applyEvent(state, ScoreEvent.DirectScoreChange("p1", 10, "X")).state
        state = engine.applyEvent(state, ScoreEvent.DirectScoreChange("p2", 10, "X")).state
        state = engine.applyEvent(
            state,
            ScoreEvent.MiniGameResult(rankingPlayerIds = listOf("p2", "p1"))
        ).state

        val ranking = engine.getRanking(state)
        assertEquals("p2", ranking.first().playerId)
        assertTrue(ranking.first().score >= ranking[1].score)
    }
}

