package com.example.boardgame.server.model;

import com.example.boardgame.socket.protocol.PlayerSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Room {
    // ⚠️ [추가 포인트 1]  최대 인원 설정
    public static final int MAX_PLAYERS = 4;

    public static final String WAITING = "WAITING";
    public static final String READY = "READY";
    public static final String IN_GAME = "IN_GAME";
    public static final String FINISHED = "FINISHED";

    private final String code;
    private final long createdAtMillis;
    private long updatedAtMillis;
    private String hostPlayerId = "";
    private String status = WAITING;

    // LinkedHashMap 유지: 유저가 방에 들어온 순서를 보장하여 턴 순서 배정에 유리함
    private final Map<String, Player> players = new LinkedHashMap<>();

    // 전담하여 관리할 게임의 핵심 상태들
    private GameState gameState;
    private MiniGameState miniGameState;
    private MicroGameState microGameState;

    public Room(String code) {
        this.code = code;
        this.createdAtMillis = System.currentTimeMillis();
        this.updatedAtMillis = createdAtMillis;
    }

    // 인원 제한 방어 로직 추가
    public boolean addPlayer(Player player) {
        // 이미 방이 꽉 찼고, 새로 들어오려는 유저라면 입장 거부
        if (players.size() >= MAX_PLAYERS && !players.containsKey(player.getId())) {
            return false;
        }

        players.put(player.getId(), player);
        if (hostPlayerId.isEmpty()) {
            hostPlayerId = player.getId();
            player.setHost(true);
        }
        touch();
        return true;
    }

    public void removePlayer(String playerId) {
        Player removed = players.remove(playerId);
        if (removed != null) {
            removed.setHost(false);
        }
        if (playerId != null && playerId.equals(hostPlayerId)) {
            hostPlayerId = players.isEmpty() ? "" : players.keySet().iterator().next();
            Player host = players.get(hostPlayerId);
            if (host != null) {
                host.setHost(true);
            }
        }
        touch();
    }

    public boolean canStart(int minPlayers) {
        // 인원수가 부족하거나 최대 인원을 초과하면 시작 불가
        if (players.size() < minPlayers || players.size() > MAX_PLAYERS) {
            return false;
        }
        for (Player player : players.values()) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }

    public void refreshReadyStatus(int minPlayers) {
        if (IN_GAME.equals(status) || FINISHED.equals(status)) {
            return;
        }
        status = canStart(minPlayers) ? READY : WAITING;
        touch();
    }

    // [추가 포인트 2] 특정 플레이어 데이터를 쉽게 꺼내기 위한 헬퍼 메서드
    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public RoomSnapshot toSnapshot() {
        List<PlayerSnapshot> playerSnapshots = new ArrayList<>();
        for (Player player : players.values()) {
            playerSnapshots.add(player.toSnapshot());
        }
        return new RoomSnapshot(code, hostPlayerId, status, playerSnapshots);
    }

    public void touch() {
        updatedAtMillis = System.currentTimeMillis();
    }

    // --- Getters & Setters ---

    public String getCode() { return code; }
    public String getHostPlayerId() { return hostPlayerId; }
    public String getStatus() { return status; }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public Map<String, Player> getPlayers() { return players; }
    public Collection<Player> getPlayerList() { return players.values(); }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        touch();
    }

    public MiniGameState getMiniGameState() { return miniGameState; }
    public void setMiniGameState(MiniGameState miniGameState) {
        this.miniGameState = miniGameState;
        touch();
    }

    public MicroGameState getMicroGameState() { return microGameState; }
    public void setMicroGameState(MicroGameState microGameState) {
        this.microGameState = microGameState;
        touch();
    }

    public long getCreatedAtMillis() { return createdAtMillis; }
    public long getUpdatedAtMillis() { return updatedAtMillis; }
}
