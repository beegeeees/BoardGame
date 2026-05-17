# Code Walkthrough

This document explains the current project structure for readers who are new to networking and backend game logic. The project is split into three modules:

- `app`: Android UI and WebSocket client.
- `shared`: message and snapshot classes used by both Android and the server.
- `socket-server`: JVM WebSocket server and authoritative game logic.

The current game can be tested on LAN. It supports room creation, joining, matchmaking, ready state, starting with 1-4 players, dice rolls, starter tile effects, and starter mini/micro score flows.

## Project Configuration Files

### `settings.gradle.kts`

Purpose:

- Names the Gradle project.
- Includes the three modules: `app`, `shared`, and `socket-server`.
- Configures where Gradle downloads plugins and dependencies.

### `build.gradle.kts`

Purpose:

- Top-level Gradle file.
- Makes Android and Google Services plugins available to subprojects.

### `gradle/libs.versions.toml`

Purpose:

- Central dependency catalog.
- Keeps versions for Android Gradle Plugin, Firebase Auth, OkHttp, Java-WebSocket, JUnit, and AndroidX libraries.

### `app/build.gradle.kts`

Purpose:

- Configures the Android app module.
- Adds dependencies on `shared`, Firebase Auth, OkHttp, AppCompat, Material, Activity, and ConstraintLayout.
- Applies the Google Services plugin only when `app/google-services.json` exists.

### `shared/build.gradle.kts`

Purpose:

- Configures the plain Java protocol module.
- This module does not depend on Android or Firebase.

### `socket-server/build.gradle.kts`

Purpose:

- Configures the JVM server module.
- Adds dependencies on `shared` and Java-WebSocket.
- Defines `BoardGameSocketServer` as the runnable main class.

### `app/src/main/AndroidManifest.xml`

Purpose:

- Declares the Android app entry point.
- Adds `INTERNET` permission for WebSocket networking.
- Enables cleartext traffic so `ws://` LAN testing works without TLS.

## Networking Flow

1. The Android app opens a WebSocket connection to the server URL.
2. The app sends `SocketMessage` commands such as `CREATE_ROOM` or `ROLL_DICE`.
3. The server receives the command in `BoardGameSocketServer`.
4. `BoardGameSocketServer` passes the command to `GameSocketHandler`.
5. `GameSocketHandler` calls `RoomService` or one of the gameplay services.
6. The server sends a direct `REQUEST_OK` or `REQUEST_ERROR` response.
7. The server broadcasts `ROOM_UPDATED` or `GAME_UPDATED` to everyone in the room.
8. The Android app receives broadcasts and updates the debug UI.

## Android App Files

### `app/src/main/java/com/example/boardgame/MainActivity.java`

Purpose:

- Provides a simple debug screen for LAN testing.
- Connects UI buttons to socket commands.
- Displays connection, room, game, and log state.

Important fields:

- `socketController`: UI-facing wrapper around the WebSocket client.
- `serverUrlInput`: text field for `ws://...` URL.
- `nicknameInput`: local nickname used when creating/joining rooms.
- `roomCodeInput`: room code for joining and display.
- `scoreInput`: temporary score input for mini/micro game testing.
- `connectionStateText`: shows `CONNECTED`, `DISCONNECTED`, etc.
- `myPlayerText`: shows the player ID assigned by the server.
- `roomStateText`: renders room/player state.
- `gameStateText`: renders current game state.
- `eventLogText`: shows responses and errors.
- `myPlayerId`: stores the player ID from `REQUEST_OK`.
- `eventLog`: keeps visible debug messages.

Methods:

- `onCreate(Bundle)`: initializes the activity, applies edge-to-edge layout, binds views, socket listener, and buttons.
- `onDestroy()`: disconnects the socket when the activity is destroyed.
- `bindViews()`: finds all XML views by ID and stores references.
- `bindSocket()`: registers socket callbacks for state changes, messages, and errors.
- `bindButtons()`: connects every debug button to one socket command.
- `handleSocketMessage(SocketMessage)`: routes incoming server messages by type.
- `renderRoom(RoomSnapshot)`: formats room/player data into readable text.
- `renderGame(GameSnapshot)`: formats game turn data into readable text.
- `appendLog(String)`: appends one line to the debug log.
- `nickname()`: returns the typed nickname or `Player`.
- `score()`: parses the score field, returning `0` if invalid.
- `textOf(EditText)`: returns trimmed text from an input.
- `shortId(String)`: shortens long IDs for display.

