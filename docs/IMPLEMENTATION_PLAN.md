# Implementation Plan

This document is a project status checklist, not a promise that every section is implemented. The code is currently a socket architecture scaffold.

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

Not proven yet:

- Multiplayer behavior has not been tested on emulator/phone.

## Milestone 1: Make It Run

Goal:

```text
server starts -> Android connects -> one room can be created
```

Tasks:

- Install a full JDK with `javac`.
- Run `./gradlew :shared:compileJava :socket-server:compileJava :app:compileDebugJavaWithJavac`.
- Run `./gradlew :socket-server:run`.
- Connect to `ws://10.0.2.2:8080/game` from emulator.

## Milestone 2: Basic Room Flow

Goal:

```text
one to four clients -> one room -> ready state syncs
```

Tasks:

- Create room.
- Join by room code.
- Matchmake.
- Ready/unready.
- Show player list.
- Show request errors.

## Milestone 3: Basic Turn Flow

Goal:

```text
host starts -> current player rolls -> server advances state
```

Tasks:

- Start game.
- Show current player.
- Roll dice.
- Apply starter tile effect.
- Broadcast updated game state.

## Milestone 4: Auth On Server

Goal:

```text
server trusts Firebase UID, not client text
```

Tasks:

- Add Firebase Admin SDK to `socket-server`.
- Replace `LanAuthVerifier`.
- Verify ID token before create/join/matchmake.
- Store UID in server player/session.
- Reject invalid tokens.

## Milestone 5: Real Game Rules

Goal:

```text
server owns the actual board game
```

Tasks:

- Polish tile definitions.
- Decide final card effects.
- Finish mini game flow.
- Finish micro game flow.
- Finalize scoring and winner calculation.
- Add tests.

Keep the current service split unless a feature clearly needs a new boundary. Avoid adding more services before the existing ones become hard to maintain.

## Milestone 6: Reliability And WAN

Tasks:

- Reconnect/resume by Firebase UID and room code.
- Heartbeat timeout detection.
- Room cleanup.
- Protocol versioning.
- `wss://` deployment.
- ngrok test.
- Monitoring/logging.
