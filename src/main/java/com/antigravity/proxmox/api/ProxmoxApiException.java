package com.antigravity.proxmox.api;

public class ProxmoxApiException extends Exception {
    private final int statusCode;

    public ProxmoxApiException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public ProxmoxApiException(int statusCode, String message) {
        super("HTTP " + statusCode + ": " + message);
        this.statusCode = statusCode;
    }

    public ProxmoxApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