### `app/src/main/java/com/example/boardgame/auth/FirebaseAuthTokenProvider.java`

Purpose:

- Gets the current Firebase Auth ID token from Android.
- This is prepared for real auth, but the LAN server currently accepts an empty token.

Nested types:

- `TokenCallback`: callback interface for success/failure.

Methods:

- `FirebaseAuthTokenProvider()`: uses `FirebaseAuth.getInstance()`.
- `FirebaseAuthTokenProvider(FirebaseAuth)`: allows dependency injection for testing.
- `requireIdToken(TokenCallback)`: gets the signed-in user token or returns an error if no user is signed in.
- `TokenCallback.onToken(String)`: called with a Firebase ID token.
- `TokenCallback.onError(Exception)`: called when token loading fails.

### `app/src/main/java/com/example/boardgame/controller/socket/SocketRoomController.java`

Purpose:

- Gives the Android UI simple method names instead of requiring it to build raw socket messages.
- This is the Android-side controller for room/game commands.

Fields:

- `socketClient`: lower-level WebSocket client.

Methods:

- `SocketRoomController()`: creates a default `BoardGameSocketClient`.
- `SocketRoomController(BoardGameSocketClient)`: allows passing a custom client.
- `setListener(SocketEventListener)`: receives connection/messages/errors.
- `connect(String)`: opens a WebSocket connection.
- `disconnect()`: closes the WebSocket connection.
- `createRoom(String, String)`: sends `CREATE_ROOM`.
- `joinRoom(String, String, String)`: sends `JOIN_ROOM`.
- `matchmake(String, String)`: sends `MATCHMAKE`.
- `setReady(boolean)`: sends `SET_READY`.
- `startGame()`: sends `START_GAME`.
- `rollDice()`: sends `ROLL_DICE`.
- `applyTileEffect()`: sends `APPLY_TILE_EFFECT`.
- `startMiniGame(String)`: sends `START_MINI_GAME`.
- `submitMiniGameScore(int)`: sends `SUBMIT_MINI_GAME_SCORE`.
- `finishMiniGame()`: sends `FINISH_MINI_GAME`.
- `submitMicroGameScore(int)`: sends `SUBMIT_MICRO_GAME_SCORE`.
- `finishMicroGame()`: sends `FINISH_MICRO_GAME`.
- `commandBuilder(String, String, String)`: builds commands that include nickname and Firebase token.

### `app/src/main/java/com/example/boardgame/socket/BoardGameSocketClient.java`

Purpose:

- Owns the Android WebSocket connection using OkHttp.
- Sends serialized `SocketMessage` text.
- Converts incoming text back into `SocketMessage`.

Fields:

- `HEARTBEAT_SECONDS`: interval for app-level ping.
- `NORMAL_CLOSE`: WebSocket close code for normal disconnect.
- `okHttpClient`: OkHttp networking client.
- `listener`: app callback receiver.
- `heartbeatExecutor`: background heartbeat scheduler.
- `webSocket`: active OkHttp WebSocket.
- `state`: current connection state.

Methods:

- `BoardGameSocketClient()`: creates a default OkHttp client.
- `BoardGameSocketClient(OkHttpClient)`: allows custom OkHttp setup.
- `setListener(SocketEventListener)`: registers callbacks.
- `connect(String)`: opens a WebSocket to the server URL.
- `send(SocketMessage)`: sends one command if connected.
- `disconnect()`: stops heartbeat and closes the socket.
- `getState()`: returns current connection state.
- `startHeartbeat()`: sends `APP_PING` regularly.
- `stopHeartbeat()`: stops the heartbeat thread.
- `changeState(ConnectionState)`: stores and reports connection state.
- `notifyError(Throwable)`: reports errors to the listener.

