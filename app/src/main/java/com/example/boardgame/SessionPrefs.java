package com.example.boardgame;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public final class SessionPrefs {
    private static final String PREFS_NAME = "boardgame_client_prefs";
    private static final String KEY_NICKNAME = "key_nickname";
    private static final String KEY_DEV_CLIENT_TOKEN = "key_dev_client_token";
    private static final String KEY_LOGIN_PROVIDER = "key_login_provider";
    private static final String LOGIN_PROVIDER_GUEST = "guest";

    private SessionPrefs() {
    }

    public static String getNickname(Context context) {
        return prefs(context).getString(KEY_NICKNAME, "");
    }

    public static void setGuestLogin(Context context, String nickname) {
        prefs(context).edit()
                .putString(KEY_NICKNAME, nickname == null ? "" : nickname)
                .putString(KEY_LOGIN_PROVIDER, LOGIN_PROVIDER_GUEST)
                .apply();
    }

    public static String getOrCreateDevClientToken(Context context) {
        SharedPreferences sharedPreferences = prefs(context);
        String existing = sharedPreferences.getString(KEY_DEV_CLIENT_TOKEN, "");
        if (existing != null && !existing.trim().isEmpty()) {
            return existing;
        }

        String created = "DEV_" + UUID.randomUUID();
        sharedPreferences.edit().putString(KEY_DEV_CLIENT_TOKEN, created).apply();
        return created;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

}
