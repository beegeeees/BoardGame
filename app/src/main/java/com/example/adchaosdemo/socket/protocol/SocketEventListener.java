package com.example.adchaosdemo.socket.protocol;

public interface SocketEventListener {
    void onStateChanged(ConnectionState state);
    void onMessage(SocketMessage message);
    void onError(Throwable throwable);
}

