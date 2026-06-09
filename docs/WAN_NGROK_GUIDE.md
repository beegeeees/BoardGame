# Server Setup And ngrok

This guide covers local LAN testing, Firebase Admin auth, and temporary WAN testing through ngrok.

## Server Defaults

Start server on port `8080`:

```bash
./gradlew :socket-server:run
```

Start server on a different port:

```bash
./gradlew :socket-server:run --args="9090"
```

Default behavior:

```text
no credential env var      -> DEV auth
credential env var exists  -> FIREBASE auth
BOARDGAME_NETWORK unset    -> LAN bind, 0.0.0.0
BOARDGAME_NETWORK=WAN      -> WAN/ngrok bind, 127.0.0.1
```

## Firebase Auth

The Android app needs:

```text
app/google-services.json
```

The server needs a Firebase Admin service account only when testing real Firebase token verification.

Use either env var:

```bash
FIREBASE_SERVICE_ACCOUNT=/absolute/path/to/service-account.json ./gradlew :socket-server:run
```

or:

```bash
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json ./gradlew :socket-server:run
```

The JSON filename does not matter. The env var points to the file path.

If neither env var is set, the server uses `DevAuthVerifier`. That is useful for local gameplay tests, but it is not production-safe.

## LAN Testing

Start the server:

```bash
./gradlew :socket-server:run
```

Android emulator URL:

```text
ws://10.0.2.2:8080/game
```

Physical phone on same Wi-Fi:

```text
ws://YOUR_COMPUTER_LAN_IP:8080/game
```

The app has server preset buttons for emulator, LAN, and WAN.

## WAN Testing With ngrok

ngrok gives a public TLS endpoint while the Java server still runs locally.

```text
Android wss://<ngrok-domain>/game
        -> ngrok
        -> local ws://127.0.0.1:8080/game
```

Start the server for ngrok:

```bash
BOARDGAME_NETWORK=WAN ./gradlew :socket-server:run
```

With Firebase Admin auth:

```bash
FIREBASE_SERVICE_ACCOUNT=/absolute/path/to/service-account.json BOARDGAME_NETWORK=WAN ./gradlew :socket-server:run
```

Start ngrok in another terminal:

```bash
ngrok http 8080
```

If ngrok prints:

```text
https://abc123.ngrok-free.app
```

Use this in Android:

```text
wss://abc123.ngrok-free.app/game
```

Use `wss://`, not `https://`, because the Android client opens a WebSocket.

Current WAN preset in the app:

```text
wss://sandworm-ferret-bath.ngrok-free.dev/game
```

## Smoke Test

1. Start server.
2. Start ngrok if testing WAN.
3. Open app on client A and connect.
4. Create a room.
5. Open app on client B and connect to the same URL.
6. Join by room code.
7. Toggle ready for both players.
8. Host starts the game.
9. Roll dice and apply tile effects.
10. Test leaving/disconnect and confirm lobby/room UI updates.

## Expected Server Logs

Startup:

```text
BoardGame socket server listening on ws://127.0.0.1:8080/game
BoardGame network=WAN auth=FIREBASE
```

Connection:

```text
event=socket_open remote=...
event=socket_close remote=...
```

Command error:

```text
event=command_failed type=JOIN_ROOM requestId=... errorCode=ROOM_NOT_FOUND
```

## Troubleshooting

If Android cannot connect:

- check the URL ends with `/game`
- use `ws://` for LAN and `wss://` for ngrok
- keep ngrok and the local server running
- update the app URL if a free ngrok domain changes
- do not use `127.0.0.1` on a physical phone

If commands fail with `UNAUTHENTICATED`:

- check `app/google-services.json`
- check `FIREBASE_SERVICE_ACCOUNT` or `GOOGLE_APPLICATION_CREDENTIALS`
- restart without credential env vars for DEV auth gameplay testing

If two clients do not see the same room:

- confirm both clients use the same server URL
- check server logs for `command_failed`
- reconnect both clients and retry room creation/join
