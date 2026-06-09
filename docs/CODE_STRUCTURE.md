# Code Structure

This project is a Java Android client plus a Java WebSocket server. Firebase is used for Auth only; live room and game state are not stored in Firebase Realtime Database.

## Modules

```text
app/             Android UI, Firebase sign-in, WebSocket client
shared/          Socket protocol DTOs and JSON mapping
socket-server/   Authoritative lobby, room, and board-game server
```

## Android Client

Main responsibilities:

- choose server URL and connect
- sign in as guest/Firebase anonymous user
- create/join/leave rooms
- render lobby, waiting room, board, dice, mini-game, and micro-game UI
- send user commands to the server
- render server snapshots as the official state

Important files:

```text
app/src/main/java/com/example/boardgame/ServerSession.java
app/src/main/java/com/example/boardgame/controller/socket/SocketRoomController.java
app/src/main/java/com/example/boardgame/socket/BoardGameSocketClient.java
app/src/main/java/com/example/boardgame/LobbyListActivity.java
app/src/main/java/com/example/boardgame/LobbyActivity.java
app/src/main/java/com/example/boardgame/BoardActivity.java
app/src/main/java/com/example/boardgame/DiceActivity.java
app/src/main/java/com/example/boardgame/AdGame1Activity.java
app/src/main/java/com/example/boardgame/AdGame2Activity.java
app/src/main/java/com/example/boardgame/AdGame3Activity.java
```

`ServerSession` is the client-side socket state hub. Activities should read snapshots from it instead of keeping their own independent room/game truth.

## Shared Protocol

Main responsibilities:

- define message names
- parse and serialize socket envelopes
- define lobby, room, player, and game snapshots
- keep protocol code independent from Android and server-only libraries

Important files:

```text
shared/src/main/java/com/example/boardgame/socket/protocol/MessageTypes.java
shared/src/main/java/com/example/boardgame/socket/protocol/SocketMessage.java
shared/src/main/java/com/example/boardgame/socket/protocol/SnapshotMessageMapper.java
shared/src/main/java/com/example/boardgame/socket/protocol/LobbySnapshot.java
shared/src/main/java/com/example/boardgame/socket/protocol/RoomSnapshot.java
shared/src/main/java/com/example/boardgame/socket/protocol/GameSnapshot.java
```

Protocol rule:

```text
Client sends command messages.
Server replies with REQUEST_OK/REQUEST_ERROR and broadcasts snapshots.
```

## Socket Server

Main responsibilities:

- accept WebSocket connections
- verify Firebase ID tokens for room entry when credentials are configured
- own room membership, host, ready state, passwords, and cleanup
- own turn order, dice, tile effects, score changes, and game phases
- broadcast `LOBBY_UPDATED`, `ROOM_UPDATED`, and `GAME_UPDATED`
- reject invalid, stale, or out-of-phase commands

Important files:

```text
socket-server/src/main/java/com/example/boardgame/server/BoardGameSocketServer.java
socket-server/src/main/java/com/example/boardgame/server/GameSocketHandler.java
socket-server/src/main/java/com/example/boardgame/server/ClientSession.java
socket-server/src/main/java/com/example/boardgame/server/ServerErrorMapper.java
socket-server/src/main/java/com/example/boardgame/server/FirebaseAdminAuthVerifier.java
socket-server/src/main/java/com/example/boardgame/server/DevAuthVerifier.java
```

Server models:

```text
socket-server/src/main/java/com/example/boardgame/server/model/Room.java
socket-server/src/main/java/com/example/boardgame/server/model/Player.java
socket-server/src/main/java/com/example/boardgame/server/model/GameState.java
socket-server/src/main/java/com/example/boardgame/server/model/MiniGameState.java
socket-server/src/main/java/com/example/boardgame/server/model/MicroGameState.java
```

Server services:

```text
socket-server/src/main/java/com/example/boardgame/server/service/RoomService.java
socket-server/src/main/java/com/example/boardgame/server/service/BoardGameService.java
socket-server/src/main/java/com/example/boardgame/server/service/MiniGameService.java
socket-server/src/main/java/com/example/boardgame/server/service/MicroGameService.java
socket-server/src/main/java/com/example/boardgame/server/service/ScoreService.java
```

## Data Ownership

Server owns:

- official room list
- room membership and host
- room password hash/salt
- ready state
- turn order and current turn
- dice result validation
- tile effects
- score updates
- mini/micro game timing and score submission rules
- final game status

Client owns:

- UI screens
- input controls
- local mini/micro game interaction
- Firebase sign-in flow
- selected server URL

The client may animate or preview locally, but final state should come from server snapshots.
