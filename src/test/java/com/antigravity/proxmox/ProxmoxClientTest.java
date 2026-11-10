package com.antigravity.proxmox;

import com.antigravity.proxmox.api.ProxmoxClient;
import com.antigravity.proxmox.model.*;
import com.antigravity.proxmox.util.FormatUtils;
import org.json.JSONObject;

import java.util.List;

public class ProxmoxClientTest {

    private static void assertEquals(String desc, Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) {
            System.out.println("  ✓ PASS: " + desc);
        } else {
            System.err.println("  ✗ FAIL: " + desc + " (expected: " + expected + ", got: " + actual + ")");
            System.exit(1);
        }
    }

    private static void assertTrue(String desc, boolean condition) {
        if (condition) {
            System.out.println("  ✓ PASS: " + desc);
        } else {
            System.err.println("  ✗ FAIL: " + desc);
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println(" Running Proxmox Companion Unit Tests");
        System.out.println("========================================");

        // 1. FormatUtils tests
        System.out.println("\n[1] Testing FormatUtils...");
        assertEquals("Format 1024 bytes to KB", "1.0 KB", FormatUtils.formatBytes(1024));
        assertEquals("Format 1073741824 bytes to GB", "1.0 GB", FormatUtils.formatBytes(1073741824L));
        assertEquals("Format percentage 0.155", "15.5%", FormatUtils.formatPercent(0.155));
        assertEquals("Format 90061s uptime", "1d 1h 1m", FormatUtils.formatUptime(90061));

        // 2. ServerProfile serialization tests
        System.out.println("\n[2] Testing ServerProfile JSON...");
        ServerProfile profile = new ServerProfile();
        profile.setName("Test Node");
        profile.setHost("http://127.0.0.1");
        profile.setPort(18006);
        profile.setAuthMode(ServerProfile.AUTH_TOKEN);
        profile.setTokenUser("root@pam");
        profile.setTokenId("app");
        profile.setTokenSecret("11112222-3333-4444-5555-666677778888");
        profile.setTrustAllSsl(true);

        JSONObject json = profile.toJson();
        ServerProfile deserialized = ServerProfile.fromJson(json);
        assertEquals("Profile name match", profile.getName(), deserialized.getName());
        assertEquals("Profile host match", profile.getHost(), deserialized.getHost());
        assertEquals("Profile port match", profile.getPort(), deserialized.getPort());
        assertEquals("Profile authMode match", profile.getAuthMode(), deserialized.getAuthMode());
        assertEquals("Profile tokenSecret match", profile.getTokenSecret(), deserialized.getTokenSecret());

        // 3. API Token Client Integration
        System.out.println("\n[3] Testing ProxmoxClient via API Token against mock server...");
        ProxmoxClient tokenClient = new ProxmoxClient(profile);

        String ver = tokenClient.testConnection();
        assertTrue("testConnection returned version info", ver.contains("PVE 8.2.4"));

        List<NodeInfo> nodes = tokenClient.getNodes();
        assertEquals("Cluster has 1 node", 1, nodes.size());
        NodeInfo node = nodes.get(0);
        assertEquals("Node name is pve-node-01", "pve-node-01", node.getName());
        assertTrue("Node is online", node.isOnline());
        assertEquals("Node CPU percent calculated", 16, node.getCpuPercent());

        NodeInfo nodeDetails = tokenClient.getNodeStatus(node.getName());
        node.copyDetailedFrom(nodeDetails);
        assertEquals("Node kernel version", "Linux 6.8.4-2-pve", node.getKernelVersion());
        assertEquals("Node PVE manager version", "pve-manager/8.2.4/test123", node.getPveVersion());
        assertEquals("Node loadavg1", 0.35, node.getLoadAvg1());

        List<VmInfo> vms = tokenClient.getNodeQemuVms(node.getName());
        assertEquals("VM count is 1", 1, vms.size());
        VmInfo vm = vms.get(0);
        assertEquals("VM ID is 100", 100, vm.getVmid());
        assertEquals("VM name is home-assistant", "home-assistant", vm.getName());
        assertTrue("VM is running", vm.isRunning());
        assertEquals("VM max memory", 4294967296L, vm.getMaxMem());

        List<LxcInfo> lxcs = tokenClient.getNodeLxcContainers(node.getName());
        assertEquals("LXC count is 1", 1, lxcs.size());
        LxcInfo lxc = lxcs.get(0);
        assertEquals("CT ID is 200", 200, lxc.getVmid());
        assertEquals("CT name is nextcloud", "nextcloud", lxc.getName());
        assertTrue("CT is running", lxc.isRunning());

        List<StorageInfo> storages = tokenClient.getNodeStorage(node.getName());
        assertEquals("Storage count is 1", 1, storages.size());
        StorageInfo st = storages.get(0);
        assertEquals("Storage name is local-zfs", "local-zfs", st.getStorage());
        assertEquals("Storage percent used", 25, st.getPercent());

        List<TaskInfo> tasks = tokenClient.getClusterTasks();
        assertEquals("Task count is 1", 1, tasks.size());
        TaskInfo task = tasks.get(0);
        assertTrue("Task status is OK", task.isOk());

        String logOutput = tokenClient.getTaskLog(node.getName(), task.getUpid());
        assertTrue("Task log output contains TASK OK", logOutput.contains("TASK OK"));

        String actionUpid = tokenClient.vmAction(node.getName(), vm.getVmid(), "start");
        assertTrue("VM Action returned UPID", actionUpid.startsWith("UPID:"));

        // 4. Ticket Authentication Client Integration
        System.out.println("\n[4] Testing ProxmoxClient via Username/Password Ticket against mock server...");
        ServerProfile passProfile = new ServerProfile();
        passProfile.setName("Ticket Node");
        passProfile.setHost("http://127.0.0.1");
        passProfile.setPort(18006);
        passProfile.setAuthMode(ServerProfile.AUTH_PASSWORD);
        passProfile.setUsername("root");
        passProfile.setRealm("pam");
        passProfile.setPassword("secretpassword");
        passProfile.setTrustAllSsl(true);

        ProxmoxClient ticketClient = new ProxmoxClient(passProfile);
        ticketClient.login();
        String ticketVer = ticketClient.testConnection();
        assertTrue("Ticket auth testConnection succeeds", ticketVer.contains("PVE 8.2.4"));

        List<NodeInfo> ticketNodes = ticketClient.getNodes();
        assertEquals("Ticket client retrieved nodes", 1, ticketNodes.size());

        System.out.println("\n========================================");
        System.out.println(" ALL UNIT & INTEGRATION TESTS PASSED!");
        System.out.println("========================================");
    }
}
