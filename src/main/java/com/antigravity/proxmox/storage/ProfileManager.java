package com.antigravity.proxmox.storage;

import android.content.Context;
import android.content.SharedPreferences;
import com.antigravity.proxmox.model.ServerProfile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {
    private static final String PREF_NAME = "proxmox_profiles";
    private static final String KEY_SERVERS = "servers_json";
    private static final String KEY_ACTIVE_ID = "active_server_id";

    private final SharedPreferences prefs;

    public ProfileManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<ServerProfile> getProfiles() {
        List<ServerProfile> list = new ArrayList<>();
        String jsonStr = prefs.getString(KEY_SERVERS, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(ServerProfile.fromJson(obj));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void saveProfile(ServerProfile profile) {
        List<ServerProfile> profiles = getProfiles();
        boolean found = false;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(profile.getId())) {
                profiles.set(i, profile);
                found = true;
                break;
            }
        }
        if (!found) {
            profiles.add(profile);
        }
        persistProfiles(profiles);

        // If no active profile, make this the active one
        if (getActiveProfileId() == null || getActiveProfileId().isEmpty()) {
            setActiveProfileId(profile.getId());
        }
    }

    public synchronized void deleteProfile(String id) {
        List<ServerProfile> profiles = getProfiles();
        List<ServerProfile> updated = new ArrayList<>();
        for (ServerProfile p : profiles) {
            if (!p.getId().equals(id)) {
                updated.add(p);
            }
        }
        persistProfiles(updated);

        if (id.equals(getActiveProfileId())) {
            if (!updated.isEmpty()) {
                setActiveProfileId(updated.get(0).getId());
            } else {
                setActiveProfileId("");
            }
        }
    }

    public synchronized ServerProfile getProfileById(String id) {
        if (id == null) return null;
        for (ServerProfile p : getProfiles()) {
            if (id.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    public synchronized ServerProfile getActiveProfile() {
        String activeId = getActiveProfileId();
        if (activeId != null && !activeId.isEmpty()) {
            ServerProfile profile = getProfileById(activeId);
            if (profile != null) return profile;
        }
        List<ServerProfile> list = getProfiles();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public synchronized String getActiveProfileId() {
        return prefs.getString(KEY_ACTIVE_ID, "");
    }

    public synchronized void setActiveProfileId(String id) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply();
    }

    private void persistProfiles(List<ServerProfile> profiles) {
        JSONArray arr = new JSONArray();
        for (ServerProfile p : profiles) {
            try {
                arr.put(p.toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_SERVERS, arr.toString()).apply();
    }
}
