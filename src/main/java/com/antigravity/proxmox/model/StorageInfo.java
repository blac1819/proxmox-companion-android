package com.antigravity.proxmox.model;

import org.json.JSONObject;

public class StorageInfo {
    private String storage;
    private String type;
    private String node;
    private boolean active;
    private boolean enabled;
    private long used;
    private long total;
    private long avail;
    private String content;

    public static StorageInfo fromJson(JSONObject obj, String nodeName) {
        StorageInfo s = new StorageInfo();
        s.storage = obj.optString("storage", "unknown");
        s.type = obj.optString("type", "unknown");
        s.node = nodeName;
        s.active = obj.optInt("active", 1) == 1;
        s.enabled = obj.optInt("enabled", 1) == 1;
        s.used = obj.optLong("used", 0L);
        s.total = obj.optLong("total", 0L);
        s.avail = obj.optLong("avail", 0L);
        s.content = obj.optString("content", "");
        return s;
    }

    public String getStorage() { return storage; }
    public String getType() { return type; }
    public String getNode() { return node; }
    public boolean isActive() { return active; }
    public boolean isEnabled() { return enabled; }
    public long getUsed() { return used; }
    public long getTotal() { return total; }
    public long getAvail() { return avail; }
    public String getContent() { return content; }

    public int getPercent() {
        if (total <= 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round((used * 100.0) / total)));
    }
}
