package com.example.adchaosdemo.score

enum class BoardTileType {
    START,
    SCORE_PLUS,
    SCORE_MINUS,
    CHANCE,
    CARD,
    AD
}

data class ScoreRuleSet(
    val tileScoreMap: Map<BoardTileType, Int> = mapOf(
        BoardTileType.START to 5,
        BoardTileType.SCORE_PLUS to 3,
        BoardTileType.SCORE_MINUS to -3,
        BoardTileType.CHANCE to 0,
        BoardTileType.CARD to 0,
        BoardTileType.AD to 0
    ),
    val adTileSuccessScore: Int = 5,
    val adTileFailScore: Int = -5,
    val miniGameRankScoreMap: Map<Int, Int> = mapOf(
        1 to 10,
        2 to 7,
        3 to 5,
        4 to 3
    )
)

data class PlayerScoreState(
    val playerId: String,
    val nickname: String,
    val score: Int = 0,
    val miniGameWinCount: Int = 0
)

data class ScoreBoardState(
    val players: List<PlayerScoreState>,
    val round: Int = 1
)

sealed interface ScoreEvent {
    data class TileLanded(
        val playerId: String,
        val tileType: BoardTileType
    ) : ScoreEvent

    data class AdResolved(
        val playerId: String,
        val success: Boolean
    ) : ScoreEvent

    data class MiniGameResult(
        val rankingPlayerIds: List<String>
    ) : ScoreEvent

    data class DirectScoreChange(
        val playerId: String,
        val delta: Int,
        val reason: String = "DIRECT"
    ) : ScoreEvent

    data object NextRound : ScoreEvent
}

data class ScoreLog(
    val playerId: String,
    val delta: Int,
    val reason: String
)

data class ScoreApplyResult(
    val state: ScoreBoardState,
    val logs: List<ScoreLog>
)

