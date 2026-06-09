# Multiplayer Synchronization Explanation

This document explains the synchronization problems that happen in a small multiplayer board game and how this project handles them. It is written for presentation, discussion, and code review.

## 1. What Synchronization Means

In this project, synchronization means:

```text
Every client should eventually show the same official room and game state.
```

For example, if four players are in the same room:

- all clients should see the same player list
- all clients should agree who is ready
- all clients should agree whose turn it is
- all clients should agree where each board piece is
- all clients should agree when a mini-game or micro-game starts and ends
- all clients should recover if an old message arrives late

This project is not a real-time action game. The board game is turn-based, and the mini/micro game logic mostly runs locally on the client. Because of that, we do not need complicated real-time prediction. The simpler and better model is:

```text
Client sends command -> Server validates command -> Server broadcasts official snapshot
```

The server is the source of truth.

## 2. Why Synchronization Problems Happen

Even a simple multiplayer app can go out of sync because the network and UI are asynchronous.

The Android client, WebSocket connection, Firebase token fetching, server command handling, and UI rendering do not all happen at the exact same time. They are connected by messages, and messages can arrive later than expected.

Common causes:

- a player taps a button twice quickly
- one phone has slower network latency
- a client leaves a room while an old room update is still on the way
- two clients send commands almost at the same time
- the server accepts one command and rejects another
- the app reconnects while old async work is still running
- `REQUEST_OK` arrives separately from `ROOM_UPDATED` or `GAME_UPDATED`

Without synchronization rules, the UI can show stale or impossible state.

## 3. Example Bugs Caused by Poor Synchronization

### Bug 1: Client and server disagree about room membership

Example:

```text
1. Client tries to create a room while disconnected.
2. UI later reconnects.
3. An old request or old snapshot is still processed.
4. Client thinks it belongs to an old room.
5. Server state and client UI no longer match.
```

Result:

- user may see a stale room
- user may be unable to join another room
- UI may render a room that no longer exists

### Bug 2: Stale room information appears after leave

Example:

```text
1. Player leaves room 123456.
2. Client clears local room state.
3. A late ROOM_UPDATED message for room 123456 arrives.
4. Client accepts it.
5. Client shows the old room again.
```

This is a synchronization failure because the client accepted a message that no longer matched its current session intent.

### Bug 3: UI moves backward

Example:

```text
1. Client renders room revision 20.
2. An older room snapshot revision 19 arrives late.
3. Client accepts revision 19.
4. UI moves backward.
```

The player might see a ready state disappear, a player list revert, or a turn indicator move to an old player.

### Bug 4: Wrong command acknowledgement

Old design problem:

```text
pendingCommandType = "SET_READY"
pendingCommandType = "START_GAME"
REQUEST_OK for SET_READY arrives
client thinks START_GAME succeeded
```

This happens when the client stores only one global pending command type.

Command acknowledgements are asynchronous, so each acknowledgement must be matched to the command that created it.

### Bug 5: Stale commands change new state

Example:

```text
1. Client sees game state A.
2. Another player changes the room to game state B.
3. First client sends a command based on old state A.
4. Server applies that command to state B.
```

If the server does not check the client's expected state, the command may be logically valid in the old state but wrong in the new state.

## 4. Main Design Decision: Server Is the Source of Truth

The client does not directly decide official game state.

The client sends commands:

- `CREATE_ROOM`
- `JOIN_ROOM`
- `LEAVE_ROOM`
- `SET_READY`
- `START_GAME`
- `ROLL_DICE`
- `APPLY_TILE_EFFECT`
- `SUBMIT_MINI_GAME_SCORE`
- `SUBMIT_MICRO_GAME_SCORE`

The server validates the command, updates the model, then broadcasts snapshots:

- `LOBBY_UPDATED`
- `ROOM_UPDATED`
- `GAME_UPDATED`

The Android UI renders from those snapshots.

This is important because local UI state can be wrong, but server snapshots represent the official state.

## 5. Technique 1: Serialized Server Handling

`GameSocketHandler.handle(...)` is synchronized:

```java
public synchronized void handle(ClientSession session, SocketMessage message) {
    try {
        handleCommand(session, message);
    } catch (...) {
        ...
    }
}
```

This means the server handles one command at a time.

Why this helps:

- two players cannot mutate the same room at the exact same Java instruction moment
- room/player/game state is easier to reason about
- the project avoids complicated lock management

Tradeoff:

- this is not highly scalable for a large production server
- but it is a good fit for this small turn-based board game

## 6. Technique 2: Finite-State Machine

The game has explicit phases such as:

- `WAITING_FOR_ROLL`
- `WAITING_FOR_TILE_EFFECT`
- `WAITING_FOR_MICRO_GAME`
- `WAITING_FOR_MINI_GAME`
- `MINI_GAME_RUNNING`
- `FINISHED`

