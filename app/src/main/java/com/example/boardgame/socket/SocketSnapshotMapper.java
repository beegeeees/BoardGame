package com.example.boardgame.socket;

import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;
import com.example.boardgame.socket.protocol.SnapshotCodec;
import com.example.boardgame.socket.protocol.SocketMessage;

public final class SocketSnapshotMapper {
    private SocketSnapshotMapper() {
    }

    public static RoomSnapshot toRoomSnapshot(SocketMessage message) {
        return new RoomSnapshot(
                message.getOrDefault("roomCode", ""),
                message.getOrDefault("hostPlayerId", ""),
                message.getOrDefault("status", ""),
                SnapshotCodec.decodePlayers(message.getOrDefault("players", ""))
        );
    }

    public static GameSnapshot toGameSnapshot(SocketMessage message) {
        return new GameSnapshot(
                message.getOrDefault("roomCode", ""),
                message.getInt("currentRound", 1),
                message.getInt("finalRound", 5),
                message.getOrDefault("currentPlayerId", ""),
                message.getInt("lastDiceRoll", 0),
                message.getOrDefault("turnPhase", ""),
                SnapshotCodec.decodeIds(message.getOrDefault("turnOrder", ""))
        );
    }
}
