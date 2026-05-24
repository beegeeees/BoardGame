# Score Integration Note

## Added classes
- `app/src/main/java/com/example/adchaosdemo/score/ScoreEngine.kt`
- `app/src/main/java/com/example/adchaosdemo/score/ScoreModels.kt`
- `app/src/main/java/com/example/adchaosdemo/score/BoardScoreController.kt`

## Recommended integration flow
1. At game start, create controller with player list:
   - `val scoreController = BoardScoreController(players)`
2. On each board turn result:
   - tile landing: `onTileLanded(playerId, tileType)`
   - ad result: `onAdResult(playerId, success)`
   - chance/card score change: `onChanceOrCardScoreChanged(playerId, delta, reason)`
3. On mini-game end:
   - `onMiniGameFinished(rankingPlayerIds)`
4. On round end:
   - `nextRound()`
5. Use returned `ScoreSnapshot` to render score UI and logs.

## Rule defaults currently encoded
- Start tile: `+5`
- Plus tile: `+3`
- Minus tile: `-3`
- Ad success: `+5`
- Ad fail: `-5`
- Mini-game rank score: `1st +10`, `2nd +7`, `3rd +5`, `4th +3`

## Tests
- `app/src/test/java/com/example/adchaosdemo/score/ScoreEngineTest.kt`
- `app/src/test/java/com/example/adchaosdemo/score/BoardScoreControllerTest.kt`
