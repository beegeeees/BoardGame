package com.example.boardgame.server.service;

import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomService {
    public static final int MIN_PLAYERS = 1;
    public static final int MAX_PLAYERS = 4;

    public static class MatchResult {
        private final Room room;
        private final Player player;

        public MatchResult(Room room, Player player) {
            this.room = room;
            this.player = player;
        }

        public Room getRoom() {
            return room;
        }

        public Player getPlayer() {
            return player;
        }
    }

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public Room createRoom(String firebaseUid, String nickname) {
        Room room = new Room(createUniqueRoomCode());
        room.addPlayer(createPlayer(firebaseUid, nickname));
        rooms.put(room.getCode(), room);
        return room;
    }

    public Player joinRoom(String roomCode, String firebaseUid, String nickname) {
        Room room = requireRoom(roomCode);
        if (!Room.WAITING.equals(room.getStatus()) && !Room.READY.equals(room.getStatus())) {
            throw new IllegalStateException("Room is already in game");
        }
        if (room.getPlayers().size() >= MAX_PLAYERS) {
            throw new IllegalStateException("Room is full");
        }

        Player player = createPlayer(firebaseUid, nickname);
        room.addPlayer(player);
        room.refreshReadyStatus(MIN_PLAYERS);
        return player;
    }

    public MatchResult matchmake(String firebaseUid, String nickname) {
        for (Room room : rooms.values()) {
            if ((Room.WAITING.equals(room.getStatus()) || Room.READY.equals(room.getStatus()))
                    && room.getPlayers().size() < MAX_PLAYERS) {
                Player player = joinRoom(room.getCode(), firebaseUid, nickname);
                return new MatchResult(room, player);
            }
        }

        Room room = createRoom(firebaseUid, nickname);
        Player player = room.getPlayerList().iterator().next();
        return new MatchResult(room, player);
    }

    public void setReady(String roomCode, String playerId, boolean ready) {
        Room room = requireRoom(roomCode);
        Player player = requirePlayer(room, playerId);
        player.setReady(ready);
        room.refreshReadyStatus(MIN_PLAYERS);
    }

    public void disconnect(String roomCode, String playerId) {
        Room room = rooms.get(roomCode);
        if (room == null) {
            return;
        }
        room.removePlayer(playerId);
        if (room.getPlayers().isEmpty()) {
            rooms.remove(roomCode);
        } else {
            room.refreshReadyStatus(MIN_PLAYERS);
        }
    }

    public Room requireRoom(String roomCode) {
        Room room = rooms.get(roomCode);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        return room;
    }

    public Player requirePlayer(Room room, String playerId) {
        Player player = room.getPlayers().get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found in room");
        }
        return player;
    }

    public void requireHost(Room room, String playerId) {
        if (!playerId.equals(room.getHostPlayerId())) {
            throw new IllegalStateException("Only the host can start the game");
        }
    }

    public Collection<Room> getRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    private Player createPlayer(String firebaseUid, String nickname) {
        return new Player(UUID.randomUUID().toString(), firebaseUid, nickname);
    }

    private String createUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = String.valueOf(100000 + random.nextInt(900000));
        } while (rooms.containsKey(roomCode));
        return roomCode;
    }
}