Inner class `BoardGameWebSocketListener`:

- `onOpen(WebSocket, Response)`: marks connection as connected and starts heartbeat.
- `onMessage(WebSocket, String)`: parses server text into `SocketMessage`.
- `onClosing(WebSocket, int, String)`: marks the connection as closing.
- `onClosed(WebSocket, int, String)`: clears socket and marks disconnected.
- `onFailure(WebSocket, Throwable, Response)`: reports network failure and marks disconnected.

### `app/src/main/java/com/example/boardgame/socket/SocketSnapshotMapper.java`

Purpose:

- Converts generic socket messages into typed snapshot objects for UI rendering.

Methods:

- `toRoomSnapshot(SocketMessage)`: reads a `ROOM_UPDATED` message.
- `toGameSnapshot(SocketMessage)`: reads a `GAME_UPDATED` message.

## Shared Protocol Files

### `shared/src/main/java/com/example/boardgame/socket/protocol/ConnectionState.java`

Purpose:

- Enum used by the Android client to report connection status.

Values:

- `DISCONNECTED`: no active socket.
- `CONNECTING`: socket opening is in progress.
- `CONNECTED`: socket is open.
- `CLOSING`: socket is closing.

### `shared/src/main/java/com/example/boardgame/socket/protocol/MessageTypes.java`

Purpose:

- Central list of message type strings.
- Prevents typos between Android and server.

Fields:

- Client commands: `CREATE_ROOM`, `JOIN_ROOM`, `MATCHMAKE`, `SET_READY`, `START_GAME`, `ROLL_DICE`, `APPLY_TILE_EFFECT`, mini/micro commands, `APP_PING`.
- Server responses/broadcasts: `REQUEST_OK`, `REQUEST_ERROR`, `ROOM_UPDATED`, `GAME_UPDATED`, `MINI_GAME_UPDATED`, `MICRO_GAME_UPDATED`, `SERVER_NOTICE`, `APP_PONG`.

### `shared/src/main/java/com/example/boardgame/socket/protocol/SocketEventListener.java`

Purpose:

- Callback interface used by Android UI to react to socket events.

Methods:

- `onStateChanged(ConnectionState)`: called when connection state changes.
- `onMessage(SocketMessage)`: called for incoming server messages.
- `onError(Throwable)`: called for network or parsing errors.

### `shared/src/main/java/com/example/boardgame/socket/protocol/SocketMessage.java`

Purpose:

- Represents one command, response, or broadcast.
- Encodes messages as URL-form text such as `type=CREATE_ROOM&requestId=...`.

Fields:

- `FIELD_TYPE`: key name for message type.
- `FIELD_REQUEST_ID`: key name for request ID.
- `type`: command/response name.
- `requestId`: ID used to match responses to requests.
- `fields`: extra key/value data.

Methods:

- `command(String)`: creates a command with a generated request ID.
- `builder(String)`: starts manual message building.
- `parse(String)`: converts wire text into a `SocketMessage`.
- `toWireText()`: converts a `SocketMessage` into wire text.
- `getType()`: returns message type.
- `getRequestId()`: returns request ID.
- `get(String)`: returns one field or `null`.
- `getOrDefault(String, String)`: returns a field with fallback.
- `getInt(String, int)`: returns an integer field.
- `getBoolean(String, boolean)`: returns a boolean field.
- `getFields()`: returns all extra fields.
- `encode(String)`: URL-encodes a value.
- `decode(String)`: URL-decodes a value.

Inner class `Builder`:

- `Builder(String)`: stores message type.
- `requestId(String)`: sets request ID.
- `put(String, String)`: adds text field.
- `put(String, int)`: adds integer field.
- `put(String, boolean)`: adds boolean field.
- `build()`: creates immutable `SocketMessage`.

### `shared/src/main/java/com/example/boardgame/socket/protocol/PlayerSnapshot.java`

Purpose:

