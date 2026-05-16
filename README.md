# BoardGame

Android board-game project rebuilt around Firebase Auth plus a custom WebSocket server.

Firebase is no longer used for Realtime Database gameplay state. The old RTDB controllers, services, storage classes, models, utilities, and rules file have been removed. Live room state and gameplay state now belong to the socket server.

## Current Status

This is still starter structure code, not a finished multiplayer game.

Done:

- `app`: Android debug UI with Firebase Auth helper and OkHttp WebSocket client.
- `shared`: dependency-free socket protocol DTOs shared by app/server.
- `socket-server`: LAN JVM server using Java-WebSocket for room creation, joining, matchmaking, ready state, start game, dice rolls, starter tile effects, and starter mini/micro score flows.
- Balanced server layout: `GameSocketHandler`, `RoomService`, `BoardGameService`, `MiniGameService`, `MicroGameService`, `ScoreService`, and minimal models.
- Removed old Firebase Realtime Database gameplay code and dependencies.
- Added request IDs, basic errors, room/game broadcasts, and heartbeat messages.

Still needed:

- Server-side Firebase ID token verification.
- Reconnect/resume support.
- Polished board-game rules on the server.
- Final tile effects, mini games, micro games, scoring, and winner calculation.
- WAN deployment through `wss://`.

## Modules

```text
app
```

Android client. It owns Firebase sign-in, token fetching, socket connection, and UI integration.

```text
shared
```

Plain Java shared code. It has no Android, Firebase, or server-only dependencies.

```text
socket-server
```

Plain JVM LAN server. It owns authoritative live room/game state.

## LAN Socket Test

Start the server:

```bash
./gradlew :socket-server:run
```

Connect Android to:

```text
ws://YOUR_LAN_IP:8080/game
```

Android emulator host shortcut:

```text
ws://10.0.2.2:8080/game
```

## Firebase Config

Firebase is currently used for Auth only.

Put the real config here:

```text
app/google-services.json
```

Example file:

```text
app/google-services.json.example
```

## Docs

- [Code Structure](docs/CODE_STRUCTURE.md)
- [Code Walkthrough](docs/CODE_WALKTHROUGH.md)
- [Socket Architecture](docs/SOCKET_ARCHITECTURE.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Team Assignment](docs/TEAM_ASSIGNMENT.md)
