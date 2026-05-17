package com.example.boardgame.server.service;

import com.example.boardgame.server.model.Room;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RoomServiceTest {
    @Test
    public void sameUidCannotCreateTwoRooms() {
        RoomService roomService = new RoomService();
        roomService.createRoom("uid-1", "Player 1");

        try {
            roomService.createRoom("uid-1", "Player 1 again");
            fail("Expected duplicate UID to be rejected");
        } catch (IllegalStateException expected) {
            assertEquals("User is already in a room", expected.getMessage());
        }
    }

    @Test
    public void sameUidCannotJoinAnotherRoom() {
        RoomService roomService = new RoomService();
        roomService.createRoom("uid-1", "Player 1");
        Room secondRoom = roomService.createRoom("uid-2", "Player 2");

        try {
            roomService.joinRoom(secondRoom.getCode(), "uid-1", "Player 1 again");
            fail("Expected duplicate UID to be rejected");
        } catch (IllegalStateException expected) {
            assertEquals("User is already in a room", expected.getMessage());
        }
    }

    @Test
    public void uidCanJoinAfterDisconnect() {
        RoomService roomService = new RoomService();
        Room firstRoom = roomService.createRoom("uid-1", "Player 1");
        String playerId = firstRoom.getPlayerList().iterator().next().getId();
        roomService.disconnect(firstRoom.getCode(), playerId);

        Room secondRoom = roomService.createRoom("uid-2", "Player 2");
        roomService.joinRoom(secondRoom.getCode(), "uid-1", "Player 1 again");

        assertEquals(2, secondRoom.getPlayers().size());
    }
}
