package com.antigravity.proxmox.model;

public class GuestSnapshot {
    private final String name;
    private final String description;
    private final long timestamp;
    private final boolean hasState; // RAM included

    public GuestSnapshot(String name, String description, long timestamp, boolean hasState) {
        this.name = name;
        this.description = description;
        this.timestamp = timestamp;
        this.hasState = hasState;
    }

    public String getName() { return name; }
    public String getDescription() { return description != null ? description : ""; }
    public long getTimestamp() { return timestamp; }
    public boolean hasState() { return hasState; }
}