- Read-only player data sent to clients.

Fields and getters:

- `id`, `getId()`: server player ID.
- `nickname`, `getNickname()`: display name.
- `score`, `getScore()`: board score.
- `position`, `getPosition()`: board tile index.
- `ready`, `isReady()`: lobby ready state.
- `host`, `isHost()`: whether player is room host.

### `shared/src/main/java/com/example/boardgame/socket/protocol/RoomSnapshot.java`

Purpose:

- Read-only room data sent to clients.

Fields and getters:

- `code`, `getCode()`: room code.
- `hostPlayerId`, `getHostPlayerId()`: current host.
- `status`, `getStatus()`: `WAITING`, `READY`, `IN_GAME`, or `FINISHED`.
- `players`, `getPlayers()`: list of `PlayerSnapshot`.

### `shared/src/main/java/com/example/boardgame/socket/protocol/GameSnapshot.java`

Purpose:

- Read-only game turn data sent to clients.

Fields and getters:

- `roomCode`, `getRoomCode()`: room this game belongs to.
- `currentRound`, `getCurrentRound()`: current round number.
- `finalRound`, `getFinalRound()`: final round number.
- `currentPlayerId`, `getCurrentPlayerId()`: whose turn it is.
- `lastDiceRoll`, `getLastDiceRoll()`: last server dice result.
- `turnPhase`, `getTurnPhase()`: current phase string.
- `turnOrder`, `getTurnOrder()`: ordered player IDs.

### `shared/src/main/java/com/example/boardgame/socket/protocol/SnapshotCodec.java`

Purpose:

- Encodes snapshot lists into a string field.
- Decodes snapshot string fields back into typed lists.

Methods:

- `encodePlayers(List<PlayerSnapshot>)`: converts players into compact text.
- `decodePlayers(String)`: restores players from compact text.
- `encodeIds(List<String>)`: converts ID lists into compact text.
- `decodeIds(String)`: restores ID lists.
- `encode(String)`: Base64 URL-encodes one value.
- `decode(String)`: Base64 URL-decodes one value.
- `parseInt(String)`: safely parses an integer field.

## Socket Server Files

### `socket-server/src/main/java/com/example/boardgame/server/BoardGameSocketServer.java`

Purpose:

- Starts the JVM WebSocket server using Java-WebSocket.
- Owns all connected sessions.
- Forwards messages to `GameSocketHandler`.
- Broadcasts messages to players in a room.

Fields:

- `DEFAULT_PORT`: port used when no CLI argument is supplied.
- `sessions`: maps WebSocket connections to `ClientSession`.
- `gameSocketHandler`: command handler and game coordinator.

Methods:

- `BoardGameSocketServer(int)`: binds the server to a port.
- `main(String[])`: starts the server process.
- `onOpen(WebSocket, ClientHandshake)`: creates a session for a new connection.
- `onMessage(WebSocket, String)`: parses text and routes commands.
- `onClose(WebSocket, int, String, boolean)`: removes session and notifies game logic.
- `onError(WebSocket, Exception)`: logs server socket errors.
- `onStart()`: logs startup and enables connection timeout.
- `sendToRoom(String, SocketMessage)`: sends one broadcast to every session in a room.

### `socket-server/src/main/java/com/example/boardgame/server/ClientSession.java`

Purpose:

- Represents one connected client.
- Stores which room/player the WebSocket belongs to.

Fields:

- `connectionId`: temporary ID for the socket connection.
- `webSocket`: Java-WebSocket connection object.
- `roomCode`: room currently joined.
- `playerId`: player ID currently bound.
- `firebaseUid`: verified Firebase UID currently bound.

Methods:

- `ClientSession(WebSocket)`: wraps one WebSocket.
- `bindPlayer(String, String, String)`: associates the connection with a room/player/UID.
- `send(SocketMessage)`: sends one message to the client.
- `sendError(SocketMessage, String, String)`: sends `REQUEST_ERROR`.
- `close()`: closes the WebSocket.
- `getConnectionId()`: returns connection ID.
- `getRoomCode()`: returns bound room code.
- `getPlayerId()`: returns bound player ID.
- `getFirebaseUid()`: returns bound Firebase UID.

