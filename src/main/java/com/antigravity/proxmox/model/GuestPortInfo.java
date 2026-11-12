package com.antigravity.proxmox.model;

public class GuestPortInfo {
    private final String protocol; // TCP, UDP
    private final String localAddress;
    private final int port;
    private final String processName;
    private final int pid;

    public GuestPortInfo(String protocol, String localAddress, int port, String processName, int pid) {
        this.protocol = protocol;
        this.localAddress = localAddress;
        this.port = port;
        this.processName = processName;
        this.pid = pid;
    }

    public String getProtocol() { return protocol; }
    public String getLocalAddress() { return localAddress; }
    public int getPort() { return port; }
    public String getProcessName() { return processName != null && !processName.isEmpty() ? processName : "-"; }
    public int getPid() { return pid; }

    public String getFormattedEndpoint() {
        return localAddress + ":" + port;
    }
}
