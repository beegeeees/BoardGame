package com.example.adchaosdemo

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.adchaosdemo.socket.SocketRoomController
import com.example.adchaosdemo.socket.protocol.ConnectionState
import com.example.adchaosdemo.socket.protocol.GameSnapshot
import com.example.adchaosdemo.socket.protocol.MessageTypes
import com.example.adchaosdemo.socket.protocol.RoomSnapshot
import com.example.adchaosdemo.socket.protocol.SnapshotMessageMapper
import com.example.adchaosdemo.socket.protocol.SocketEventListener
import com.example.adchaosdemo.socket.protocol.SocketMessage
import java.util.UUID

object ServerRoomGateway : SocketEventListener {
    private const val PREFS = "server_room_gateway"
    private const val KEY_WS_URL = "ws_url"
    private const val DEFAULT_WS_URL = "ws://10.0.2.2:8080/game"

    interface Listener {
        fun onConnectionStateChanged(state: ConnectionState) {}
        fun onRoomUpdated(room: RoomSnapshot) {}
        fun onGameUpdated(game: GameSnapshot) {}
        fun onServerError(message: String) {}
        fun onRequestOk(type: String, roomCode: String, playerId: String) {}
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val controller = SocketRoomController()
    private val listeners = linkedSetOf<Listener>()
    private var pendingCommand: (() -> Unit)? = null
    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private val devSessionToken: String = "DEV_${UUID.randomUUID()}"

    var currentRoomCode: String = ""
        private set
    var currentPlayerId: String = ""
        private set
    var latestRoomSnapshot: RoomSnapshot? = null
        private set
    var latestGameSnapshot: GameSnapshot? = null
        private set

    init {
        controller.setListener(this)
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun connect(context: Context) {
        controller.connect(getServerUrl(context))
    }

    fun disconnect() {
        controller.disconnect()
        currentRoomCode = ""
        currentPlayerId = ""
        latestRoomSnapshot = null
        latestGameSnapshot = null
    }

    fun createRoom(context: Context, nickname: String, firebaseIdToken: String = "") {
        val token = firebaseIdToken.ifBlank { devSessionToken }
        val command = { controller.createRoom(nickname, token) }
        runOrQueueCommand(context, command)
    }

    fun joinRoom(context: Context, roomCode: String, nickname: String, firebaseIdToken: String = "") {
        val token = firebaseIdToken.ifBlank { devSessionToken }
        val command = { controller.joinRoom(roomCode, nickname, token) }
        runOrQueueCommand(context, command)
    }

    fun setReady(ready: Boolean) {
        controller.setReady(ready)
    }

    fun startGame() {
        controller.startGame()
    }

    fun getServerUrl(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_WS_URL, DEFAULT_WS_URL)
            .orEmpty()
            .ifBlank { DEFAULT_WS_URL }
    }

    fun setServerUrl(context: Context, wsUrl: String) {
        val normalized = normalizeServerUrl(wsUrl)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WS_URL, normalized)
            .apply()
    }

    fun normalizeServerUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isBlank()) return DEFAULT_WS_URL

        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd <= 0) return trimmed

        val pathStart = trimmed.indexOf('/', schemeEnd + 3)
        val base = if (pathStart >= 0) trimmed.substring(0, pathStart) else trimmed
        return "$base/game"
    }

    override fun onStateChanged(state: ConnectionState) {
        connectionState = state
        if (state == ConnectionState.CONNECTED) {
            val command = pendingCommand
            pendingCommand = null
            command?.invoke()
        }
        mainHandler.post {
            listeners.forEach { it.onConnectionStateChanged(state) }
        }
    }

    override fun onMessage(message: SocketMessage) {
        when (message.type) {
            MessageTypes.REQUEST_OK -> {
                val roomCode = message.getOrDefault("roomCode", "")
                val playerId = message.getOrDefault("playerId", "")
                if (roomCode.isNotBlank()) currentRoomCode = roomCode
                if (playerId.isNotBlank()) currentPlayerId = playerId
                mainHandler.post {
                    listeners.forEach { it.onRequestOk("", roomCode, playerId) }
                }
            }

            MessageTypes.REQUEST_ERROR -> {
                val code = message.getOrDefault("code", "REQUEST_ERROR")
                val reason = message.getOrDefault("reason", "요청 실패")
                postError("$code: $reason")
            }

            MessageTypes.ROOM_UPDATED -> {
                latestRoomSnapshot = SnapshotMessageMapper.toRoomSnapshot(message)
                mainHandler.post {
                    latestRoomSnapshot?.let { snapshot ->
                        currentRoomCode = snapshot.code
                        listeners.forEach { it.onRoomUpdated(snapshot) }
                    }
                }
            }

            MessageTypes.GAME_UPDATED -> {
                latestGameSnapshot = SnapshotMessageMapper.toGameSnapshot(message)
                mainHandler.post {
                    latestGameSnapshot?.let { snapshot ->
                        listeners.forEach { it.onGameUpdated(snapshot) }
                    }
                }
            }

            MessageTypes.SERVER_NOTICE -> {
                val notice = message.getOrDefault("message", "SERVER_NOTICE")
                postError(notice)
            }
        }
    }

    override fun onError(throwable: Throwable) {
        postError(throwable.message ?: "네트워크 연결 실패")
    }

    private fun postError(message: String) {
        mainHandler.post {
            listeners.forEach { it.onServerError(message) }
        }
    }

    private fun runOrQueueCommand(context: Context, command: () -> Unit) {
        if (connectionState == ConnectionState.CONNECTED) {
            command.invoke()
            return
        }
        pendingCommand = command
        connect(context)
    }
}
