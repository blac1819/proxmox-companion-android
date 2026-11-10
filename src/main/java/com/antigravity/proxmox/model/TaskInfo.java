package com.antigravity.proxmox.model;

import org.json.JSONObject;

public class TaskInfo {
    private String upid;
    private String node;
    private String user;
    private String type;
    private String status;
    private long starttime;
    private long endtime;
    private String id;

    public static TaskInfo fromJson(JSONObject obj) {
        TaskInfo t = new TaskInfo();
        t.upid = obj.optString("upid", "");
        t.node = obj.optString("node", "");
        t.user = obj.optString("user", "");
        t.type = obj.optString("type", "");
        t.status = obj.optString("status", "running");
        t.starttime = obj.optLong("starttime", 0L);
        t.endtime = obj.optLong("endtime", 0L);
        t.id = obj.optString("id", "");
        return t;
    }

    public String getUpid() { return upid; }
    public String getNode() { return node; }
    public String getUser() { return user; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public boolean isOk() { return "OK".equalsIgnoreCase(status); }
    public boolean isRunning() { return status == null || status.isEmpty() || "running".equalsIgnoreCase(status); }
    public long getStarttime() { return starttime; }
    public long getEndtime() { return endtime; }
    public String getId() { return id; }

    public String getDescription() {
        if (id != null && !id.isEmpty()) {
            return type + " (" + id + ")";
        }
        return type;
    }
}
