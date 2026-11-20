package com.antigravity.proxmox.storage;

import android.content.Context;
import com.antigravity.proxmox.model.CommunityScript;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommunityScriptCatalog {
    private static final List<CommunityScript> SCRIPTS = new ArrayList<>();
    private static boolean isLoaded = false;

    /**
     * Initializes the full catalog from the bundled 480+ community scripts JSON asset.
     */
    public static synchronized void ensureLoaded(Context context) {
        if (isLoaded && !SCRIPTS.isEmpty()) return;

        try {
            InputStream is = context.getAssets().open("community_scripts.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray array = new JSONArray(sb.toString());
            SCRIPTS.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.optString("name", "");
                String desc = obj.optString("description", "");
                String category = obj.optString("category", "LXC");
                String command = obj.optString("command", "");
                String iconUrl = obj.optString("iconUrl", "");
                String docsUrl = obj.optString("documentationUrl", "");

                if (!name.isEmpty() && !command.isEmpty()) {
                    SCRIPTS.add(new CommunityScript(name, desc, category, command, iconUrl, docsUrl));
                }
            }
            isLoaded = true;
        } catch (Exception e) {
            // Fallback to essential built-in scripts if asset read fails
            if (SCRIPTS.isEmpty()) {
                loadFallbackScripts();
            }
            isLoaded = true;
        }

        Collections.sort(SCRIPTS, new Comparator<CommunityScript>() {
            @Override
            public int compare(CommunityScript a, CommunityScript b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
    }

    private static void loadFallbackScripts() {
        SCRIPTS.add(new CommunityScript(
                "Home Assistant OS",
                "Home Assistant open-source home automation platform running as a highly optimized QEMU VM.",
                "VM",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/vm/haos-vm.sh)\"",
                "",
                "https://www.home-assistant.io"
        ));
        SCRIPTS.add(new CommunityScript(
                "Docker LXC",
                "Pre-configured Debian container with Docker Engine and Docker Compose V2 pre-installed and ready.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/docker.sh)\"",
                "",
                "https://docs.docker.com"
        ));
        SCRIPTS.add(new CommunityScript(
                "Pi-hole LXC",
                "Network-wide ad blocking via DNS sinkhole running in a lightweight Alpine/Debian LXC.",
                "LXC",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/ct/pihole.sh)\"",
                "",
                "https://pi-hole.net"
        ));
        SCRIPTS.add(new CommunityScript(
                "Proxmox Post-Install Helper",
                "Enables pve-no-subscription repository, disables enterprise nag popup, updates sources, and installs tools.",
                "Tools",
                "bash -c \"$(wget -qLO - https://github.com/community-scripts/ProxmoxVE/raw/main/tools/pve/post-pve-install.sh)\"",
                "",
                "https://community-scripts.github.io/ProxmoxVE/"
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
            boolean matchesQuery = q.isEmpty()
                    || s.getName().toLowerCase().contains(q)
                    || s.getDescription().toLowerCase().contains(q);
            if (matchesCategory && matchesQuery) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}
