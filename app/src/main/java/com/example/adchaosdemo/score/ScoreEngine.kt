package com.example.adchaosdemo.score

class ScoreEngine(
    private val ruleSet: ScoreRuleSet = ScoreRuleSet()
) {
    fun createInitialState(players: List<Pair<String, String>>): ScoreBoardState {
        val states = players.map { (id, name) ->
            PlayerScoreState(playerId = id, nickname = name)
        }
        return ScoreBoardState(players = states, round = 1)
    }

    fun applyEvent(state: ScoreBoardState, event: ScoreEvent): ScoreApplyResult {
        return when (event) {
            is ScoreEvent.TileLanded -> applyTileLanded(state, event)
            is ScoreEvent.AdResolved -> applyAdResolved(state, event)
            is ScoreEvent.MiniGameResult -> applyMiniGameResult(state, event)
            is ScoreEvent.DirectScoreChange -> applyDirectChange(state, event)
            ScoreEvent.NextRound -> ScoreApplyResult(state.copy(round = state.round + 1), emptyList())
        }
    }

    fun getRanking(state: ScoreBoardState): List<PlayerScoreState> {
        return state.players.sortedWith(
            compareByDescending<PlayerScoreState> { it.score }
                .thenByDescending { it.miniGameWinCount }
                .thenBy { it.nickname }
        )
    }

    private fun applyTileLanded(state: ScoreBoardState, event: ScoreEvent.TileLanded): ScoreApplyResult {
        val delta = ruleSet.tileScoreMap[event.tileType] ?: 0
        val reason = "TILE_${event.tileType.name}"
        return applyDelta(state, event.playerId, delta, reason)
    }

    private fun applyAdResolved(state: ScoreBoardState, event: ScoreEvent.AdResolved): ScoreApplyResult {
        val delta = if (event.success) ruleSet.adTileSuccessScore else ruleSet.adTileFailScore
        val reason = if (event.success) "AD_SUCCESS" else "AD_FAIL"
        return applyDelta(state, event.playerId, delta, reason)
    }

    private fun applyMiniGameResult(state: ScoreBoardState, event: ScoreEvent.MiniGameResult): ScoreApplyResult {
        if (event.rankingPlayerIds.isEmpty()) return ScoreApplyResult(state, emptyList())
        val rankByPlayerId = event.rankingPlayerIds.withIndex().associate { (index, playerId) ->
            playerId to (index + 1)
        }
        val firstPlayerId = event.rankingPlayerIds.firstOrNull()
        val logs = mutableListOf<ScoreLog>()
        val updated = state.players.map { player ->
            val rank = rankByPlayerId[player.playerId]
            if (rank == null) return@map player
            val delta = ruleSet.miniGameRankScoreMap[rank] ?: 0
            if (delta != 0) {
                logs += ScoreLog(player.playerId, delta, "MINIGAME_RANK_$rank")
            }
            player.copy(
                score = player.score + delta,
                miniGameWinCount = player.miniGameWinCount + if (player.playerId == firstPlayerId) 1 else 0
            )
        }
        return ScoreApplyResult(state.copy(players = updated), logs)
    }

    private fun applyDirectChange(state: ScoreBoardState, event: ScoreEvent.DirectScoreChange): ScoreApplyResult {
        return applyDelta(state, event.playerId, event.delta, event.reason)
    }

    private fun applyDelta(
        state: ScoreBoardState,
        playerId: String,
        delta: Int,
        reason: String
    ): ScoreApplyResult {
        if (delta == 0) return ScoreApplyResult(state, emptyList())
        var changed = false
        val updated = state.players.map { player ->
            if (player.playerId != playerId) return@map player
            changed = true
            player.copy(score = player.score + delta)
        }
        if (!changed) return ScoreApplyResult(state, emptyList())
        return ScoreApplyResult(state.copy(players = updated), listOf(ScoreLog(playerId, delta, reason)))
    }
}

