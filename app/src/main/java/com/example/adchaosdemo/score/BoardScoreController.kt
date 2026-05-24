package com.example.adchaosdemo.score

data class ScoreViewRow(
    val playerId: String,
    val nickname: String,
    val score: Int,
    val miniGameWinCount: Int
)

data class ScoreSnapshot(
    val round: Int,
    val ranking: List<ScoreViewRow>,
    val logs: List<ScoreLog>
)

class BoardScoreController(
    players: List<Pair<String, String>>,
    private val engine: ScoreEngine = ScoreEngine()
) {
    private var state: ScoreBoardState = engine.createInitialState(players)

    fun getSnapshot(): ScoreSnapshot {
        return ScoreSnapshot(
            round = state.round,
            ranking = engine.getRanking(state).map { it.toViewRow() },
            logs = emptyList()
        )
    }

    fun onTileLanded(playerId: String, tileType: BoardTileType): ScoreSnapshot {
        val result = engine.applyEvent(state, ScoreEvent.TileLanded(playerId, tileType))
        state = result.state
        return makeSnapshot(result.logs)
    }

    fun onAdResult(playerId: String, success: Boolean): ScoreSnapshot {
        val result = engine.applyEvent(state, ScoreEvent.AdResolved(playerId, success))
        state = result.state
        return makeSnapshot(result.logs)
    }

    fun onMiniGameFinished(rankingPlayerIds: List<String>): ScoreSnapshot {
        val result = engine.applyEvent(state, ScoreEvent.MiniGameResult(rankingPlayerIds))
        state = result.state
        return makeSnapshot(result.logs)
    }

    fun onChanceOrCardScoreChanged(playerId: String, delta: Int, reason: String): ScoreSnapshot {
        val result = engine.applyEvent(state, ScoreEvent.DirectScoreChange(playerId, delta, reason))
        state = result.state
        return makeSnapshot(result.logs)
    }

    fun nextRound(): ScoreSnapshot {
        val result = engine.applyEvent(state, ScoreEvent.NextRound)
        state = result.state
        return makeSnapshot(result.logs)
    }

    private fun makeSnapshot(logs: List<ScoreLog>): ScoreSnapshot {
        return ScoreSnapshot(
            round = state.round,
            ranking = engine.getRanking(state).map { it.toViewRow() },
            logs = logs
        )
    }

    private fun PlayerScoreState.toViewRow(): ScoreViewRow {
        return ScoreViewRow(
            playerId = playerId,
            nickname = nickname,
            score = score,
            miniGameWinCount = miniGameWinCount
        )
    }
}

