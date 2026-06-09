# System Architecture

This project is a multiplayer board game with an Android client, a shared JSON protocol, and a Java WebSocket server.

Firebase is used for player authentication only. The socket server owns live lobby, room, and game state.

## Basic Architecture

```text
Firebase Auth
      |
      | ID token
      v
Android Client
      |
      | WebSocket JSON messages
      v
Java Socket Server
      |
      | in-memory authoritative state
      v
Rooms / Players / Game State
```

For WAN testing:

```text
Android Client
      |
      | wss://<ngrok-domain>/game
      v
ngrok
      |
      | ws://127.0.0.1:8080/game
      v
Java Socket Server
```

## High-Level App Flow

```text
1. User opens the app.
2. App signs in through Firebase Auth.
3. App connects to the socket server.
4. Server sends current lobby room list.
5. User creates or joins a room.
6. Server verifies auth token, room password, and room capacity.
7. Server broadcasts updated lobby and room state.
8. Players mark ready.
9. Host starts the game.
10. Server creates official game state.
11. Player rolls dice.
12. Server validates turn and updates board state.
13. Server applies tile effect.
14. Client runs mini/micro game UI when needed.
15. Client submits score result.
16. Server validates and applies score.
17. Server broadcasts updated game state.
```

## Main APIs And Libraries

### Firebase Auth

Client side:

```text
Firebase Auth Android SDK
```

Used for:

- guest/anonymous login
- Firebase ID token creation

Server side:

```text
Firebase Admin SDK
```

Used for:

- verifying Firebase ID tokens
- rejecting invalid or expired tokens

If server credential env vars are missing, the server uses dev auth for local testing.

### WebSocket

Client side:

```text
OkHttp WebSocket
```

Server side:

```text
Java-WebSocket
```

Used for:

- live lobby updates
- room ready-state updates
- game-state broadcasts
- command acknowledgements

WebSocket is used because the server needs to push changes to all connected clients without polling.

### JSON Protocol

JSON mapping:

```text
Gson
```

Shared protocol module:

```text
shared/
```

The shared module defines message names and snapshot DTOs used by both Android and the server.

## Socket Message Shape

Every command uses this envelope:

```json
{
  "type": "ROLL_DICE",
  "requestId": "uuid",
  "fields": {
    "diceRoll": 4,
    "expectedRevision": 12
  }
}
```

Main client commands:

```text
CREATE_ROOM
JOIN_ROOM
LEAVE_ROOM
SET_READY
START_GAME
ROLL_DICE
APPLY_TILE_EFFECT
SUBMIT_MINI_GAME_SCORE
SUBMIT_MICRO_GAME_SCORE
```

Main server messages:

```text
SERVER_HELLO
REQUEST_OK
REQUEST_ERROR
LOBBY_UPDATED
ROOM_UPDATED
GAME_UPDATED
```

## State Ownership

Client owns:

- screen flow
- user input
- local mini/micro game UI
- selected server URL

Server owns:

- lobby room list
- room membership
- room password verification
- host and ready state
- game phase
- current turn
- dice validation
- tile effects
- score updates
- final game result

Core rule:

```text
Client sends intentions.
Server decides official state.
Client renders server snapshots.
```

## Deployment Modes

Emulator:

```text
ws://10.0.2.2:8080/game
```

LAN:

```text
ws://<computer-lan-ip>:8080/game
```

WAN through ngrok:

```text
wss://<ngrok-domain>/game
```

Server env behavior:

```text
FIREBASE_SERVICE_ACCOUNT or GOOGLE_APPLICATION_CREDENTIALS set
    -> Firebase token verification

no credential env var
    -> dev auth

BOARDGAME_NETWORK=WAN
    -> bind for ngrok

BOARDGAME_NETWORK unset
    -> bind for LAN
```
