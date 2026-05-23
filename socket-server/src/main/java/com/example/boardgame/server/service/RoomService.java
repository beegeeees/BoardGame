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
    //  보드게임은 혼자 할 수 없으므로 최소 인원을 2명으로 변경했습니다.
    // (만약 혼자서 UI 테스트를 해야 한다면 임시로 1로 낮춰서 테스트하세요.)
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

    public static class MatchResult {
        private final Room room;
        private final Player player;

        public MatchResult(Room room, Player player) {
            this.room = room;
            this.player = player;
        }

        public Room getRoom() { return room; }
        public Player getPlayer() { return player; }
    }

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public synchronized Room createRoom(String firebaseUid, String nickname) {
        requireUidAvailable(firebaseUid); // 중복 로그인 방지
        Room room = new Room(createUniqueRoomCode());
        room.addPlayer(createPlayer(firebaseUid, nickname));
        rooms.put(room.getCode(), room);
        return room;
    }

    public synchronized Player joinRoom(String roomCode, String firebaseUid, String nickname) {
        requireUidAvailable(firebaseUid);
        Room room = requireRoom(roomCode);

        // 이미 게임이 시작되었거나 끝난 방에는 난입 불가
        if (!Room.WAITING.equals(room.getStatus()) && !Room.READY.equals(room.getStatus())) {
            throw new IllegalStateException("Room is already in game");
        }
        if (room.getPlayers().size() >= MAX_PLAYERS) {
            throw new IllegalStateException("Room is full");
        }

        Player player = createPlayer(firebaseUid, nickname);
        room.addPlayer(player);
        room.refreshReadyStatus(MIN_PLAYERS); // 2명 이상이고 모두 Ready면 READY 상태로 전환
        return player;
    }

    public synchronized MatchResult matchmake(String firebaseUid, String nickname) {
        // 빈자리가 있는 대기방 자동 탐색
        for (Room room : rooms.values()) {
            if ((Room.WAITING.equals(room.getStatus()) || Room.READY.equals(room.getStatus()))
                    && room.getPlayers().size() < MAX_PLAYERS) {
                Player player = joinRoom(room.getCode(), firebaseUid, nickname);
                return new MatchResult(room, player);
            }
        }

        // 빈 방이 없으면 내가 새로 방을 팜
        Room room = createRoom(firebaseUid, nickname);
        Player player = room.getPlayerList().iterator().next();
        return new MatchResult(room, player);
    }

    public synchronized void setReady(String roomCode, String playerId, boolean ready) {
        Room room = requireRoom(roomCode);
        Player player = requirePlayer(room, playerId);
        player.setReady(ready);
        room.refreshReadyStatus(MIN_PLAYERS);
    }

    public synchronized void disconnect(String roomCode, String playerId) {
        Room room = rooms.get(roomCode);
        if (room == null) {
            return;
        }

        room.removePlayer(playerId);

        if (room.getPlayers().isEmpty()) {
            // 방에 아무도 안 남으면 방 폭파
            rooms.remove(roomCode);
        } else {
            // ⚠️ [멤버 3을 위한 중요 체크포인트]
            // 대기방일 때는 인원수가 줄어들면 다시 WAITING 상태로 돌아가면 되지만,
            // 게임 도중(IN_GAME)에 누군가 튕기면 어떻게 할지 멤버 2와 상의해야 합니다.
            // 현재 Room.java의 refreshReadyStatus 로직상 게임 도중에는 상태가 변하지 않으므로,
            // 남은 사람들이 계속 게임을 하거나 강제 종료 시키는 추가 로직이 필요할 수 있습니다.
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
        // Player 고유 ID를 생성하여 반환 (Firebase UID와는 별개의 세션 ID 역할)
        return new Player(UUID.randomUUID().toString(), firebaseUid, nickname);
    }

    private void requireUidAvailable(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.trim().isEmpty()) {
            throw new IllegalArgumentException("Firebase UID is required");
        }

        for (Room room : rooms.values()) {
            for (Player player : room.getPlayerList()) {
                if (firebaseUid.equals(player.getFirebaseUid())) {
                    throw new IllegalStateException("User is already in a room");
                }
            }
        }
    }

    private String createUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = String.valueOf(100000 + random.nextInt(900000)); // 6자리 난수
        } while (rooms.containsKey(roomCode));
        return roomCode;
    }
}
