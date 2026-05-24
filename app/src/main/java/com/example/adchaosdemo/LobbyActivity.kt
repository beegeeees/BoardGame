package com.example.adchaosdemo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adchaosdemo.socket.protocol.ConnectionState
import com.example.adchaosdemo.socket.protocol.GameSnapshot
import com.example.adchaosdemo.socket.protocol.RoomSnapshot

class LobbyActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MY_NICKNAME = "extra_my_nickname"
        const val EXTRA_ROOM_CODE = "extra_room_code"
        const val EXTRA_LEFT_ROOM_CODE = "extra_left_room_code"
    }

    private lateinit var roomCodeText: TextView
    private lateinit var roomMetaText: TextView
    private lateinit var startButton: Button
    private lateinit var leaveButton: Button
    private lateinit var startHintText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var pendingStartRunnable: Runnable? = null
    private var roomCode: String = ""
    private var myNickname: String = ""
    private var isNavigatingToBoard = false
    private var isLeavingLobby = false
    private var debugMode = false
    private var localRoomState: RoomState? = null

    private val gatewayListener = object : ServerRoomGateway.Listener {
        override fun onConnectionStateChanged(state: ConnectionState) {
            if (debugMode) return
            if (state == ConnectionState.DISCONNECTED) {
                Toast.makeText(this@LobbyActivity, "서버 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRoomUpdated(room: RoomSnapshot) {
            if (debugMode) return
            if (room.code.equals(roomCode, ignoreCase = true)) {
                renderRoomSnapshot(room)
            }
        }

        override fun onGameUpdated(game: GameSnapshot) {
            if (debugMode) return
            if (!game.roomCode.equals(roomCode, ignoreCase = true)) return
            if (isNavigatingToBoard) return
            isNavigatingToBoard = true
            val players = ServerRoomGateway.latestRoomSnapshot?.players
                ?.map { it.nickname }
                ?.take(4)
                .orEmpty()
            moveToBoard(players)
        }

        override fun onServerError(message: String) {
            if (debugMode) return
            Toast.makeText(this@LobbyActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val slotNameViews by lazy {
        listOf(
            findViewById<TextView>(R.id.slot1Name),
            findViewById<TextView>(R.id.slot2Name),
            findViewById<TextView>(R.id.slot3Name),
            findViewById<TextView>(R.id.slot4Name)
        )
    }

    private val slotStateViews by lazy {
        listOf(
            findViewById<TextView>(R.id.slot1State),
            findViewById<TextView>(R.id.slot2State),
            findViewById<TextView>(R.id.slot3State),
            findViewById<TextView>(R.id.slot4State)
        )
    }

    private val slotCards by lazy {
        listOf(
            findViewById<TextView>(R.id.slot1Name).parent as android.view.View,
            findViewById<TextView>(R.id.slot2Name).parent as android.view.View,
            findViewById<TextView>(R.id.slot3Name).parent as android.view.View,
            findViewById<TextView>(R.id.slot4Name).parent as android.view.View
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        roomCodeText = findViewById(R.id.roomCodeText)
        roomMetaText = findViewById(R.id.roomMetaText)
        startButton = findViewById(R.id.startGameButton)
        startHintText = findViewById(R.id.startHintText)
        leaveButton = findViewById(R.id.leaveRoomButton)

        myNickname = intent.getStringExtra(EXTRA_MY_NICKNAME)
            .orEmpty()
            .ifBlank { SessionPrefs.getNickname(this).ifBlank { "PlayerMe" } }
        roomCode = intent.getStringExtra(EXTRA_ROOM_CODE).orEmpty().ifBlank { "111111" }
        debugMode = SessionPrefs.isDebugMode(this)

        ServerRoomGateway.addListener(gatewayListener)
        roomCodeText.text = getString(R.string.lobby_room_code, roomCode)

        if (debugMode) {
            ensureLocalJoin()
            localRoomState?.let { renderLocalRoom(it) }
        } else {
            ServerRoomGateway.joinRoom(this, roomCode, myNickname)
            ServerRoomGateway.latestRoomSnapshot?.let { renderRoomSnapshot(it) }
        }

        startButton.setOnClickListener {
            if (debugMode) {
                val room = localRoomState ?: return@setOnClickListener
                val me = room.members.firstOrNull { it.nickname == myNickname } ?: return@setOnClickListener
                val canStart = room.members.size in 2..4 && room.members.all { it.isReady } && me.isHost
                if (!canStart) return@setOnClickListener
                updateStartButtonForLocal(room, isStarting = true)
                pendingStartRunnable?.let { runnable -> handler.removeCallbacks(runnable) }
                pendingStartRunnable = Runnable {
                    moveToBoard(room.members.map { it.nickname }.take(4))
                }.also { runnable -> handler.postDelayed(runnable, 2000L) }
            } else {
                val room = ServerRoomGateway.latestRoomSnapshot ?: return@setOnClickListener
                val me = room.players.firstOrNull { it.id == ServerRoomGateway.currentPlayerId } ?: return@setOnClickListener
                val allReady = room.players.all { it.isReady }
                val canStart = room.players.size in 2..4 && allReady && me.isHost
                if (!canStart) return@setOnClickListener
                updateStartButtonState(isStarting = true, room = room)

                pendingStartRunnable?.let { runnable -> handler.removeCallbacks(runnable) }
                pendingStartRunnable = Runnable {
                    ServerRoomGateway.startGame()
                }.also { runnable -> handler.postDelayed(runnable, 2000L) }
            }
        }

        leaveButton.setOnClickListener { leaveLobbyAndFinish() }
    }

    override fun onBackPressed() {
        leaveLobbyAndFinish()
    }

    override fun onDestroy() {
        ServerRoomGateway.removeListener(gatewayListener)
        pendingStartRunnable?.let { handler.removeCallbacks(it) }
        pendingStartRunnable = null
        super.onDestroy()
    }

    private fun ensureLocalJoin() {
        var room = RoomLocalStore.findRoom(roomCode)
        if (room == null) {
            Toast.makeText(this, getString(R.string.lobby_list_code_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        room = RoomLocalStore.joinRoom(roomCode, myNickname) ?: room
        localRoomState = room
    }

    private fun renderLocalRoom(room: RoomState) {
        localRoomState = room
        val slots = room.members
            .map { PlayerSlot(nickname = it.nickname, isHost = it.isHost, isReady = it.isReady) }
            .toMutableList()
        while (slots.size < 4) {
            slots += PlayerSlot.empty()
        }
        renderSlots(slots)
        val readyCount = room.members.count { it.isReady }
        roomMetaText.text = getString(R.string.lobby_room_meta, room.members.size, 4, readyCount)
        updateStartButtonForLocal(room, isStarting = false)
    }

    private fun updateStartButtonForLocal(room: RoomState, isStarting: Boolean) {
        val me = room.members.firstOrNull { it.nickname == myNickname }
        val allReady = room.members.all { it.isReady }
        val canStart = room.members.size in 2..4 && allReady && (me?.isHost == true)

        when {
            isStarting -> {
                startButton.isEnabled = false
                startButton.text = getString(R.string.lobby_start_loading)
                startHintText.text = getString(R.string.lobby_start_hint_loading)
                startButton.setBackgroundResource(R.drawable.bg_action_secondary)
            }
            canStart -> {
                startButton.isEnabled = true
                startButton.text = getString(R.string.lobby_start_button)
                startHintText.text = getString(R.string.lobby_start_hint_ready)
                startButton.setBackgroundResource(R.drawable.bg_action_primary)
            }
            else -> {
                startButton.isEnabled = false
                startButton.text = getString(R.string.lobby_start_button)
                startHintText.text = if (me?.isHost == true) {
                    getString(R.string.lobby_start_hint_need_all_ready)
                } else {
                    getString(R.string.lobby_start_hint_host_only)
                }
                if (room.members.size >= 2) {
                    startButton.setBackgroundResource(R.drawable.bg_action_primary)
                } else {
                    startButton.setBackgroundResource(R.drawable.bg_action_secondary)
                }
            }
        }
    }

    private fun renderRoomSnapshot(room: RoomSnapshot) {
        val slots = room.players
            .map { PlayerSlot(nickname = it.nickname, isHost = it.isHost, isReady = it.isReady) }
            .toMutableList()
        while (slots.size < 4) {
            slots += PlayerSlot.empty()
        }
        renderSlots(slots)
        updateRoomMeta(room)
        updateStartButtonState(isStarting = false, room = room)
    }

    private fun updateRoomMeta(room: RoomSnapshot) {
        val enteredCount = room.players.size
        val readyCount = room.players.count { it.isReady }
        roomMetaText.text = getString(R.string.lobby_room_meta, enteredCount, 4, readyCount)
    }

    private fun updateStartButtonState(isStarting: Boolean, room: RoomSnapshot) {
        val me = room.players.firstOrNull { it.id == ServerRoomGateway.currentPlayerId }
        val allReady = room.players.all { it.isReady }
        val canStart = room.players.size in 2..4 && allReady && (me?.isHost == true)

        when {
            isStarting -> {
                startButton.isEnabled = false
                startButton.text = getString(R.string.lobby_start_loading)
                startHintText.text = getString(R.string.lobby_start_hint_loading)
                startButton.setBackgroundResource(R.drawable.bg_action_secondary)
            }
            canStart -> {
                startButton.isEnabled = true
                startButton.text = getString(R.string.lobby_start_button)
                startHintText.text = getString(R.string.lobby_start_hint_ready)
                startButton.setBackgroundResource(R.drawable.bg_action_primary)
            }
            else -> {
                startButton.isEnabled = false
                startButton.text = getString(R.string.lobby_start_button)
                startHintText.text = if (me?.isHost == true) {
                    getString(R.string.lobby_start_hint_need_all_ready)
                } else {
                    getString(R.string.lobby_start_hint_host_only)
                }
                if (room.players.size >= 2) {
                    startButton.setBackgroundResource(R.drawable.bg_action_primary)
                } else {
                    startButton.setBackgroundResource(R.drawable.bg_action_secondary)
                }
            }
        }
    }

    private fun toggleMyReady() {
        if (debugMode) {
            val updated = RoomLocalStore.toggleReady(roomCode, myNickname) ?: localRoomState ?: return
            renderLocalRoom(updated)
            return
        }
        val room = ServerRoomGateway.latestRoomSnapshot ?: return
        val me = room.players.firstOrNull { it.id == ServerRoomGateway.currentPlayerId } ?: return
        ServerRoomGateway.setReady(!me.isReady)
    }

    private fun renderSlots(slots: List<PlayerSlot>) {
        slots.forEachIndexed { index, slot ->
            val nameView = slotNameViews[index]
            val stateView = slotStateViews[index]
            val cardView = slotCards[index]

            nameView.text = if (slot.nickname.isBlank()) {
                getString(R.string.lobby_slot_waiting_name)
            } else if (slot.isHost) {
                getString(R.string.lobby_slot_host_format, slot.nickname)
            } else {
                slot.nickname
            }

            val statusTextRes = if (slot.isReady) R.string.lobby_status_ready else R.string.lobby_status_waiting
            val stateBackgroundRes = if (slot.isReady) R.drawable.bg_status_ready else R.drawable.bg_status_waiting
            stateView.text = getString(statusTextRes)
            stateView.setBackgroundResource(stateBackgroundRes)
            if (slot.nickname == myNickname) {
                stateView.setOnClickListener { toggleMyReady() }
            } else {
                stateView.setOnClickListener(null)
            }

            cardView.alpha = 0.80f
            cardView.translationY = 10f
            cardView.animate()
                .alpha(1.00f)
                .translationY(0f)
                .setDuration(220L)
                .start()
        }
    }

    private fun moveToBoard(players: List<String>) {
        isNavigatingToBoard = true
        val intent = Intent(this, BoardActivity::class.java).apply {
            putStringArrayListExtra(BoardActivity.EXTRA_PLAYERS, ArrayList(players))
        }
        startActivity(intent)
        finish()
    }

    private fun leaveLobbyAndFinish() {
        if (isLeavingLobby || isNavigatingToBoard) return
        isLeavingLobby = true
        if (debugMode) {
            RoomLocalStore.leaveRoom(roomCode, myNickname)
        } else {
            ServerRoomGateway.disconnect()
        }
        setResult(RESULT_OK, Intent().putExtra(EXTRA_LEFT_ROOM_CODE, roomCode))
        finish()
    }
}

data class PlayerSlot(
    val nickname: String,
    val isHost: Boolean = false,
    val isReady: Boolean = false
) {
    companion object {
        fun empty(): PlayerSlot = PlayerSlot(nickname = "", isReady = false)
    }
}
