package com.example.boardgame.server;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;
import com.example.boardgame.server.service.BoardGameService;
import com.example.boardgame.server.service.MicroGameService;
import com.example.boardgame.server.service.MiniGameService;
import com.example.boardgame.server.service.RoomService;
import com.example.boardgame.server.service.ScoreService;
import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.RoomSnapshot;
import com.example.boardgame.socket.protocol.SnapshotMessageMapper;
import com.example.boardgame.socket.protocol.SocketMessage;

public class GameSocketHandler {
    private final BoardGameSocketServer socketServer;
    private final AuthVerifier authVerifier;
    private final RoomService roomService = new RoomService();
    private final ScoreService scoreService = new ScoreService();
    private final BoardGameService boardGameService = new BoardGameService();
    private final MiniGameService miniGameService = new MiniGameService(boardGameService, scoreService);
    private final MicroGameService microGameService = new MicroGameService(boardGameService, scoreService);

    public GameSocketHandler(BoardGameSocketServer socketServer, AuthVerifier authVerifier) {
        this.socketServer = socketServer;
        this.authVerifier = authVerifier;
    }

    public synchronized void handle(ClientSession session, SocketMessage message) {
        try {
            Result result = handleCommand(session, message);
            sendOk(session, message, result);
            publishRoom(result.room);
            if (result.publishGame) {
                publishGame(result.room);
            }
        } catch (AuthException e) {
            session.sendError(message, "UNAUTHENTICATED", "Authentication required");
        } catch (IllegalArgumentException | IllegalStateException e) {
            session.sendError(message, "BAD_REQUEST", e.getMessage());
        }
    }

    public synchronized void disconnect(ClientSession session) {
        String roomCode = session.getRoomCode();
        String playerId = session.getPlayerId();
        if (roomCode.isEmpty() || playerId.isEmpty()) {
            return;
        }

        roomService.disconnect(roomCode, playerId);
        try {
            publishRoom(roomService.requireRoom(roomCode));
        } catch (IllegalArgumentException ignored) {
            // The room was removed because the last player disconnected.
        }
    }

    private Result handleCommand(ClientSession session, SocketMessage message) {
        switch (message.getType()) {
            case MessageTypes.CREATE_ROOM:
                return createRoom(session, message);
            case MessageTypes.JOIN_ROOM:
                return joinRoom(session, message);
            case MessageTypes.MATCHMAKE:
                return matchmake(session, message);
            case MessageTypes.SET_READY:
                return setReady(session, message);
            case MessageTypes.START_GAME:
                return startGame(session);
            case MessageTypes.ROLL_DICE:
                return rollDice(session);
            case MessageTypes.APPLY_TILE_EFFECT:
                return applyTileEffect(session);
            case MessageTypes.START_MINI_GAME:
                return startMiniGame(session, message);
            case MessageTypes.SUBMIT_MINI_GAME_SCORE:
                return submitMiniGameScore(session, message);
            case MessageTypes.FINISH_MINI_GAME:
                return finishMiniGame(session);
            case MessageTypes.SUBMIT_MICRO_GAME_SCORE:
                return submitMicroGameScore(session, message);
            case MessageTypes.FINISH_MICRO_GAME:
                return finishMicroGame(session);
            default:
                throw new IllegalArgumentException("Unsupported type: " + message.getType());
        }
    }

    private Result createRoom(ClientSession session, SocketMessage message) {
        requireNotInRoom(session);
        String firebaseUid = verify(message, session);
        Room room = roomService.createRoom(firebaseUid, message.getOrDefault("nickname", "Player"));
        Player player = room.getPlayerList().iterator().next();
        session.bindPlayer(room.getCode(), player.getId(), firebaseUid);
        return Result.roomOnly(room, player);
    }

    private Result joinRoom(ClientSession session, SocketMessage message) {
        requireNotInRoom(session);
        String roomCode = message.getOrDefault("roomCode", "");
        String firebaseUid = verify(message, session);
        Player player = roomService.joinRoom(roomCode, firebaseUid, message.getOrDefault("nickname", "Player"));
        Room room = roomService.requireRoom(roomCode);
        session.bindPlayer(room.getCode(), player.getId(), firebaseUid);
        return Result.roomOnly(room, player);
    }

