# Team Assignment

The project is split for three people by durable responsibility. The current Android debug screen is temporary test tooling, so it is shared and not owned by one member.

## Member 1: Android Client Integration

Primary goal:

- Own the production Android-side connection layer and Firebase Auth client integration.

Owned files:

```text
app/src/main/java/com/example/boardgame/auth/FirebaseAuthTokenProvider.java
app/src/main/java/com/example/boardgame/controller/socket/SocketRoomController.java
app/src/main/java/com/example/boardgame/socket/BoardGameSocketClient.java
app/src/main/java/com/example/boardgame/socket/SocketSnapshotMapper.java
app/build.gradle.kts
```

Responsibilities:

- Firebase sign-in/token handoff on Android.
- Android WebSocket connection through OkHttp.
- UI-facing command methods in `SocketRoomController`.
- Convert room/game socket broadcasts into client-friendly snapshots.
- Report connection state, server messages, and errors to UI.
- Keep Android client code from deciding authoritative game results.

Definition of done:

- Android can connect/disconnect cleanly.
- Android sends room/game intentions through typed controller methods.
- Android receives and maps room/game broadcasts reliably.
- Firebase ID token can be passed to room commands when real auth is enabled.

## Member 2: Socket Server, Protocol, And Room/Lobby

Primary goal:

- Own networking, session lifecycle, command routing, auth entry points, and room/lobby flow.

Owned files:

```text
socket-server/src/main/java/com/example/boardgame/server/BoardGameSocketServer.java
socket-server/src/main/java/com/example/boardgame/server/ClientSession.java
socket-server/src/main/java/com/example/boardgame/server/GameSocketHandler.java
socket-server/src/main/java/com/example/boardgame/server/AuthVerifier.java
socket-server/src/main/java/com/example/boardgame/server/LanAuthVerifier.java
socket-server/src/main/java/com/example/boardgame/server/model/Room.java
socket-server/src/main/java/com/example/boardgame/server/service/RoomService.java
socket-server/build.gradle.kts
shared/src/main/java/com/example/boardgame/socket/protocol/ConnectionState.java
shared/src/main/java/com/example/boardgame/socket/protocol/MessageTypes.java
shared/src/main/java/com/example/boardgame/socket/protocol/SocketEventListener.java
shared/src/main/java/com/example/boardgame/socket/protocol/SocketMessage.java
shared/src/main/java/com/example/boardgame/socket/protocol/SnapshotCodec.java
shared/src/main/java/com/example/boardgame/socket/protocol/RoomSnapshot.java
shared/build.gradle.kts
```

Responsibilities:

- Java-WebSocket server lifecycle.
- Session tracking.
- Room broadcasts.
- Command routing in `GameSocketHandler`.
- Room creation, joining, matchmaking, ready state, and host reassignment.
- Request/response format.
- Error response format.
- Heartbeat messages.
- Firebase Admin token verification later.
- Keep `shared` independent from Android, Firebase, and server-only libraries.

Definition of done:

- Server starts with `./gradlew :socket-server:run`.
- One to four players can connect to one room over LAN sockets.
- Room state stays consistent when players join, ready, disconnect, or host changes.
- Invalid commands return clear `REQUEST_ERROR` messages.
- Protocol changes are documented and kept compatible with Android.

## Member 3: Game Rules And Server State

Primary goal:

- Own authoritative game state and rules.

Owned files:

```text
socket-server/src/main/java/com/example/boardgame/server/model/Player.java
socket-server/src/main/java/com/example/boardgame/server/model/GameState.java
socket-server/src/main/java/com/example/boardgame/server/model/MiniGameState.java
socket-server/src/main/java/com/example/boardgame/server/model/MicroGameState.java
socket-server/src/main/java/com/example/boardgame/server/service/BoardGameService.java
socket-server/src/main/java/com/example/boardgame/server/service/MiniGameService.java
socket-server/src/main/java/com/example/boardgame/server/service/MicroGameService.java
socket-server/src/main/java/com/example/boardgame/server/service/ScoreService.java
shared/src/main/java/com/example/boardgame/socket/protocol/PlayerSnapshot.java
shared/src/main/java/com/example/boardgame/socket/protocol/GameSnapshot.java
```

Responsibilities:

- Turn order.
- Server-side dice rolling.
- Player movement.
- Tile effects.
- Mini game and micro game state.
- Score rewards.
- Winner calculation later.
- Snapshot data shape for room/game state.

Definition of done:

- Server rejects illegal game actions.
- Clients cannot choose dice values, tile effects, score rewards, or winners.
- Game rule services stay focused and readable; add new service boundaries only when the current split becomes difficult to maintain.

## Shared Temporary Test Tooling

These files exist to manually test the socket flow and may be edited by any member after quick coordination:

```text
app/src/main/java/com/example/boardgame/MainActivity.java
app/src/main/res/layout/activity_main.xml
app/src/main/AndroidManifest.xml
```

Guidelines:

- Keep the debug UI simple.
- Do not build final gameplay UI assumptions into the debug screen.
- Do not move game authority into the debug UI.
- Keep `android:usesCleartextTraffic="true"` while LAN testing with `ws://`.

## Shared Documentation And Coordination

Shared docs:

```text
README.md
docs/CODE_STRUCTURE.md
docs/CODE_WALKTHROUGH.md
docs/SOCKET_ARCHITECTURE.md
docs/IMPLEMENTATION_PLAN.md
docs/TEAM_ASSIGNMENT.md
```

Coordination rules:

- Firebase is for Auth, and optionally lightweight room discovery later.
- Do not use Firebase Realtime Database for live gameplay state.
- Do not trust client-provided dice rolls, random outcomes, card draws, score rewards, or winners.
- Use `MiniGame*` names only for end-of-round games.
- Use `MicroGame*` names only for tile-triggered games.
- Any change to `MessageTypes`, `SocketMessage`, or snapshot classes affects both Android and server.
- Any change to command payload fields must update `docs/SOCKET_ARCHITECTURE.md` and `docs/CODE_WALKTHROUGH.md`.