### `socket-server/src/main/java/com/example/boardgame/server/GameSocketHandler.java`

Purpose:

- Converts socket commands into service calls.
- Sends success/error responses.
- Broadcasts updated room/game snapshots.

Fields:

- `socketServer`: used for broadcasting.
- `authVerifier`: validates or assigns user identity.
- `roomService`: owns room/lobby state.
- `gameService`: owns gameplay rules.

Methods:

- `GameSocketHandler(BoardGameSocketServer, AuthVerifier)`: wires dependencies.
- `handle(ClientSession, SocketMessage)`: main command entry point.
- `disconnect(ClientSession)`: removes a player when a socket closes.
- `handleCommand(ClientSession, SocketMessage)`: routes command type to a private method.
- `createRoom(ClientSession, SocketMessage)`: handles `CREATE_ROOM`.
- `joinRoom(ClientSession, SocketMessage)`: handles `JOIN_ROOM`.
- `matchmake(ClientSession, SocketMessage)`: handles `MATCHMAKE`.
- `setReady(ClientSession, SocketMessage)`: handles `SET_READY`.
- `startGame(ClientSession)`: handles `START_GAME`.
- `rollDice(ClientSession)`: handles `ROLL_DICE`.
- `applyTileEffect(ClientSession)`: handles `APPLY_TILE_EFFECT`.
- `startMiniGame(ClientSession, SocketMessage)`: handles `START_MINI_GAME`.
- `submitMiniGameScore(ClientSession, SocketMessage)`: handles `SUBMIT_MINI_GAME_SCORE`.
- `finishMiniGame(ClientSession)`: handles `FINISH_MINI_GAME`.
- `submitMicroGameScore(ClientSession, SocketMessage)`: handles `SUBMIT_MICRO_GAME_SCORE`.
- `finishMicroGame(ClientSession)`: handles `FINISH_MICRO_GAME`.
- `sendOk(ClientSession, SocketMessage, Result)`: sends `REQUEST_OK`.
- `publishRoom(Room)`: broadcasts `ROOM_UPDATED`.
- `publishGame(Room)`: broadcasts `GAME_UPDATED`.
- `requireBoundRoom(ClientSession)`: loads the room for a session.
- `requireNotInRoom(ClientSession)`: prevents joining twice on one connection.
- `verify(SocketMessage, ClientSession)`: obtains the server identity for the player.

Inner class `Result`:

- Holds the affected room, player, and whether game state should be broadcast.
- `roomOnly(Room, Player)`: result for lobby-only changes.
- `roomAndGame(Room, Player)`: result for game changes.

### `socket-server/src/main/java/com/example/boardgame/server/AuthVerifier.java`

Purpose:

- Interface for validating a user identity.
- Implementations return the trusted UID after token verification.

Methods:

- `verify(String, String)`: returns trusted user ID for a token/connection.

### `socket-server/src/main/java/com/example/boardgame/server/FirebaseAdminAuthVerifier.java`

Purpose:

- Verifies Firebase ID tokens with Firebase Admin SDK.
- Checks token revocation and returns the verified Firebase UID.
- Loads credentials from `FIREBASE_SERVICE_ACCOUNT` or `GOOGLE_APPLICATION_CREDENTIALS`.

Methods:

- `verify(String, String)`: returns the verified Firebase UID.

## Server Model Files

### `socket-server/src/main/java/com/example/boardgame/server/model/Room.java`

Purpose:

- Holds one room’s players, status, and current game states.

Fields:

- Status constants: `WAITING`, `READY`, `IN_GAME`, `FINISHED`.
- `code`: room code.
- `createdAtMillis`: creation time.
- `updatedAtMillis`: last state change time.
- `hostPlayerId`: current host.
- `status`: room lifecycle state.
- `players`: players by player ID.
- `gameState`: board-game turn state.
- `miniGameState`: current mini game.
- `microGameState`: current micro game.

Methods:

