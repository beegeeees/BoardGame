package com.example.adchaosdemo.score

object ScoreEngineUsageExample {
    fun sampleFlow(): ScoreBoardState {
        val engine = ScoreEngine()
        var state = engine.createInitialState(
            listOf(
                "p1" to "Player1",
                "p2" to "Player2",
                "p3" to "Player3",
                "p4" to "Player4"
            )
        )

        state = engine.applyEvent(
            state,
            ScoreEvent.TileLanded(playerId = "p1", tileType = BoardTileType.SCORE_PLUS)
        ).state
        state = engine.applyEvent(
            state,
            ScoreEvent.AdResolved(playerId = "p2", success = false)
        ).state
        state = engine.applyEvent(
            state,
            ScoreEvent.MiniGameResult(rankingPlayerIds = listOf("p2", "p1", "p4", "p3"))
        ).state

        return state
    }
}

