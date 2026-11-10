package com.antigravity.proxmox.model;

import org.json.JSONObject;

public class VmInfo {
    private int vmid;
    private String name;
    private String node;
    private String status; // "running", "stopped", "paused"
    private double cpu;
    private int cpus;
    private int maxCpu;
    private long mem;
    private long maxMem;
    private long disk;
    private long maxDisk;
    private long netIn;
    private long netOut;
    private long diskRead;
    private long diskWrite;
    private long uptime;
    private boolean template;

    public static VmInfo fromJson(JSONObject obj, String nodeName) {
        VmInfo vm = new VmInfo();
        vm.vmid = obj.optInt("vmid", 0);
        vm.name = obj.optString("name", "VM " + vm.vmid);
        vm.node = nodeName;
        vm.status = obj.optString("status", "stopped");
        vm.cpu = obj.optDouble("cpu", 0.0);
        vm.cpus = obj.optInt("cpus", obj.optInt("maxcpu", 1));
        vm.maxCpu = obj.optInt("maxcpu", 1);
        vm.mem = obj.optLong("mem", 0L);
        vm.maxMem = obj.optLong("maxmem", 0L);
        vm.disk = obj.optLong("disk", 0L);
        vm.maxDisk = obj.optLong("maxdisk", 0L);
        vm.netIn = obj.optLong("netin", 0L);
        vm.netOut = obj.optLong("netout", 0L);
        vm.diskRead = obj.optLong("diskread", 0L);
        vm.diskWrite = obj.optLong("diskwrite", 0L);
        vm.uptime = obj.optLong("uptime", 0L);
        vm.template = obj.optInt("template", 0) == 1;
        return vm;
    }

    public int getVmid() { return vmid; }
    public String getName() { return name; }
    public String getNode() { return node; }
    public String getStatus() { return status; }
    public boolean isRunning() { return "running".equalsIgnoreCase(status); }
    public boolean isPaused() { return "paused".equalsIgnoreCase(status); }
    public boolean isStopped() { return "stopped".equalsIgnoreCase(status); }
    public double getCpu() { return cpu; }
    public int getCpus() { return cpus; }
    public int getMaxCpu() { return maxCpu; }
    public long getMem() { return mem; }
    public long getMaxMem() { return maxMem; }
    public long getDisk() { return disk; }
    public long getMaxDisk() { return maxDisk; }
    public long getNetIn() { return netIn; }
    public long getNetOut() { return netOut; }
    public long getDiskRead() { return diskRead; }
    public long getDiskWrite() { return diskWrite; }
    public long getUptime() { return uptime; }
    public boolean isTemplate() { return template; }

    public int getCpuPercent() {
        return (int) Math.min(100, Math.max(0, Math.round(cpu * 100.0)));
    }

    public int getMemPercent() {
        if (maxMem <= 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round((mem * 100.0) / maxMem)));
    }
}
