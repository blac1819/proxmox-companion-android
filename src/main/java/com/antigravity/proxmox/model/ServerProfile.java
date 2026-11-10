package com.antigravity.proxmox.model;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public class ServerProfile {
    public static final String AUTH_TOKEN = "TOKEN";
    public static final String AUTH_PASSWORD = "PASSWORD";

    private String id;
    private String name;
    private String host;
    private int port;
    private String authMode; // AUTH_TOKEN or AUTH_PASSWORD

    // Token authentication fields
    private String tokenUser;    // e.g. root@pam
    private String tokenId;      // e.g. app
    private String tokenSecret;  // UUID secret

    // Password authentication fields
    private String username;     // e.g. root
    private String realm;        // e.g. pam, pve
    private String password;

    private boolean trustAllSsl;

    public ServerProfile() {
        this.id = UUID.randomUUID().toString();
        this.port = 8006;
        this.authMode = AUTH_TOKEN;
        this.realm = "pam";
        this.trustAllSsl = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name == null || name.isEmpty() ? host : name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host == null ? "" : host.trim(); }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port <= 0 ? 8006 : port; }
    public void setPort(int port) { this.port = port; }

    public String getAuthMode() { return authMode; }
    public void setAuthMode(String authMode) { this.authMode = authMode; }

    public String getTokenUser() { return tokenUser == null ? "" : tokenUser.trim(); }
    public void setTokenUser(String tokenUser) { this.tokenUser = tokenUser; }

    public String getTokenId() { return tokenId == null ? "" : tokenId.trim(); }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getTokenSecret() { return tokenSecret == null ? "" : tokenSecret.trim(); }
    public void setTokenSecret(String tokenSecret) { this.tokenSecret = tokenSecret; }

    public String getUsername() { return username == null ? "" : username.trim(); }
    public void setUsername(String username) { this.username = username; }

    public String getRealm() { return realm == null || realm.isEmpty() ? "pam" : realm.trim(); }
    public void setRealm(String realm) { this.realm = realm; }

    public String getPassword() { return password == null ? "" : password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isTrustAllSsl() { return trustAllSsl; }
    public void setTrustAllSsl(boolean trustAllSsl) { this.trustAllSsl = trustAllSsl; }

    public String getBaseUrl() {
        String cleanHost = getHost();
        if (cleanHost.startsWith("http://") || cleanHost.startsWith("https://")) {
            return cleanHost + ":" + getPort();
        }
        return "https://" + cleanHost + ":" + getPort();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("host", host);
        obj.put("port", port);
        obj.put("authMode", authMode);
        obj.put("tokenUser", tokenUser);
        obj.put("tokenId", tokenId);
        obj.put("tokenSecret", tokenSecret);
        obj.put("username", username);
        obj.put("realm", realm);
        obj.put("password", password);
        obj.put("trustAllSsl", trustAllSsl);
        return obj;
    }

    public static ServerProfile fromJson(JSONObject obj) {
        ServerProfile profile = new ServerProfile();
        profile.setId(obj.optString("id", UUID.randomUUID().toString()));
        profile.setName(obj.optString("name", ""));
        profile.setHost(obj.optString("host", ""));
        profile.setPort(obj.optInt("port", 8006));
        profile.setAuthMode(obj.optString("authMode", AUTH_TOKEN));
        profile.setTokenUser(obj.optString("tokenUser", ""));
        profile.setTokenId(obj.optString("tokenId", ""));
        profile.setTokenSecret(obj.optString("tokenSecret", ""));
        profile.setUsername(obj.optString("username", ""));
        profile.setRealm(obj.optString("realm", "pam"));
        profile.setPassword(obj.optString("password", ""));
        profile.setTrustAllSsl(obj.optBoolean("trustAllSsl", true));
        return profile;
    }

    @Override
    public String toString() {
        return getName() + " (" + getHost() + ")";
    }
}
