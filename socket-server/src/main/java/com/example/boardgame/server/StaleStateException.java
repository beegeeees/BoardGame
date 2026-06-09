package com.example.boardgame.server;

import com.example.boardgame.server.model.Room;

class StaleStateException extends IllegalStateException {
    private final Room room;

    StaleStateException(Room room) {
        super("Client state is stale");
        this.room = room;
    }

    Room getRoom() {
        return room;
    }
}
