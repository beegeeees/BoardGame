# Game Overview

This is a multiplayer turn-based board game for up to four players.

## Basic Flow

```text
Title/Login
-> lobby room list
-> create or join room
-> waiting room ready state
-> board game
-> dice roll
-> tile effect
-> mini/micro game when needed
-> final score
```

## Room Rules

- Players create or join rooms through the socket server.
- Rooms can have an optional password.
- Passwords are stored server-side as hash plus salt.
- The first player is host.
- If host leaves before game start, another player becomes host.
- Empty rooms are removed.
- Players can leave before game start.
- After game start, leaving through `LEAVE_ROOM` is rejected.

## Board Rules

Current server constants:

```text
board size:   16 tiles
final round:  3 rounds
max players:  4
```

The server owns turn order and validates whose turn it is.

Each turn:

```text
WAITING_FOR_ROLL
-> player rolls dice
-> WAITING_FOR_TILE_EFFECT
-> server applies tile effect
-> next player or mini/micro game phase
```

## Tile Types

```text
0             START
1,3,5,11,15   PLUS_SCORE
7,9,13        MINUS_SCORE
2,10          CARD
4,8,12        QUESTION
6,14          AD / micro game
other         NORMAL
```

Current effects:

- `START`: gain 5 score
- `PLUS_SCORE`: gain 3 score
- `MINUS_SCORE`: lose 3 score unless a defense card is used
- `CARD`: gain one defense card if possible
- `QUESTION`: randomly gain or lose 5 score
- `AD`: current player enters a micro game
- `NORMAL`: no special reward

## Mini Games

Mini games are shared room events, not single-player tile events.

Current server behavior:

- start after the round reaches the mini-game phase
- all room players may submit scores
- duration is 240 seconds
- submission grace is 5 seconds
- scores are ranked by `ScoreService`
- rewards by rank are `30, 20, 10, 5`
- after mini-game finish, the next round starts
- after final round, the game finishes

The client owns the actual mini-game UI/gameplay. The server only validates timing, score range, phase, and final reward application.

## Micro Games

Micro games are for the current player who lands on an `AD` tile.

Current server behavior:

- only the current turn player can submit
- duration is 20 seconds
- submission grace is 3 seconds
- submitted score is added directly to that player
- after submit or timeout, turn advances

The client owns the actual micro-game UI/gameplay. The server validates player, phase, score range, and time window.

## Synchronization

The server is the source of truth. The client sends commands and renders server snapshots.

Important safeguards:

- `GameSocketHandler.handle(...)` processes commands serially.
- `GameState` enforces valid phase transitions.
- room/game snapshots include a monotonic room revision.
- state-changing commands include `expectedRevision`.
- stale commands are rejected with `STALE_STATE`.
- Android ignores stale snapshots and snapshots for rooms it has already left.

See `docs/SYNCHRONIZATION_PRESENTATION.md` for the full explanation.
