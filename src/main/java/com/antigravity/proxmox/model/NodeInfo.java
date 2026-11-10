package com.antigravity.proxmox.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class NodeInfo {
    private String name;
    private String status; // "online" or "offline"
    private double cpu;
    private int maxCpu;
    private long mem;
    private long maxMem;
    private long disk;
    private long maxDisk;
    private long uptime;
    private String pveVersion;
    private String kernelVersion;
    private double loadAvg1;
    private double loadAvg5;
    private double loadAvg15;

    public static NodeInfo fromJson(JSONObject obj) {
        NodeInfo node = new NodeInfo();
        node.name = obj.optString("node", "unknown");
        node.status = obj.optString("status", "unknown");
        node.cpu = obj.optDouble("cpu", 0.0);
        node.maxCpu = obj.optInt("maxcpu", 1);
        node.mem = obj.optLong("mem", 0L);
        node.maxMem = obj.optLong("maxmem", 0L);
        node.disk = obj.optLong("disk", 0L);
        node.maxDisk = obj.optLong("maxdisk", 0L);
        node.uptime = obj.optLong("uptime", 0L);
        return node;
    }

    public void updateDetailedStatus(JSONObject obj) {
        this.pveVersion = obj.optString("pveversion", "");
        this.kernelVersion = obj.optString("kversion", "");
        this.uptime = obj.optLong("uptime", this.uptime);
        this.cpu = obj.optDouble("cpu", this.cpu);

        JSONObject memObj = obj.optJSONObject("memory");
        if (memObj != null) {
            this.maxMem = memObj.optLong("total", this.maxMem);
            this.mem = memObj.optLong("used", this.mem);
        }

        JSONObject rootfsObj = obj.optJSONObject("rootfs");
        if (rootfsObj != null) {
            this.maxDisk = rootfsObj.optLong("total", this.maxDisk);
            this.disk = rootfsObj.optLong("used", this.disk);
        }

        JSONArray loadObj = obj.optJSONArray("loadavg");
        if (loadObj != null && loadObj.length() >= 3) {
            this.loadAvg1 = loadObj.optDouble(0, 0.0);
            this.loadAvg5 = loadObj.optDouble(1, 0.0);
            this.loadAvg15 = loadObj.optDouble(2, 0.0);
        }
    }

    public void copyDetailedFrom(NodeInfo other) {
        if (other == null) return;
        this.pveVersion = other.pveVersion;
        this.kernelVersion = other.kernelVersion;
        this.uptime = other.uptime;
        this.cpu = other.cpu;
        this.maxMem = other.maxMem;
        this.mem = other.mem;
        this.maxDisk = other.maxDisk;
        this.disk = other.disk;
        this.loadAvg1 = other.loadAvg1;
        this.loadAvg5 = other.loadAvg5;
        this.loadAvg15 = other.loadAvg15;
    }

    public String getName() { return name; }
    public String getStatus() { return status; }
    public boolean isOnline() { return "online".equalsIgnoreCase(status); }
    public double getCpu() { return cpu; }
    public int getMaxCpu() { return maxCpu; }
    public long getMem() { return mem; }
    public long getMaxMem() { return maxMem; }
    public long getDisk() { return disk; }
    public long getMaxDisk() { return maxDisk; }
    public long getUptime() { return uptime; }
    public String getPveVersion() { return pveVersion; }
    public String getKernelVersion() { return kernelVersion; }
    public double getLoadAvg1() { return loadAvg1; }
    public double getLoadAvg5() { return loadAvg5; }
    public double getLoadAvg15() { return loadAvg15; }

    public int getCpuPercent() {
        return (int) Math.min(100, Math.max(0, Math.round(cpu * 100.0)));
    }

    public int getMemPercent() {
        if (maxMem <= 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round((mem * 100.0) / maxMem)));
    }

    public int getDiskPercent() {
        if (maxDisk <= 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round((disk * 100.0) / maxDisk)));
    }
}
