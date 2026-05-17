# Code Structure

The project is now intentionally small:

- Android app for Firebase Auth, socket connection, and UI.
- Shared Java module for protocol code.
- JVM socket server for LAN room/game state.

The old Firebase Realtime Database gameplay path has been removed.

## Layout

```text
BoardGame/
├── app/
│   └── src/main/java/com/example/boardgame/
│       ├── auth/
│       │   └── FirebaseAuthTokenProvider.java
│       ├── controller/socket/
│       │   └── SocketRoomController.java
│       ├── socket/
│       │   └── BoardGameSocketClient.java
│       └── MainActivity.java
├── shared/
│   └── src/main/java/com/example/boardgame/socket/
│       └── protocol/
└── socket-server/
    └── src/main/java/com/example/boardgame/server/
        ├── model/
        └── service/
```

## `app`

Android client responsibilities:

- Sign in with Firebase Auth.
- Get the current Firebase ID token.
- Connect to the socket server.
- Send user intentions such as create room, join room, ready, start game, and roll dice.
- Display the latest server snapshots.

Important files:

```text
app/src/main/java/com/example/boardgame/auth/FirebaseAuthTokenProvider.java
app/src/main/java/com/example/boardgame/controller/socket/SocketRoomController.java
app/src/main/java/com/example/boardgame/socket/BoardGameSocketClient.java
```

## `shared`

Shared responsibilities:

- Define message names.
- Encode/decode socket messages.
- Provide snapshot DTOs for room/game state.
- Stay independent from Android, Firebase, and server libraries.

Important files:

```text
shared/src/main/java/com/example/boardgame/socket/protocol/MessageTypes.java
shared/src/main/java/com/example/boardgame/socket/protocol/SocketMessage.java
shared/src/main/java/com/example/boardgame/socket/protocol/SnapshotMessageMapper.java
shared/src/main/java/com/example/boardgame/socket/protocol/RoomSnapshot.java
shared/src/main/java/com/example/boardgame/socket/protocol/GameSnapshot.java
```

Rules:

- No Android imports.
- No Firebase imports.
- No server-only imports.

## `socket-server`

Server responsibilities:

- Accept WebSocket connections.
- Match players into rooms.
- Own ready state.
- Own turn order.
- Roll dice server-side.
- Broadcast room/game snapshots.
- Reject invalid commands.

Important files:

```text
socket-server/src/main/java/com/example/boardgame/server/BoardGameSocketServer.java
socket-server/src/main/java/com/example/boardgame/server/ClientSession.java
socket-server/src/main/java/com/example/boardgame/server/GameSocketHandler.java
socket-server/src/main/java/com/example/boardgame/server/AuthVerifier.java
socket-server/src/main/java/com/example/boardgame/server/FirebaseAdminAuthVerifier.java
```

Server package layout:

```text
model/
  Room.java
  Player.java
  GameState.java
  MiniGameState.java
  MicroGameState.java

service/
  RoomService.java
  BoardGameService.java
  MiniGameService.java
  MicroGameService.java
  ScoreService.java
```

This is the intended starting backend layout. `GameSocketHandler` receives commands and broadcasts snapshots. `RoomService` owns lobby and room membership. `BoardGameService` owns turn, dice, movement, and tile flow. `MiniGameService` and `MicroGameService` own their timed score flows. `ScoreService` ranks raw scores and applies rewards.

## Authority Rules

Server owns:

- Room membership.
- Matchmaking.
- Ready state.
- Turn order.
- Dice rolls.
- Random events.
- Card draws.
- Tile effects.
- Score rewards.
- Game completion.

Client owns:

- UI state.
- User input.
- Firebase sign-in.
- Displaying server snapshots.

The client sends intentions. It must not decide final gameplay results.
