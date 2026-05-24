package com.example.adchaosdemo.socket.protocol;

public final class MessageTypes {
    public static final String CREATE_ROOM = "CREATE_ROOM";
    public static final String JOIN_ROOM = "JOIN_ROOM";
    public static final String MATCHMAKE = "MATCHMAKE";
    public static final String SET_READY = "SET_READY";
    public static final String START_GAME = "START_GAME";

    public static final String REQUEST_OK = "REQUEST_OK";
    public static final String REQUEST_ERROR = "REQUEST_ERROR";
    public static final String ROOM_UPDATED = "ROOM_UPDATED";
    public static final String GAME_UPDATED = "GAME_UPDATED";
    public static final String SERVER_NOTICE = "SERVER_NOTICE";
    public static final String APP_PING = "APP_PING";
    public static final String APP_PONG = "APP_PONG";

    private MessageTypes() {
    }
}

