package com.example.adchaosdemo

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class OptionsActivity : AppCompatActivity() {

    private lateinit var volumeValueText: TextView
    private lateinit var easyVolumeCheck: CheckBox
    private lateinit var soundEffectsSwitch: Switch
    private lateinit var vibrationSwitch: Switch
    private lateinit var diceSpeedNormal: RadioButton
    private lateinit var diceSpeedFast: RadioButton
    private lateinit var debugModeSwitch: Switch
    private lateinit var serverUrlInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        volumeValueText = findViewById(R.id.volumeValueText)
        easyVolumeCheck = findViewById(R.id.easyVolumeCheck)
        soundEffectsSwitch = findViewById(R.id.soundEffectsSwitch)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        diceSpeedNormal = findViewById(R.id.diceSpeedNormal)
        diceSpeedFast = findViewById(R.id.diceSpeedFast)
        debugModeSwitch = findViewById(R.id.debugModeSwitch)
        serverUrlInput = findViewById(R.id.serverUrlInput)

        val minusButton = findViewById<Button>(R.id.volumeMinusButton)
        val plusButton = findViewById<Button>(R.id.volumePlusButton)
        val saveServerUrlButton = findViewById<Button>(R.id.saveServerUrlButton)
        val closeButton = findViewById<Button>(R.id.closeOptionsButton)

        minusButton.setOnClickListener { updateVolumeBy(-1) }
        plusButton.setOnClickListener { updateVolumeBy(1) }
        closeButton.setOnClickListener { finish() }

        easyVolumeCheck.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                buttonView.isChecked = false
                Toast.makeText(this, R.string.options_easy_volume_locked, Toast.LENGTH_SHORT).show()
            }
        }

        soundEffectsSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            SessionPrefs.setSoundEffectsOn(this, isChecked)
        }
        vibrationSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            SessionPrefs.setVibrationOn(this, isChecked)
        }
        diceSpeedNormal.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (isChecked) SessionPrefs.setDiceFast(this, false)
        }
        diceSpeedFast.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (isChecked) SessionPrefs.setDiceFast(this, true)
        }
        debugModeSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            SessionPrefs.setDebugMode(this, isChecked)
        }
        saveServerUrlButton.setOnClickListener {
            val wsUrl = serverUrlInput.text.toString().trim()
            if (!(wsUrl.startsWith("ws://") || wsUrl.startsWith("wss://"))) {
                serverUrlInput.error = getString(R.string.options_server_url_invalid)
                return@setOnClickListener
            }
            val normalizedUrl = ServerRoomGateway.normalizeServerUrl(wsUrl)
            ServerRoomGateway.setServerUrl(this, normalizedUrl)
            serverUrlInput.setText(normalizedUrl)
            Toast.makeText(this, R.string.options_server_url_saved, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        renderVolume()
        renderExtraOptions()
        renderServerUrl()
        easyVolumeCheck.isChecked = false
    }

    private fun updateVolumeBy(delta: Int) {
        val current = SessionPrefs.getVolume(this)
        SessionPrefs.setVolume(this, current + delta)
        renderVolume()
    }

    private fun renderVolume() {
        val volume = SessionPrefs.getVolume(this)
        volumeValueText.text = getString(R.string.options_volume_value, volume)
    }

    private fun renderExtraOptions() {
        soundEffectsSwitch.isChecked = SessionPrefs.isSoundEffectsOn(this)
        vibrationSwitch.isChecked = SessionPrefs.isVibrationOn(this)
        if (SessionPrefs.isDiceFast(this)) {
            diceSpeedFast.isChecked = true
        } else {
            diceSpeedNormal.isChecked = true
        }
        debugModeSwitch.isChecked = SessionPrefs.isDebugMode(this)
    }

    private fun renderServerUrl() {
        serverUrlInput.setText(ServerRoomGateway.getServerUrl(this))
    }
}
