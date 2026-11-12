package com.antigravity.proxmox.model;

public class CommunityScript {
    private final String name;
    private final String description;
    private final String category; // LXC, VM, Tools
    private final String command;
    private final String iconUrl;
    private final String documentationUrl;

    public CommunityScript(String name, String description, String category, String command, String iconUrl, String documentationUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.command = command;
        this.iconUrl = iconUrl;
        this.documentationUrl = documentationUrl;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getCommand() { return command; }
    public String getIconUrl() { return iconUrl; }
    public String getDocumentationUrl() { return documentationUrl; }
}
