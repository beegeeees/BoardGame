package com.example.boardgame;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.boardgame.auth.FirebaseAuthTokenProvider;
import com.example.boardgame.controller.socket.SocketRoomController;
import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.LobbySnapshot;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.RoomSnapshot;
import com.example.boardgame.socket.protocol.SnapshotMessageMapper;
import com.example.boardgame.socket.protocol.SocketEventListener;
import com.example.boardgame.socket.protocol.SocketMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public final class ServerSession {
    public interface Listener {
        default void onConnectionStateChanged(ConnectionState state) {
        }

        default void onServerHello(SocketMessage message) {
        }

        default void onLobbyUpdated(LobbySnapshot lobby) {
        }

        default void onRoomUpdated(RoomSnapshot room) {
        }

        default void onGameUpdated(GameSnapshot game) {
        }

        default void onRequestOk(String commandType, String roomCode, String playerId) {
        }

        default void onServerError(String errorCode, String details) {
        }
    }

    public static final String DEFAULT_LAN_URL = "ws://10.0.2.2:8080/game";
    public static final String DEFAULT_WAN_URL = "wss://sandworm-ferret-bath.ngrok-free.dev/game";

    private static final String PREFS_NAME = "boardgame_server_session";
    private static final String KEY_SERVER_URL = "key_server_url";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<Listener> LISTENERS = new CopyOnWriteArraySet<>();
    private static final SocketRoomController CONTROLLER = new SocketRoomController();

    private static volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private static volatile LobbySnapshot latestLobbySnapshot = new LobbySnapshot(null);
    private static volatile RoomSnapshot latestRoomSnapshot;
    private static volatile GameSnapshot latestGameSnapshot;
    private static volatile String currentRoomCode = "";
    private static volatile String currentPlayerId = "";
    private static final Map<String, String> PENDING_COMMANDS = new ConcurrentHashMap<>();
    private static volatile long latestRoomRevision = -1L;
    private static volatile long latestGameRevision = -1L;
    private static volatile long connectionGeneration = 0L;

    static {
        CONTROLLER.setListener(new SocketEventListener() {
            @Override
            public void onStateChanged(ConnectionState state) {
                connectionState = state;
                if (state == ConnectionState.DISCONNECTED) {
                    connectionGeneration += 1;
                    PENDING_COMMANDS.clear();
                }
                dispatch(listener -> listener.onConnectionStateChanged(state));
            }

            @Override
            public void onMessage(SocketMessage message) {
                handleMessage(message);
            }

            @Override
            public void onError(Throwable throwable) {
                PENDING_COMMANDS.clear();
                String details = throwable == null ? "Socket error" : throwable.getMessage();
                dispatch(listener -> listener.onServerError("CLIENT_SOCKET_ERROR", details));
            }
        });
    }

    private ServerSession() {
    }

    public static void addListener(Listener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    public static LobbySnapshot getLatestLobbySnapshot() {
        return latestLobbySnapshot;
    }

    public static RoomSnapshot getLatestRoomSnapshot() {
        return latestRoomSnapshot;
    }

    public static GameSnapshot getLatestGameSnapshot() {
        return latestGameSnapshot;
    }

    public static String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public static String getCurrentRoomCode() {
        return currentRoomCode;
    }

    public static String getServerUrl(Context context) {
        return prefs(context).getString(KEY_SERVER_URL, DEFAULT_LAN_URL);
    }

    public static void setServerUrl(Context context, String serverUrl) {
        String normalizedUrl = normalizeServerUrl(serverUrl);
        prefs(context).edit().putString(KEY_SERVER_URL, normalizedUrl).apply();
        if (connectionState != ConnectionState.DISCONNECTED) {
            disconnect();
        }
    }

    public static String normalizeServerUrl(String serverUrl) {
        String value = serverUrl == null ? "" : serverUrl.trim();
        if (value.isEmpty()) {
            return DEFAULT_LAN_URL;
        }
        if (!value.endsWith("/game")) {
            value = value.endsWith("/") ? value + "game" : value + "/game";
        }
        return value;
    }

    public static void useDefaultLan(Context context) {
        setServerUrl(context, DEFAULT_LAN_URL);
    }

    public static void useDefaultWan(Context context) {
        setServerUrl(context, DEFAULT_WAN_URL);
    }

    public static void connect(Context context) {
        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
            return;
        }
        connectionGeneration += 1;
        CONTROLLER.connect(getServerUrl(context));
    }

    public static void disconnect() {
        connectionGeneration += 1;
        PENDING_COMMANDS.clear();
        clearRoomState();
        CONTROLLER.disconnect();
    }

    public static boolean createRoom(Context context, String nickname, String roomPassword) {
        return runWhenConnected(context, generation -> withIdToken(context, token -> {
            if (!isSameConnectedSession(generation)) {
                notifyNotConnected();
                return;
            }
            rememberPending(CONTROLLER.createRoom(nickname, token, roomPassword), MessageTypes.CREATE_ROOM);
        }));
    }

    public static boolean joinRoom(Context context, String roomCode, String nickname, String roomPassword) {
        return runWhenConnected(context, generation -> withIdToken(context, token -> {
            if (!isSameConnectedSession(generation)) {
                notifyNotConnected();
                return;
            }
            rememberPending(CONTROLLER.joinRoom(roomCode, nickname, token, roomPassword), MessageTypes.JOIN_ROOM);
        }));
    }

    public static void leaveRoom() {
        String leavingRoomCode = currentRoomCode;
        if (CONTROLLER.isConnected()) {
            rememberPending(CONTROLLER.leaveRoom(), MessageTypes.LEAVE_ROOM);
        } else {
            PENDING_COMMANDS.clear();
        }
        clearRoomState();
        removeRoomFromLatestLobby(leavingRoomCode);
    }

    public static void setReady(boolean ready) {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        rememberPending(CONTROLLER.setReady(ready, latestKnownRevision()), MessageTypes.SET_READY);
    }

    public static void startGame() {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        rememberPending(CONTROLLER.startGame(latestKnownRevision()), MessageTypes.START_GAME);
    }

    public static void rollDice(int diceRoll) {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        rememberPending(CONTROLLER.rollDice(diceRoll, latestKnownRevision()), MessageTypes.ROLL_DICE);
    }

    public static void applyTileEffect() {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        rememberPending(CONTROLLER.applyTileEffect(latestKnownRevision()), MessageTypes.APPLY_TILE_EFFECT);
    }

    public static void startMiniGame(String miniGameType) {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        rememberPending(CONTROLLER.startMiniGame(miniGameType, latestKnownRevision()), MessageTypes.START_MINI_GAME);
    }

    public static void submitMiniGameScore(int score) {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        rememberPending(CONTROLLER.submitMiniGameScore(score, latestKnownRevision()),
                MessageTypes.SUBMIT_MINI_GAME_SCORE);
    }

    public static void finishMiniGame() {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        rememberPending(CONTROLLER.finishMiniGame(latestKnownRevision()), MessageTypes.FINISH_MINI_GAME);
    }

    public static void submitMicroGameScore(int score) {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        rememberPending(CONTROLLER.submitMicroGameScore(score, latestKnownRevision()),
                MessageTypes.SUBMIT_MICRO_GAME_SCORE);
    }

    private static boolean runWhenConnected(Context context, ConnectedAction action) {
        if (connectionState == ConnectionState.CONNECTED) {
            action.run(connectionGeneration);
            return true;
        }
        if (connectionState == ConnectionState.DISCONNECTED) {
            connect(context.getApplicationContext());
        }
        PENDING_COMMANDS.clear();
        notifyNotConnected();
        return false;
    }

    private static boolean isSameConnectedSession(long generation) {
        return connectionState == ConnectionState.CONNECTED && connectionGeneration == generation;
    }

    private static void notifyNotConnected() {
        PENDING_COMMANDS.clear();
        dispatch(listener -> listener.onServerError(
                "CLIENT_NOT_CONNECTED",
                "서버 연결 후 다시 시도해 주세요."
        ));
    }

    private static void withIdToken(Context context, TokenConsumer consumer) {
        Context appContext = context.getApplicationContext();
        try {
            new FirebaseAuthTokenProvider().requireIdToken(new FirebaseAuthTokenProvider.TokenCallback() {
                @Override
                public void onToken(String idToken) {
                    consumer.accept(idToken);
                }

                @Override
                public void onError(Exception exception) {
                    consumer.accept(SessionPrefs.getOrCreateDevClientToken(appContext));
                }
            });
        } catch (RuntimeException exception) {
            consumer.accept(SessionPrefs.getOrCreateDevClientToken(appContext));
        }
    }

    private static void handleMessage(SocketMessage message) {
        String type = message.getType();
        if (MessageTypes.SERVER_HELLO.equals(type)) {
            dispatch(listener -> listener.onServerHello(message));
            return;
        }
        if (MessageTypes.LOBBY_UPDATED.equals(type)) {
            latestLobbySnapshot = SnapshotMessageMapper.toLobbySnapshot(message);
            dispatch(listener -> listener.onLobbyUpdated(latestLobbySnapshot));
            return;
        }
        if (MessageTypes.ROOM_UPDATED.equals(type)) {
            RoomSnapshot incomingRoom = SnapshotMessageMapper.toRoomSnapshot(message);
            if (!shouldAcceptRoomSnapshot(incomingRoom.getCode())) {
                return;
            }
            if (incomingRoom.getRevision() < latestRoomRevision) {
                return;
            }
            latestRoomSnapshot = incomingRoom;
            latestRoomRevision = incomingRoom.getRevision();
            currentRoomCode = latestRoomSnapshot.getCode();
            dispatch(listener -> listener.onRoomUpdated(latestRoomSnapshot));
            return;
        }
        if (MessageTypes.GAME_UPDATED.equals(type)) {
            GameSnapshot incomingGame = SnapshotMessageMapper.toGameSnapshot(message);
            if (!shouldAcceptRoomSnapshot(incomingGame.getRoomCode())) {
                return;
            }
            if (incomingGame.getRevision() < latestGameRevision) {
                return;
            }
            latestGameSnapshot = incomingGame;
            latestGameRevision = incomingGame.getRevision();
            dispatch(listener -> listener.onGameUpdated(latestGameSnapshot));
            return;
        }
        if (MessageTypes.REQUEST_OK.equals(type)) {
            String roomCode = message.getOrDefault("roomCode", "");
            String playerId = message.getOrDefault("playerId", "");
            String resolvedCommandType = PENDING_COMMANDS.remove(message.getRequestId());
            if (resolvedCommandType == null) {
                resolvedCommandType = "";
            }
            if (MessageTypes.LEAVE_ROOM.equals(resolvedCommandType)) {
                clearRoomState();
            }
            if (!roomCode.isEmpty()) {
                currentRoomCode = roomCode;
            }
            if (!playerId.isEmpty()) {
                currentPlayerId = playerId;
            }
            String commandType = resolvedCommandType;
            dispatch(listener -> listener.onRequestOk(commandType, roomCode, playerId));
            return;
        }
        if (MessageTypes.REQUEST_ERROR.equals(type)) {
            String resolvedCommandType = PENDING_COMMANDS.remove(message.getRequestId());
            if (resolvedCommandType == null) {
                resolvedCommandType = "";
            }
            if (MessageTypes.LEAVE_ROOM.equals(resolvedCommandType)) {
                clearRoomState();
            }
            String commandType = resolvedCommandType;
            dispatch(listener -> listener.onServerError(
                    message.getOrDefault("errorCode", "REQUEST_ERROR"),
                    message.getOrDefault("details", "Request failed")
            ));
        }
    }

    private static void clearRoomState() {
        currentRoomCode = "";
        currentPlayerId = "";
        latestRoomSnapshot = null;
        latestGameSnapshot = null;
        latestRoomRevision = -1L;
        latestGameRevision = -1L;
    }

    private static void removeRoomFromLatestLobby(String roomCode) {
        String normalizedRoomCode = roomCode == null ? "" : roomCode.trim();
        if (normalizedRoomCode.isEmpty() || latestLobbySnapshot == null) {
            return;
        }

        List<LobbySnapshot.RoomListInfo> retainedRooms = new ArrayList<>();
        boolean removed = false;
        for (LobbySnapshot.RoomListInfo room : latestLobbySnapshot.getRooms()) {
            if (normalizedRoomCode.equalsIgnoreCase(room.getCode())) {
                removed = true;
                continue;
            }
            retainedRooms.add(room);
        }
        if (!removed) {
            return;
        }

        latestLobbySnapshot = new LobbySnapshot(retainedRooms);
        dispatch(listener -> listener.onLobbyUpdated(latestLobbySnapshot));
    }

    private static void rememberPending(String requestId, String commandType) {
        if (requestId != null && !requestId.trim().isEmpty()) {
            PENDING_COMMANDS.put(requestId, commandType);
        }
    }

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

    private static boolean hasPendingRoomEntryCommand() {
        return PENDING_COMMANDS.containsValue(MessageTypes.CREATE_ROOM)
                || PENDING_COMMANDS.containsValue(MessageTypes.JOIN_ROOM);
    }

    private static long latestKnownRevision() {
        return Math.max(latestRoomRevision, latestGameRevision);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static void dispatch(DispatchAction action) {
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) {
                action.dispatch(listener);
            }
        });
    }

    private interface DispatchAction {
        void dispatch(Listener listener);
    }

    private interface ConnectedAction {
        void run(long generation);
    }

    private interface TokenConsumer {
        void accept(String token);
    }
}