    private Result matchmake(ClientSession session, SocketMessage message) {
        requireNotInRoom(session);
        String firebaseUid = verify(message, session);
        RoomService.MatchResult matchResult = roomService.matchmake(
                firebaseUid,
                message.getOrDefault("nickname", "Player")
        );
        session.bindPlayer(matchResult.getRoom().getCode(), matchResult.getPlayer().getId(), firebaseUid);
        return Result.roomOnly(matchResult.getRoom(), matchResult.getPlayer());
    }

    private Result setReady(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        roomService.setReady(room.getCode(), session.getPlayerId(), message.getBoolean("ready", false));
        return Result.roomOnly(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private Result startGame(ClientSession session) {
        Room room = requireBoundRoom(session);
        roomService.requireHost(room, session.getPlayerId());
        boardGameService.startGame(room);
        return Result.roomAndGame(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private Result rollDice(ClientSession session) {
        Room room = requireBoundRoom(session);
        boardGameService.rollDice(room, session.getPlayerId());
        return Result.roomAndGame(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private Result applyTileEffect(ClientSession session) {
        Room room = requireBoundRoom(session);
        String tileType = boardGameService.applyTileEffect(room, session.getPlayerId());
        if (BoardGameService.TILE_GAME.equals(tileType)) {
            microGameService.startMicroGame(room, session.getPlayerId(), "QUICK_TAP");
        }
        return Result.roomAndGame(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private Result startMiniGame(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        roomService.requireHost(room, session.getPlayerId());
        miniGameService.startMiniGame(room, message.getOrDefault("miniGameType", ""));
        return Result.roomAndGame(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private Result submitMiniGameScore(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        miniGameService.submitMiniGameScore(room, session.getPlayerId(), message.getInt("score", 0));
        return Result.roomAndGame(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private Result finishMiniGame(ClientSession session) {
        Room room = requireBoundRoom(session);
        roomService.requireHost(room, session.getPlayerId());
        miniGameService.finishMiniGame(room);
        return Result.roomAndGame(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private Result submitMicroGameScore(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        microGameService.submitMicroGameScore(room, session.getPlayerId(), message.getInt("score", 0));
        return Result.roomAndGame(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private Result finishMicroGame(ClientSession session) {
        Room room = requireBoundRoom(session);
        roomService.requireHost(room, session.getPlayerId());
        microGameService.finishMicroGame(room);
        return Result.roomAndGame(room, roomService.requirePlayer(room, session.getPlayerId()));
    }

    private void sendOk(ClientSession session, SocketMessage request, Result result) {
        session.send(SocketMessage.builder(MessageTypes.REQUEST_OK)
                .requestId(request.getRequestId())
                .put("roomCode", result.room.getCode())
                .put("playerId", result.player.getId())
                .put("status", result.room.getStatus())
                .build());
    }

    private void publishRoom(Room room) {
        RoomSnapshot snapshot = room.toSnapshot();
        socketServer.sendToRoom(room.getCode(), SnapshotMessageMapper.roomUpdated(snapshot));
    }

    private void publishGame(Room room) {
        GameState gameState = room.getGameState();
        if (gameState == null) {
            return;
        }
        GameSnapshot snapshot = gameState.toSnapshot();
        socketServer.sendToRoom(room.getCode(), SnapshotMessageMapper.gameUpdated(snapshot));
    }

    private Room requireBoundRoom(ClientSession session) {
        return roomService.requireRoom(session.getRoomCode());
    }

    private void requireNotInRoom(ClientSession session) {
        if (!session.getRoomCode().isEmpty()) {
            throw new IllegalStateException("Connection is already in a room");
        }
    }

    private String verify(SocketMessage message, ClientSession session) {
        return authVerifier.verify(message.getOrDefault("firebaseIdToken", ""), session.getConnectionId());
    }

    private static class Result {
        private final Room room;
        private final Player player;
        private final boolean publishGame;

        private Result(Room room, Player player, boolean publishGame) {
            this.room = room;
            this.player = player;
            this.publishGame = publishGame;
        }

        static Result roomOnly(Room room, Player player) {
            return new Result(room, player, false);
        }

        static Result roomAndGame(Room room, Player player) {
            return new Result(room, player, true);
        }
    }
}
