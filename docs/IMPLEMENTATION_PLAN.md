# Implementation Plan

This document is a project status checklist. Milestones 1-3 are implemented in the current LAN scaffold, but multiplayer still needs manual emulator/phone verification.

## Current State

Done:

- Removed Firebase Realtime Database gameplay code and dependencies.
- Kept Firebase Auth on Android.
- Added `shared` protocol module.
- Added `socket-server` JVM module.
- Added real WebSocket libraries:
  - Android: OkHttp WebSocket.
  - Server: Java-WebSocket.
- Added middleground server structure:
  - `GameSocketHandler`
  - `RoomService`
  - `BoardGameService`
  - `MiniGameService`
  - `MicroGameService`
  - `ScoreService`
  - `Room`, `Player`, `GameState`, `MiniGameState`, `MicroGameState`
- Added starter commands for room and gameplay flow.
- Implemented the socket server entry point and OkHttp Android socket client.
- Implemented create, join, matchmaking, ready state, start game, dice roll, tile effect, and starter mini/micro score commands.
- Implemented room and game snapshot broadcasts.
- Implemented server Firebase ID token verification for room entry commands.
- Prevented one verified UID from being seated in multiple active rooms.

Not proven yet:

- Multiplayer behavior has not been tested on emulator/phone.
- WAN behavior has not been implemented or tested.
- Production Firebase token verification has not been manually tested with real service account credentials.

## Milestone 1: Make It Run

Goal:

```text
server starts -> Android connects -> one room can be created
```

Status: implemented, pending local verification on the target machine.

Tasks:

- [x] Add a runnable JVM socket server.
- [x] Add Android WebSocket client wiring.
- [x] Add shared socket protocol module.
- [x] Run `./gradlew :socket-server:run`; Gradle compiles `:shared` first because `:socket-server` depends on it.
- [ ] Build/run the Android app from Android Studio; Gradle compiles `:shared` first because `:app` depends on it.
- [ ] Connect to `ws://10.0.2.2:8080/game` from emulator.
- [ ] Optionally run a full command-line compile check with `./gradlew :shared:compileJava :socket-server:compileJava :app:compileDebugJavaWithJavac`.

## Milestone 2: Basic Room Flow

Goal:

```text
one to four clients -> one room -> ready state syncs
```

Status: implemented in code, pending multi-client manual test.

Tasks:

- [x] Create room.
- [x] Join by room code.
- [x] Matchmake.
- [x] Ready/unready.
- [x] Broadcast room snapshot with player list.
- [x] Send request errors through `REQUEST_ERROR`.
- [ ] Test two emulator/device clients in the same room.
- [ ] Test room-full and invalid-room error paths from the UI.

## Milestone 3: Basic Turn Flow

Goal:

```text
host starts -> current player rolls -> server advances state
```

Status: implemented in code, pending end-to-end gameplay test.

Tasks:

- [x] Start game.
- [x] Broadcast current player.
- [x] Roll dice on the server.
- [x] Move player position on the server.
- [x] Apply starter tile effect.
- [x] Trigger starter micro-game flow from a game tile.
- [x] Broadcast updated game state.
- [ ] Test turn ownership validation from multiple clients.
- [ ] Test a full round with two or more players.

## Milestone 4: Auth On Server

Goal:

```text
server trusts Firebase UID, not client text
```

Status: implemented in code, pending manual Firebase credential and device verification.

Tasks:

- [x] Add Firebase Admin SDK to `socket-server`.
- [x] Load server credentials from `FIREBASE_SERVICE_ACCOUNT` or `GOOGLE_APPLICATION_CREDENTIALS`.
- [x] Verify Firebase ID tokens before `CREATE_ROOM`, `JOIN_ROOM`, and `MATCHMAKE`.
- [x] Reject missing, invalid, expired, or revoked tokens with `UNAUTHENTICATED`.
- [x] Store the verified Firebase UID in `Player` and `ClientSession`.
- [x] Reject a UID that is already seated in any active room.
- [x] Add focused tests for valid token, invalid token, missing token, and duplicate UID joins.
- [ ] Run with a real Firebase service account and signed-in Android client.

## Milestone 5: Real Game Rules

Goal:

```text
server owns the actual board game
```

Tasks:

- Define final board size, tile IDs, and tile types.
- Replace starter tile behavior with final tile effect rules.
- Keep dice rolls, random outcomes, rewards, and winner decisions server-owned.
- Define final card/effect rules if cards are part of the game.
- Define allowed turn phases and validate each command against the current phase.
- Add dedicated mini-game and micro-game snapshots if clients need timers, selected game type, submitted-player counts, or result summaries.
- Decide final mini-game types and required payloads.
- Decide final micro-game types and required payloads.
- Add timeout/missing-submission rules for mini and micro games.
- Finalize score rewards, tie handling, end-of-game conditions, and winner calculation.
- Add a match-complete broadcast or final `GAME_UPDATED` fields for winner/result display.
- Add unit tests for room rules, turn rules, tile effects, score ranking, and winner calculation.
- Add protocol tests for snapshot encode/decode and request/response compatibility.

Keep the current service split unless a feature clearly needs a new boundary. Avoid adding more services before the existing ones become hard to maintain.

## Milestone 6: Reliability And WAN

Tasks:

- Add reconnect/resume by verified Firebase UID, room code, and a server-issued resume token.
- Reserve a disconnected player's seat for a short grace period before removing them.
- Add heartbeat timeout policy on top of `APP_PING` / `APP_PONG`.
- Track room `createdAt` and `lastActivityAt`.
- Add scheduled cleanup for abandoned rooms, stale matchmaking rooms, and inactive games.
- Replace raw exception details in `REQUEST_ERROR` with stable protocol error codes such as `ROOM_NOT_FOUND`, `ROOM_FULL`, `NOT_HOST`, `NOT_YOUR_TURN`, and `INVALID_STATE`.
- Add protocol versioning or a handshake field before changing message shapes.
- Decide whether to keep form-encoded messages or move to JSON before payloads grow further.
- Add structured logs for room creation, joins, disconnects, command failures, and game transitions.
- Add metrics for active rooms, connected players, command latency, and error rates.
- Decide whether match history or reconnect-safe game state needs persistence.
- Deploy behind `wss://`.
- Run WAN smoke tests through ngrok or the chosen host.
- Add rate limits and origin/network controls appropriate for the deployment.