- `Room(String)`: creates a room.
- `addPlayer(Player)`: adds player and assigns host if needed.
- `removePlayer(String)`: removes player and reassigns host.
- `canStart(int)`: true when enough players are present and all are ready.
- `refreshReadyStatus(int)`: updates `WAITING`/`READY`.
- `toSnapshot()`: creates a client-safe `RoomSnapshot`.
- `touch()`: updates modified time.
- Getters/setters: expose room fields and update state.

### `socket-server/src/main/java/com/example/boardgame/server/model/Player.java`

Purpose:

- Holds server-side player state.

Fields:

- `id`: server-generated player ID.
- `firebaseUid`: auth identity.
- `nickname`: display name.
- `score`: total score.
- `position`: board tile position.
- `ready`: lobby ready flag.
- `host`: room host flag.
- `itemCards`: card inventory.

Methods:

- `Player(String, String, String)`: creates a player.
- `moveBy(int, int)`: moves around the board with wraparound.
- `addScore(int)`: changes score.
- `addItemCard(String)`: adds a card.
- `toSnapshot()`: creates client-safe `PlayerSnapshot`.
- Getters/setters: expose player state.

### `socket-server/src/main/java/com/example/boardgame/server/model/GameState.java`

Purpose:

- Holds board-game turn state.

Fields:

- Phase constants: `WAITING_FOR_ROLL`, `TILE_EFFECT`, `MINI_GAME`, `MICRO_GAME`, `ROUND_END`, `FINISHED`.
- `roomCode`: room this state belongs to.
- `finalRound`: last round.
- `currentRound`: current round.
- `currentPlayerIndex`: index inside turn order.
- `lastDiceRoll`: last dice value.
- `turnPhase`: current phase.
- `turnOrder`: ordered player IDs.

Methods:

- `GameState(String, int)`: creates game state.
- `setTurnOrder(List<String>)`: sets player turn order.
- `getCurrentPlayerId()`: returns current turn player.
- `advanceTurn()`: moves to next player or round end.
- `advanceRound()`: starts next round or finishes game.
- `toSnapshot()`: creates client-safe `GameSnapshot`.
- Getters/setters: expose game state.

### `socket-server/src/main/java/com/example/boardgame/server/model/MiniGameState.java`

Purpose:

- Holds one end-of-round mini game.

Fields:

- Status constants: `RUNNING`, `FINISHED`.
- `id`: mini game ID.
- `type`: mini game type name.
- `startedAtMillis`: start time.
- `durationMillis`: planned duration.
- `status`: running/finished.
- `scoresByPlayerId`: submitted scores.

Methods:

- `MiniGameState(String, String, long, int)`: creates mini game state.
- `submitScore(String, int)`: records one player score.
- Getters/setters: expose mini game state.

### `socket-server/src/main/java/com/example/boardgame/server/model/MicroGameState.java`

Purpose:

- Holds one tile-triggered micro game.

Fields:

- Status constants: `RUNNING`, `FINISHED`.
- `id`: micro game ID.
- `type`: micro game type name.
- `triggerPlayerId`: player who triggered it.
- `startedAtMillis`: start time.
- `durationMillis`: planned duration.
- `status`: running/finished.
- `scoresByPlayerId`: submitted scores.

Methods:

- `MicroGameState(String, String, String, long, int)`: creates micro game state.
- `submitScore(String, int)`: records one player score.
- Getters/setters: expose micro game state.

## Server Service Files

### `socket-server/src/main/java/com/example/boardgame/server/service/RoomService.java`

Purpose:

- Owns lobby and room membership logic.
- Stores rooms in memory for LAN testing.

Fields:

- `MIN_PLAYERS`: minimum players required to start.
- `MAX_PLAYERS`: maximum room size.
- `rooms`: in-memory room map.
- `random`: room code generator randomness.

Nested class `MatchResult`:

- Holds room and player returned by matchmaking.
- `getRoom()`: matched room.
- `getPlayer()`: joined/created player.

Methods:

