package com.antigravity.proxmox.storage;

import com.antigravity.proxmox.model.CommunityScript;
import java.util.ArrayList;
import java.util.List;

public class CommunityScriptCatalog {
    private static final List<CommunityScript> SCRIPTS = new ArrayList<>();

    static {
        SCRIPTS.add(new CommunityScript(
                "Home Assistant OS",
                "Home Assistant open-source home automation platform running as a highly optimized QEMU VM.",
                "VM",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/vm/haos-vm.sh)\"",
                "https://community-scripts.org/icons/homeassistant.png",
                "https://www.home-assistant.io"
        ));
        SCRIPTS.add(new CommunityScript(
                "Docker LXC",
                "Pre-configured Debian container with Docker Engine and Docker Compose V2 pre-installed and ready.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/docker.sh)\"",
                "https://community-scripts.org/icons/docker.png",
                "https://docs.docker.com"
        ));
        SCRIPTS.add(new CommunityScript(
                "Pi-hole LXC",
                "Network-wide ad blocking via DNS sinkhole running in a lightweight Alpine/Debian LXC.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/pihole.sh)\"",
                "https://community-scripts.org/icons/pihole.png",
                "https://pi-hole.net"
        ));
        SCRIPTS.add(new CommunityScript(
                "AdGuard Home LXC",
                "Network-wide software for blocking ads and tracking, with parental control and DNS over HTTPS/TLS.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/adguard.sh)\"",
                "https://community-scripts.org/icons/adguard.png",
                "https://adguard.com"
        ));
        SCRIPTS.add(new CommunityScript(
                "Nginx Proxy Manager LXC",
                "Expose web services securely with free Let's Encrypt SSL certificates and an easy-to-use web UI.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/nginxproxymanager.sh)\"",
                "https://community-scripts.org/icons/nginxproxymanager.png",
                "https://nginxproxymanager.com"
        ));
        SCRIPTS.add(new CommunityScript(
                "Plex Media Server LXC",
                "Media server organizing video, music, and photos with hardware transcoding support (Intel QSV / NVENC).",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/plex.sh)\"",
                "https://community-scripts.org/icons/plex.png",
                "https://plex.tv"
        ));
        SCRIPTS.add(new CommunityScript(
                "Jellyfin LXC",
                "100% Free and open-source media system without tracking, subscriptions, or paywalls.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/jellyfin.sh)\"",
                "https://community-scripts.org/icons/jellyfin.png",
                "https://jellyfin.org"
        ));
        SCRIPTS.add(new CommunityScript(
                "Tailscale LXC",
                "Mesh VPN based on WireGuard making cross-device connectivity and homelab access seamless without port-forwarding.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/tailscale.sh)\"",
                "https://community-scripts.org/icons/tailscale.png",
                "https://tailscale.com"
        ));
        SCRIPTS.add(new CommunityScript(
                "Nextcloud LXC",
                "Self-hosted productivity platform providing private cloud file sync, calendar, contacts, and docs.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/nextcloud.sh)\"",
                "https://community-scripts.org/icons/nextcloud.png",
                "https://nextcloud.com"
        ));
        SCRIPTS.add(new CommunityScript(
                "Vaultwarden LXC",
                "Lightweight Bitwarden-compatible password manager backend written in Rust.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/vaultwarden.sh)\"",
                "https://community-scripts.org/icons/vaultwarden.png",
                "https://github.com/dani-garcia/vaultwarden"
        ));
        SCRIPTS.add(new CommunityScript(
                "Proxmox Post-Install Helper",
                "Enables pve-no-subscription repository, disables enterprise nag popup, updates sources, and installs tools.",
                "Tools",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/tools/pve-post-install.sh)\"",
                "",
                "https://community-scripts.org"
        ));
        SCRIPTS.add(new CommunityScript(
                "Proxmox CPU Scaling Governor",
                "Configures energy-saving CPU scaling governor (powersave / conservative / performance) across cores.",
                "Tools",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/tools/scaling-governor.sh)\"",
                "",
                "https://community-scripts.org"
        ));
    }

    public static List<CommunityScript> getScripts() {
        return new ArrayList<>(SCRIPTS);
    }

    public static List<CommunityScript> filter(String query, String category) {
        List<CommunityScript> filtered = new ArrayList<>();
        String q = query != null ? query.toLowerCase().trim() : "";
        for (CommunityScript s : SCRIPTS) {
            boolean matchesCategory = (category == null || category.equalsIgnoreCase("All") || s.getCategory().equalsIgnoreCase(category));
            boolean matchesQuery = q.isEmpty() || s.getName().toLowerCase().contains(q) || s.getDescription().toLowerCase().contains(q);
            if (matchesCategory && matchesQuery) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}
