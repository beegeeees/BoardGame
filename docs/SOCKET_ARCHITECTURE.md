# Socket Architecture

Target shape:

```text
Firebase Auth        Android client                  Socket server
     |                    |                               |
     | sign in            |                               |
     |------------------->|                               |
     | ID token           |                               |
     |<-------------------|                               |
                          | ws://LAN_IP:8080/game         |
                          |------------------------------>|
                          | commands                      |
                          |<----------------------------->|
                          | room/game broadcasts          |
```

Firebase Auth identifies the user. The socket server owns live lobby, room, and gameplay state.

For the current LAN scaffold, server token verification is not implemented yet. `LanAuthVerifier` assigns a temporary LAN identity. Replace it with Firebase Admin SDK verification before WAN testing.

## Current Protocol

Messages are small form-encoded text fields. This keeps the scaffold dependency-free and easy to inspect. JSON is a good next step once messages become larger.

Transport libraries:

- Android client uses OkHttp WebSocket.
- JVM server uses Java-WebSocket.

Every request contains:

```text
type
requestId
```

Client commands:

```text
CREATE_ROOM   nickname firebaseIdToken
JOIN_ROOM     roomCode nickname firebaseIdToken
MATCHMAKE     nickname firebaseIdToken
SET_READY     ready
START_GAME
ROLL_DICE
APPLY_TILE_EFFECT
START_MINI_GAME          miniGameType
SUBMIT_MINI_GAME_SCORE   score
FINISH_MINI_GAME
SUBMIT_MICRO_GAME_SCORE  score
FINISH_MICRO_GAME
APP_PING
```

Server messages:

```text
REQUEST_OK      requestId roomCode playerId status
REQUEST_ERROR   requestId errorCode details
ROOM_UPDATED    roomCode hostPlayerId status players
GAME_UPDATED    roomCode currentRound finalRound currentPlayerId lastDiceRoll turnPhase turnOrder
APP_PONG
```

`REQUEST_OK` and `REQUEST_ERROR` answer a specific command. `ROOM_UPDATED` and `GAME_UPDATED` are broadcasts and can arrive at any time.

## Current Server Behavior

Implemented:

- WebSocket upgrade.
- One `ClientSession` per connection.
- Create room.
- Join room by code.
- Matchmake into an open room or create a new room.
- Ready/unready.
- Start game when at least one player is ready.
- Roll dice for the current player.
- Move player position.
- Apply starter tile effects.
- Start, submit, and finish mini game / micro game flows through dedicated gameplay services.
- Advance turn.
- Broadcast room and game snapshots.
- Remove players on disconnect.
- Reassign host when the host disconnects.

Missing:

- Firebase Admin SDK token verification.
- Reconnect/resume.
- Room timeout cleanup.
- Polished tile effects, mini games, micro games, scoring, and winner calculation.
- TLS/WAN deployment.

## LAN Testing

Start the server:

```bash
./gradlew :socket-server:run
```

Connect Android to:

```text
ws://YOUR_LAN_IP:8080/game
```

For emulator-to-host testing:

```text
ws://10.0.2.2:8080/game
```

## Later Firebase Lobby Option

If Firebase is later used for a lightweight room discovery lobby, keep it separate from gameplay. It should only advertise room/server connection information, such as:

```text
roomCode
serverUrl
playerCount
status
createdAt
```

Gameplay state should still remain on the socket server.

## WAN Requirements

Before real WAN multiplayer:

- Verify Firebase ID tokens on the server.
- Use `wss://`.
- Add reconnect and session resume.
- Add heartbeat timeout detection.
- Add room cleanup after inactivity.
- Add stable protocol errors.
- Add logs and metrics.
- Decide whether match history needs persistence.