This prevents commands from being accepted in the wrong order.

Example rule:

```text
ROLL_DICE is valid only when the game is waiting for a roll.
APPLY_TILE_EFFECT is valid only after a dice roll.
```

This matters because synchronization is not only about message order. It is also about rule order.

Even if a command reaches the server, the server still checks:

- is this the correct player?
- is this the correct phase?
- is this action allowed now?

## 7. Technique 3: Room Revision Numbers

The project now gives each room a monotonic revision number.

A revision is a number that only increases:

```text
revision 1 -> revision 2 -> revision 3 -> revision 4
```

In code, `Room.touch()` increments the revision:

```java
public void touch() {
    revision++;
    updatedAtMillis = System.currentTimeMillis();
}
```

Any meaningful room-owned state change calls `touch()` directly or through a setter.

Examples:

- player joins
- player leaves
- ready state changes
- host changes
- game starts
- dice roll changes game state
- score changes
- mini/micro game state changes

Important detail:

```text
The exact number of increments per command is not important.
The only important rule is that newer state never has a smaller revision.
```

This gives the client a simple way to compare snapshots.

## 8. Technique 4: Snapshot Revision Filtering

Room, game, and lobby room-list snapshots now include revision information.

Example `ROOM_UPDATED`:

```json
{
  "type": "ROOM_UPDATED",
  "fields": {
    "room": {
      "code": "123456",
      "status": "IN_GAME",
      "revision": 17,
      "players": []
    }
  }
}
```

The Android client stores the latest accepted room/game revision.

Client rule:

```text
If incoming revision is older than local revision, ignore it.
Otherwise, accept it.
```

Related code in `ServerSession`:

```java
if (incomingRoom.getRevision() < latestRoomRevision) {
    return;
}
latestRoomSnapshot = incomingRoom;
latestRoomRevision = incomingRoom.getRevision();
```

This prevents the UI from moving backward.

Equal revisions are allowed because `ROOM_UPDATED` and `GAME_UPDATED` can describe different parts of the same server update.

## 9. Technique 5: Room Identity Filtering

Revision filtering alone is not enough.

After leaving a room, the client clears local room state and resets local revision to `-1`. That means an old room snapshot could look acceptable unless we also check room identity.

The client now accepts room/game snapshots only when:

- the snapshot belongs to the current room, or
- the client is currently waiting for a create/join response

Related code:

```java
private static boolean shouldAcceptRoomSnapshot(String roomCode) {
    String normalizedRoomCode = roomCode == null ? "" : roomCode.trim();
    if (normalizedRoomCode.isEmpty()) {
        return false;
    }
    String activeRoomCode = currentRoomCode;
    if (!activeRoomCode.isEmpty()) {
        return normalizedRoomCode.equalsIgnoreCase(activeRoomCode);
    }
    return hasPendingRoomEntryCommand();
}
```

This specifically handles stale room updates after leaving or disconnecting.

## 10. Technique 6: Expected Revision in Commands

State-changing commands include the client's latest known revision.

Example:

```json
{
  "type": "ROLL_DICE",
  "requestId": "abc",
  "fields": {
    "diceRoll": 4,
    "expectedRevision": 16
  }
}
```

Server rule:

```text
If expectedRevision is present and does not equal the current room revision, reject the command.
```

Related server code:

```java
private void requireExpectedRevision(Room room, SocketMessage message) {
    long expectedRevision = message.getLong("expectedRevision", -1L);
    if (expectedRevision >= 0L && expectedRevision != room.getRevision()) {
        throw new StaleStateException(room);
    }
}
```

This prevents old client actions from being applied to newer server state.

Commands that use this check:

- `SET_READY`
- `START_GAME`
- `ROLL_DICE`
- `APPLY_TILE_EFFECT`
- `START_MINI_GAME`
- `SUBMIT_MINI_GAME_SCORE`
- `FINISH_MINI_GAME`
- `SUBMIT_MICRO_GAME_SCORE`

Commands that do not need this check:

- `CREATE_ROOM`
- `JOIN_ROOM`
- `LEAVE_ROOM`

Why `LEAVE_ROOM` does not need it:

```text
Leaving is about removing this connection's current room binding.
It does not depend on an exact room/game revision.
```

## 11. Technique 7: Stale State Recovery

When the server rejects a stale command, it does not silently ignore it.

It sends:

```text
REQUEST_ERROR errorCode=STALE_STATE
```

Then it republishes current snapshots.

Related server flow:

```java
} catch (StaleStateException e) {
    session.sendError(message, error.code(), error.details());
    publishRoom(e.getRoom());
    publishGame(e.getRoom());
    publishLobby();
}
```

