package com.example.adchaosdemo

import android.content.Context
import java.util.UUID

object SessionPrefs {

    private const val PREFS_NAME = "ad_chaos_demo_prefs"
    private const val KEY_NICKNAME = "key_nickname"
    private const val KEY_VOLUME = "key_volume"
    private const val KEY_SOUND_EFFECTS = "key_sound_effects"
    private const val KEY_VIBRATION = "key_vibration"
    private const val KEY_DICE_FAST = "key_dice_fast"
    private const val KEY_DEBUG_MODE = "key_debug_mode"
    private const val KEY_DEV_CLIENT_TOKEN = "key_dev_client_token"
    private const val DEFAULT_VOLUME = 50

    fun getNickname(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NICKNAME, "")
            .orEmpty()
    }

    fun setNickname(context: Context, nickname: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NICKNAME, nickname)
            .apply()
    }

    fun clearNickname(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_NICKNAME)
            .apply()
    }

    fun getVolume(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_VOLUME, DEFAULT_VOLUME)
            .coerceIn(0, 100)
    }

    fun setVolume(context: Context, volume: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_VOLUME, volume.coerceIn(0, 100))
            .apply()
    }

    fun isSoundEffectsOn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_EFFECTS, true)
    }

    fun setSoundEffectsOn(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SOUND_EFFECTS, on)
            .apply()
    }

    fun isVibrationOn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VIBRATION, true)
    }

    fun setVibrationOn(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VIBRATION, on)
            .apply()
    }

    fun isDiceFast(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DICE_FAST, false)
    }

    fun setDiceFast(context: Context, fast: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DICE_FAST, fast)
            .apply()
    }

    fun isDebugMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEBUG_MODE, false)
    }

    fun setDebugMode(context: Context, debugMode: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEBUG_MODE, debugMode)
            .apply()
    }

    fun getOrCreateDevClientToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEV_CLIENT_TOKEN, "").orEmpty()
        if (existing.isNotBlank()) return existing

        val created = "DEV_${UUID.randomUUID()}"
        prefs.edit().putString(KEY_DEV_CLIENT_TOKEN, created).apply()
        return created
    }
}
