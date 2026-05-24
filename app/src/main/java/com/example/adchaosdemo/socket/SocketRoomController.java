package com.example.adchaosdemo.socket;

import com.example.adchaosdemo.socket.protocol.MessageTypes;
import com.example.adchaosdemo.socket.protocol.SocketEventListener;
import com.example.adchaosdemo.socket.protocol.SocketMessage;

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

    public void createRoom(String nickname, String firebaseIdToken) {
        socketClient.send(commandBuilder(MessageTypes.CREATE_ROOM, nickname, firebaseIdToken).build());
    }

    public void joinRoom(String roomCode, String nickname, String firebaseIdToken) {
        socketClient.send(commandBuilder(MessageTypes.JOIN_ROOM, nickname, firebaseIdToken)
                .put("roomCode", roomCode)
                .build());
    }

    public void setReady(boolean ready) {
        socketClient.send(SocketMessage.builder(MessageTypes.SET_READY)
                .requestId(UUID.randomUUID().toString())
                .put("ready", ready)
                .build());
    }

    public void startGame() {
        socketClient.send(SocketMessage.command(MessageTypes.START_GAME));
    }

    private SocketMessage.Builder commandBuilder(String type, String nickname, String firebaseIdToken) {
        return SocketMessage.builder(type)
                .requestId(UUID.randomUUID().toString())
                .put("nickname", nickname)
                .put("firebaseIdToken", firebaseIdToken);
    }
}

