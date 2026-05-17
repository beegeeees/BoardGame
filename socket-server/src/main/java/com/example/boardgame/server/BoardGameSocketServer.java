package com.example.boardgame.server;

import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.SocketMessage;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BoardGameSocketServer extends WebSocketServer {
    private static final int DEFAULT_PORT = 8080;

    private final Map<WebSocket, ClientSession> sessions = new ConcurrentHashMap<>();
    private final GameSocketHandler gameSocketHandler;

    public BoardGameSocketServer(int port) {
        this(port, new FirebaseAdminAuthVerifier());
    }

    BoardGameSocketServer(int port, AuthVerifier authVerifier) {
        super(new InetSocketAddress(port));
        gameSocketHandler = new GameSocketHandler(this, authVerifier);
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new BoardGameSocketServer(port).start();
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        sessions.put(connection, new ClientSession(connection));
    }

    @Override
    public void onMessage(WebSocket connection, String text) {
        ClientSession session = sessions.get(connection);
        if (session == null) {
            return;
        }

        SocketMessage message = SocketMessage.parse(text);
        if (MessageTypes.APP_PING.equals(message.getType())) {
            session.send(SocketMessage.builder(MessageTypes.APP_PONG)
                    .requestId(message.getRequestId())
                    .build());
            return;
        }
        gameSocketHandler.handle(session, message);
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        ClientSession session = sessions.remove(connection);
        if (session != null) {
            gameSocketHandler.disconnect(session);
        }
    }

    @Override
    public void onError(WebSocket connection, Exception exception) {
        System.err.println("WebSocket error: " + exception.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("BoardGame socket server listening on ws://0.0.0.0:" + getPort() + "/game");
        setConnectionLostTimeout(30);
    }

    void sendToRoom(String roomCode, SocketMessage message) {
        for (ClientSession session : sessions.values()) {
            if (roomCode.equals(session.getRoomCode())) {
                session.send(message);
            }
        }
    }
}