- `createRoom(String, String)`: creates room and host player.
- `joinRoom(String, String, String)`: joins an existing room.
- `matchmake(String, String)`: joins an open room or creates one.
- `setReady(String, String, boolean)`: updates ready state.
- `disconnect(String, String)`: removes player and possibly room.
- `requireRoom(String)`: returns room or throws.
- `requirePlayer(Room, String)`: returns player or throws.
- `requireHost(Room, String)`: validates host-only commands.
- `getRooms()`: returns current rooms.
- `createPlayer(String, String)`: creates a server player.
- `createUniqueRoomCode()`: generates unused room code.

### `socket-server/src/main/java/com/example/boardgame/server/service/BoardGameService.java`

Purpose:

- Owns the main board flow.
- Handles game start, turn ownership, dice rolls, movement, and starter tile effects.

Fields:

- `BOARD_SIZE`: number of board tiles.
- `FINAL_ROUND`: number of rounds.
- Tile constants: starter tile type names.
- `random`: server-side randomness.

Methods:

- `startGame(Room)`: creates `GameState` and sets room `IN_GAME`.
- `rollDice(Room, String)`: validates turn, rolls server dice, moves player.
- `applyTileEffect(Room, String)`: applies starter tile behavior and returns the tile type.
- `requireGameState(Room)`: gets active game or throws.
- `requirePlayer(Room, String)`: validates player exists.
- `requirePhase(GameState, String)`: validates game phase.
- `getTileType(int)`: returns starter tile type for a board position.
- `requireCurrentPlayer(GameState, String)`: validates turn ownership.

### `socket-server/src/main/java/com/example/boardgame/server/service/MiniGameService.java`

Purpose:

- Owns end-of-round mini game flow.

Fields:

- `MINI_GAME_DURATION_MILLIS`: mini game duration.
- `MINI_GAME_SCORE_BY_RANK`: rewards for mini game rank.
- `boardGameService`: shared board-state validation helper.
- `scoreService`: ranking and reward helper.

Methods:

- `startMiniGame(Room, String)`: starts end-of-round mini game.
- `submitMiniGameScore(Room, String, int)`: records mini game score.
- `finishMiniGame(Room)`: ranks scores, applies rewards, advances round.
- `requireMiniGame(Room)`: gets active mini game or throws.
- `emptyToDefault(String, String)`: default helper for optional type names.

### `socket-server/src/main/java/com/example/boardgame/server/service/MicroGameService.java`

Purpose:

- Owns tile-triggered micro game flow.

Fields:

- `MICRO_GAME_DURATION_MILLIS`: micro game duration.
- `MICRO_GAME_SCORE_BY_RANK`: rewards for micro game rank.
- `boardGameService`: shared board-state validation helper.
- `scoreService`: ranking and reward helper.

Methods:

- `startMicroGame(Room, String, String)`: starts tile-triggered micro game.
- `submitMicroGameScore(Room, String, int)`: records micro game score.
- `finishMicroGame(Room)`: ranks scores, applies rewards, advances turn.
- `requireMicroGame(Room)`: gets active micro game or throws.
- `emptyToDefault(String, String)`: default helper for optional type names.

### `socket-server/src/main/java/com/example/boardgame/server/service/ScoreService.java`

Purpose:

- Owns shared score ranking and reward application.

Methods:

- `rankScores(Map<String, Integer>, int[])`: converts raw scores into rewards.
- `applyRewards(Room, Map<String, Integer>)`: adds rewards to players.

## XML Layout

### `app/src/main/res/layout/activity_main.xml`

Purpose:

- Debug UI for LAN testing.
- Uses plain Android views so the networking flow is easy to inspect.

Important views:

- `serverUrlInput`: WebSocket URL.
- `nicknameInput`: player nickname.
- `connectButton`, `disconnectButton`: socket connection controls.
- `roomCodeInput`: room code for join/display.
- Room buttons: create, join, matchmake, ready, unready, start.
- Game buttons: roll, tile effect.
- Mini/micro buttons: start/submit/finish test flows.
- State text views: connection, player, room, game, log.