This gives the client two pieces of information:

- the command failed because the client's state was old
- fresh state is available immediately

This is better than only returning an error because the UI can resync without requiring a manual refresh.

## 12. Technique 8: Request ID Command Tracking

Each command has a `requestId`.

The client now tracks pending commands like this:

```text
requestId -> commandType
```

Related client code:

```java
private static final Map<String, String> PENDING_COMMANDS = new ConcurrentHashMap<>();
```

When sending a command:

```java
rememberPending(CONTROLLER.setReady(ready, latestKnownRevision()), MessageTypes.SET_READY);
```

When receiving `REQUEST_OK` or `REQUEST_ERROR`:

```java
String resolvedCommandType = PENDING_COMMANDS.remove(message.getRequestId());
```

Why this matters:

```text
REQUEST_OK for command A should update UI flow for command A,
even if command B was sent later.
```

This avoids acknowledgement mismatch bugs.

## 13. Technique 9: Connection Generation Guard

The Android app also tracks a connection generation.

This protects async work such as token fetching.

Example problem:

```text
1. User clicks create room.
2. App starts fetching Firebase token.
3. User disconnects or changes server.
4. Old token callback finishes.
5. Without a guard, old create-room command could be sent on the wrong session.
```

The generation guard checks that the connection is still the same before sending:

```java
private static boolean isSameConnectedSession(long generation) {
    return connectionState == ConnectionState.CONNECTED && connectionGeneration == generation;
}
```

This is a small but important protection for mobile UI.

## 14. Normal Flow Example

Dice roll flow:

```text
1. Client has room revision 16.
2. Current player taps roll.
3. Client sends ROLL_DICE with expectedRevision=16.
4. Server checks:
   - player is bound to the room
   - it is that player's turn
   - game phase allows rolling
   - expectedRevision matches current room revision
5. Server updates game state.
6. Room revision increases.
7. Server sends REQUEST_OK.
8. Server broadcasts ROOM_UPDATED and GAME_UPDATED.
9. Client accepts snapshots because revision is newer.
10. UI renders the official state.
```

## 15. Stale Command Example

```text
1. Client A sees room revision 10.
2. Client B changes ready state.
3. Server room becomes revision 11.
4. Client A sends START_GAME with expectedRevision=10.
5. Server sees current revision is 11.
6. Server rejects START_GAME with STALE_STATE.
7. Server republishes current room/game/lobby snapshots.
8. Client A accepts revision 11 and refreshes UI.
```

Result:

```text
The stale command does not mutate newer state.
```

## 16. Stale Snapshot Example

```text
1. Client already rendered ROOM_UPDATED revision 20.
2. Client receives ROOM_UPDATED revision 19.
3. Client compares 19 < 20.
4. Client ignores revision 19.
```

Result:

```text
The UI does not move backward.
```

## 17. Leave Room Example

```text
1. Client is in room 123456.
2. Player taps leave.
3. Client sends LEAVE_ROOM.
4. Client immediately clears local room state.
5. A late ROOM_UPDATED for room 123456 arrives.
6. Client sees it is not currently in room 123456 and is not joining a room.
7. Client ignores the stale snapshot.
```

Result:

```text
The waiting room does not reappear after leaving.
```

## 18. What This Design Does Not Try To Do

The project intentionally does not implement heavier synchronization systems.

Deferred ideas:

- persistent game recovery after server restart
- reconnect/resume token that restores the exact same player session
- idempotency cache that remembers completed request IDs
- per-room locks instead of global synchronized command handling
- vector clocks or distributed consensus
- client prediction and rollback

Those techniques are useful in larger systems, but they would add too much complexity for this project.

## 19. Why This Design Fits This Project

The game is:

- turn-based
- room-based
- small scale
- mostly server-authoritative
- not physics-real-time
- using WebSocket snapshots

So the chosen synchronization model is:

```text
server-authoritative state
+ finite-state machine
+ serialized server command handling
+ room revision numbers
+ expectedRevision command checks
+ requestId acknowledgement tracking
+ client-side stale snapshot filtering
```

This gives the project practical robustness without making the code difficult to understand.

## 20. Short Presentation Summary

The core problem was that the client could receive or send messages based on old state.

That could cause:

- stale rooms
- UI moving backward
- wrong request acknowledgement handling
- commands being applied to a newer state than the one the user saw

The solution was to make synchronization explicit:

- the server owns official state
- each room has a monotonic revision
- snapshots carry revision
- clients ignore older snapshots
- commands include expected revision
- server rejects stale commands
- request IDs match acknowledgements to the correct command
- stale snapshots from old room bindings are ignored

In one sentence:

```text
The client renders snapshots, but the server decides truth; revisions and request IDs keep both sides from confusing old state with current state.
```
