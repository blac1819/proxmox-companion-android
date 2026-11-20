package com.antigravity.proxmox.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    private static final String PREF_NAME = "proxmox_app_settings";

    public static final String BACKEND_GEMINI = "gemini";
    public static final String BACKEND_LOCAL = "local";

    private static final String KEY_AI_BACKEND = "ai_backend";
    private static final String KEY_GEMINI_KEY = "gemini_api_key";
    private static final String KEY_GEMINI_MODEL = "gemini_model";
    private static final String KEY_LOCAL_AI_URL = "local_ai_url";
    private static final String KEY_POLL_INTERVAL = "poll_interval_sec";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_TIMEOUT_SEC = "conn_timeout_sec";

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getAiBackend() {
        return prefs.getString(KEY_AI_BACKEND, BACKEND_GEMINI);
    }

    public void setAiBackend(String backend) {
        prefs.edit().putString(KEY_AI_BACKEND, backend).apply();
    }

    public String getGeminiApiKey() {
        return prefs.getString(KEY_GEMINI_KEY, "");
    }

    public void setGeminiApiKey(String key) {
        prefs.edit().putString(KEY_GEMINI_KEY, key != null ? key.trim() : "").apply();
    }

    public String getGeminiModel() {
        String m = prefs.getString(KEY_GEMINI_MODEL, "gemini-2.5-flash");
        if ("gemini-1.5-flash".equals(m) || "gemini-1.5-pro".equals(m)) {
            return "gemini-2.5-flash";
        }
        return m;
    }

    public void setGeminiModel(String model) {
        prefs.edit().putString(KEY_GEMINI_MODEL, model).apply();
    }

    public String getLocalAiUrl() {
        return prefs.getString(KEY_LOCAL_AI_URL, "http://127.0.0.1:11434/v1/chat/completions");
    }

    public void setLocalAiUrl(String url) {
        prefs.edit().putString(KEY_LOCAL_AI_URL, url != null ? url.trim() : "").apply();
    }

    public int getPollIntervalSec() {
        return prefs.getInt(KEY_POLL_INTERVAL, 10);
    }

    public void setPollIntervalSec(int sec) {
        prefs.edit().putInt(KEY_POLL_INTERVAL, sec).apply();
    }

    public int getTimeoutSec() {
        return prefs.getInt(KEY_TIMEOUT_SEC, 15);
    }

    public void setTimeoutSec(int sec) {
        prefs.edit().putInt(KEY_TIMEOUT_SEC, sec).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }
}
