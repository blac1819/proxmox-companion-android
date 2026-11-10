package com.antigravity.proxmox.model;

import org.json.JSONObject;

public class LxcInfo {
    private int vmid;
    private String name;
    private String node;
    private String status; // "running", "stopped"
    private double cpu;
    private int cpus;
    private int maxCpu;
    private long mem;
    private long maxMem;
    private long swap;
    private long maxSwap;
    private long disk;
    private long maxDisk;
    private long uptime;

    public static LxcInfo fromJson(JSONObject obj, String nodeName) {
        LxcInfo lxc = new LxcInfo();
        lxc.vmid = obj.optInt("vmid", 0);
        lxc.name = obj.optString("name", "CT " + lxc.vmid);
        lxc.node = nodeName;
        lxc.status = obj.optString("status", "stopped");
        lxc.cpu = obj.optDouble("cpu", 0.0);
        lxc.cpus = obj.optInt("cpus", obj.optInt("maxcpu", 1));
        lxc.maxCpu = obj.optInt("maxcpu", 1);
        lxc.mem = obj.optLong("mem", 0L);
        lxc.maxMem = obj.optLong("maxmem", 0L);
        lxc.swap = obj.optLong("swap", 0L);
        lxc.maxSwap = obj.optLong("maxswap", 0L);
        lxc.disk = obj.optLong("disk", 0L);
        lxc.maxDisk = obj.optLong("maxdisk", 0L);
        lxc.uptime = obj.optLong("uptime", 0L);
        return lxc;
    }

    public int getVmid() { return vmid; }
    public String getName() { return name; }
    public String getNode() { return node; }
    public String getStatus() { return status; }
    public boolean isRunning() { return "running".equalsIgnoreCase(status); }
    public double getCpu() { return cpu; }
    public int getCpus() { return cpus; }
    public int getMaxCpu() { return maxCpu; }
    public long getMem() { return mem; }
    public long getMaxMem() { return maxMem; }
    public long getSwap() { return swap; }
    public long getMaxSwap() { return maxSwap; }
    public long getDisk() { return disk; }
    public long getMaxDisk() { return maxDisk; }
    public long getUptime() { return uptime; }

    public int getCpuPercent() {
        return (int) Math.min(100, Math.max(0, Math.round(cpu * 100.0)));
    }

    public int getMemPercent() {
        if (maxMem <= 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round((mem * 100.0) / maxMem)));
    }

    public int getSwapPercent() {
        if (maxSwap <= 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round((swap * 100.0) / maxSwap)));
    }

    public int getDiskPercent() {
        if (maxDisk <= 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round((disk * 100.0) / maxDisk)));
    }
}
