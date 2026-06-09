package com.example.boardgame.controller.socket;

import com.example.boardgame.socket.BoardGameSocketClient;
import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.SocketEventListener;
import com.example.boardgame.socket.protocol.SocketMessage;

import java.util.UUID;

public class SocketRoomController {

    private final BoardGameSocketClient socketClient;

    public SocketRoomController() {
        this(new BoardGameSocketClient());
    }

    public SocketRoomController(BoardGameSocketClient socketClient) {
        this.socketClient = socketClient;
    }

    public void setListener(SocketEventListener listener) {
        socketClient.setListener(listener);
    }

    public void connect(String serverUrl) {
        socketClient.connect(serverUrl);
    }

    public void disconnect() {
        socketClient.disconnect();
    }

    public boolean isConnected() {
        return socketClient.getState() == ConnectionState.CONNECTED;
    }

    public String createRoom(String nickname, String firebaseIdToken, String roomPassword) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                commandBuilder(MessageTypes.CREATE_ROOM, requestId, nickname, firebaseIdToken)
                        .put("roomPassword", roomPassword)
                        .build()
        );
        return requestId;
    }

    public String joinRoom(String roomCode, String nickname, String firebaseIdToken, String roomPassword) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                commandBuilder(MessageTypes.JOIN_ROOM, requestId, nickname, firebaseIdToken)
                        .put("roomCode", roomCode)
                        .put("roomPassword", roomPassword)
                        .build()
        );
        return requestId;
    }

    public String leaveRoom() {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.LEAVE_ROOM)
                        .requestId(requestId)
                        .build()
        );
        return requestId;
    }

    public String setReady(boolean ready, long expectedRevision) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.SET_READY)
                        .requestId(requestId)
                        .put("ready", ready)
                        .put("expectedRevision", expectedRevision)
                        .build()
        );
        return requestId;
    }

    public String startGame(long expectedRevision) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.START_GAME)
                        .requestId(requestId)
                        .put("expectedRevision", expectedRevision)
                        .build()
        );
        return requestId;
    }

    public String rollDice(int diceRoll, long expectedRevision) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.ROLL_DICE)
                        .requestId(requestId)
                        .put("diceRoll", diceRoll)
                        .put("expectedRevision", expectedRevision)
                        .build()
        );
        return requestId;
    }

    public String applyTileEffect(long expectedRevision) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.APPLY_TILE_EFFECT)
                        .requestId(requestId)
                        .put("expectedRevision", expectedRevision)
                        .build()
        );
        return requestId;
    }

    public String startMiniGame(String miniGameType, long expectedRevision) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.START_MINI_GAME)
                        .requestId(requestId)
                        .put("miniGameType", miniGameType)
                        .put("expectedRevision", expectedRevision)
                        .build()
        );
        return requestId;
    }

    public String submitMiniGameScore(int score, long expectedRevision) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.SUBMIT_MINI_GAME_SCORE)
                        .requestId(requestId)
                        .put("score", score)
                        .put("expectedRevision", expectedRevision)
                        .build()
        );
        return requestId;
    }

    public String finishMiniGame(long expectedRevision) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.FINISH_MINI_GAME)
                        .requestId(requestId)
                        .put("expectedRevision", expectedRevision)
                        .build()
        );
        return requestId;
    }

    public String submitMicroGameScore(int score, long expectedRevision) {
        String requestId = UUID.randomUUID().toString();
        socketClient.send(
                SocketMessage.builder(MessageTypes.SUBMIT_MICRO_GAME_SCORE)
                        .requestId(requestId)
                        .put("score", score)
                        .put("expectedRevision", expectedRevision)
                        .build()
        );
        return requestId;
    }

    private SocketMessage.Builder commandBuilder(
            String type,
            String requestId,
            String nickname,
            String firebaseIdToken
    ) {
        return SocketMessage.builder(type)
                .requestId(requestId)
                .put("nickname", nickname)
                .put("firebaseIdToken", firebaseIdToken);
    }
}
