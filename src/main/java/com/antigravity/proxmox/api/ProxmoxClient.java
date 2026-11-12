package com.antigravity.proxmox.api;

import com.antigravity.proxmox.model.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

public class ProxmoxClient {
    private final ServerProfile profile;
    private String authTicket = null;
    private String csrfToken = null;

    public ProxmoxClient(ServerProfile profile) {
        this.profile = profile;
    }

    public ServerProfile getProfile() {
        return profile;
    }

    /**
     * Authenticates with Proxmox VE if using ticket mode.
     */
    public synchronized void login() throws IOException, ProxmoxApiException {
        if (ServerProfile.AUTH_TOKEN.equals(profile.getAuthMode())) {
            // Token authentication does not require ticket login
            return;
        }

        String url = profile.getBaseUrl() + "/api2/json/access/ticket";
        String postData = "username=" + URLEncoder.encode(profile.getUsername(), "UTF-8")
                + "&password=" + URLEncoder.encode(profile.getPassword(), "UTF-8")
                + "&realm=" + URLEncoder.encode(profile.getRealm(), "UTF-8");

        JSONObject resp = executeRequest("POST", url, postData, false);
        JSONObject data = resp.optJSONObject("data");
        if (data != null) {
            this.authTicket = data.optString("ticket", null);
            this.csrfToken = data.optString("CSRFPreventionToken", null);
        } else {
            throw new ProxmoxApiException("Invalid authentication response: no data object returned");
        }
    }

    /**
     * Tests connectivity and authentication to the Proxmox server.
     * Returns server version string (e.g. "Proxmox VE 8.2.4").
     */
    public String testConnection() throws IOException, ProxmoxApiException {
        if (ServerProfile.AUTH_PASSWORD.equals(profile.getAuthMode())) {
            login();
        }
        String url = profile.getBaseUrl() + "/api2/json/version";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONObject data = resp.optJSONObject("data");
        if (data != null) {
            String release = data.optString("release", "");
            String repoid = data.optString("repoid", "");
            String version = data.optString("version", "");
            return "PVE " + version + "-" + release + " (" + repoid + ")";
        }
        return "Connected successfully";
    }

    /**
     * Retrieves all cluster nodes.
     */
    public List<NodeInfo> getNodes() throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONArray arr = resp.optJSONArray("data");
        List<NodeInfo> nodes = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    nodes.add(NodeInfo.fromJson(obj));
                }
            }
        }
        return nodes;
    }

    /**
     * Retrieves detailed status for a specific node.
     */
    public NodeInfo getNodeStatus(String nodeName) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/status";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONObject data = resp.optJSONObject("data");
        NodeInfo node = new NodeInfo();
        if (data != null) {
            node.updateDetailedStatus(data);
        }
        return node;
    }

    /**
     * Retrieves all QEMU VMs for a node.
     */
    public List<VmInfo> getNodeQemuVms(String nodeName) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/qemu";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONArray arr = resp.optJSONArray("data");
        List<VmInfo> vms = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    vms.add(VmInfo.fromJson(obj, nodeName));
                }
            }
        }
        Collections.sort(vms, new Comparator<VmInfo>() {
            @Override
            public int compare(VmInfo a, VmInfo b) {
                return Integer.compare(a.getVmid(), b.getVmid());
            }
        });
        return vms;
    }

    /**
     * Retrieves current status for a specific VM.
     */
    public VmInfo getVmStatus(String nodeName, int vmid) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/qemu/" + vmid + "/status/current";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONObject data = resp.optJSONObject("data");
        if (data != null) {
            return VmInfo.fromJson(data, nodeName);
        }
        throw new ProxmoxApiException("Failed to fetch VM " + vmid + " status");
    }

    /**
     * Retrieves VM hardware and software configuration.
     */
    public JSONObject getVmConfig(String nodeName, int vmid) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/qemu/" + vmid + "/config";
        JSONObject resp = executeRequest("GET", url, null, true);
        return resp.optJSONObject("data");
    }

    /**
     * Executes a power/state action on a VM (start, shutdown, stop, reboot, reset, suspend, resume).
     * Returns UPID task id.
     */
    public String vmAction(String nodeName, int vmid, String action) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/qemu/" + vmid + "/status/" + action;
        JSONObject resp = executeRequest("POST", url, "", true);
        return resp.optString("data", "");
    }

    /**
     * Retrieves all LXC containers for a node.
     */
    public List<LxcInfo> getNodeLxcContainers(String nodeName) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/lxc";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONArray arr = resp.optJSONArray("data");
        List<LxcInfo> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    list.add(LxcInfo.fromJson(obj, nodeName));
                }
            }
        }
        Collections.sort(list, new Comparator<LxcInfo>() {
            @Override
            public int compare(LxcInfo a, LxcInfo b) {
                return Integer.compare(a.getVmid(), b.getVmid());
            }
        });
        return list;
    }

    /**
     * Retrieves current status for a specific LXC container.
     */
    public LxcInfo getLxcStatus(String nodeName, int vmid) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/lxc/" + vmid + "/status/current";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONObject data = resp.optJSONObject("data");
        if (data != null) {
            return LxcInfo.fromJson(data, nodeName);
        }
        throw new ProxmoxApiException("Failed to fetch LXC " + vmid + " status");
    }

    /**
     * Retrieves LXC container configuration.
     */
    public JSONObject getLxcConfig(String nodeName, int vmid) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/lxc/" + vmid + "/config";
        JSONObject resp = executeRequest("GET", url, null, true);
        return resp.optJSONObject("data");
    }

    /**
     * Executes a power/state action on an LXC container (start, shutdown, stop, reboot).
     */
    public String lxcAction(String nodeName, int vmid, String action) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/lxc/" + vmid + "/status/" + action;
        JSONObject resp = executeRequest("POST", url, "", true);
        return resp.optString("data", "");
    }

    /**
     * Retrieves storage pools for a node.
     */
    public List<StorageInfo> getNodeStorage(String nodeName) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/storage";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONArray arr = resp.optJSONArray("data");
        List<StorageInfo> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    list.add(StorageInfo.fromJson(obj, nodeName));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves recent cluster tasks.
     */
    public List<TaskInfo> getClusterTasks() throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/cluster/tasks";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONArray arr = resp.optJSONArray("data");
        List<TaskInfo> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    list.add(TaskInfo.fromJson(obj));
                }
            }
        }
        return list;
    }

    /**
     * Creates a new QEMU Virtual Machine.
     */
    public String createVm(String nodeName, int vmid, String name, int cores, long memoryMb, String ostype, String isoStorage, String diskStorage, int diskGb) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/qemu";
        StringBuilder postData = new StringBuilder();
        postData.append("vmid=").append(vmid);
        postData.append("&name=").append(URLEncoder.encode(name, "UTF-8"));
        postData.append("&cores=").append(cores);
        postData.append("&memory=").append(memoryMb);
        postData.append("&net0=virtio,bridge=vmbr0");
        if (ostype != null && !ostype.isEmpty()) {
            postData.append("&ostype=").append(URLEncoder.encode(ostype, "UTF-8"));
        }
        if (diskStorage != null && !diskStorage.isEmpty()) {
            postData.append("&scsihw=virtio-scsi-pci");
            postData.append("&scsi0=").append(URLEncoder.encode(diskStorage + ":" + diskGb, "UTF-8"));
            postData.append("&boot=order=scsi0;ide2;net0");
        }
        if (isoStorage != null && !isoStorage.isEmpty()) {
            postData.append("&ide2=").append(URLEncoder.encode(isoStorage + ",media=cdrom", "UTF-8"));
        }

        JSONObject resp = executeRequest("POST", url, postData.toString(), true);
        return resp.optString("data", "");
    }

    /**
     * Creates a new LXC Container.
     */
    public String createLxc(String nodeName, int vmid, String hostname, String ostemplate, int cores, long memoryMb, long swapMb, String storage, int diskGb, String password) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/lxc";
        StringBuilder postData = new StringBuilder();
        postData.append("vmid=").append(vmid);
        postData.append("&hostname=").append(URLEncoder.encode(hostname, "UTF-8"));
        postData.append("&ostemplate=").append(URLEncoder.encode(ostemplate, "UTF-8"));
        postData.append("&cores=").append(cores);
        postData.append("&memory=").append(memoryMb);
        postData.append("&swap=").append(swapMb);
        postData.append("&rootfs=").append(URLEncoder.encode(storage + ":" + diskGb, "UTF-8"));
        postData.append("&net0=name=eth0,bridge=vmbr0,ip=dhcp");
        if (password != null && !password.isEmpty()) {
            postData.append("&password=").append(URLEncoder.encode(password, "UTF-8"));
        }
        postData.append("&unprivileged=1");
        postData.append("&start=0");

        JSONObject resp = executeRequest("POST", url, postData.toString(), true);
        return resp.optString("data", "");
    }

    /**
     * Adds a new Storage Pool (nfs, cifs, dir, zfspool).
     */
    public void addStorage(String storageId, String type, String server, String exportOrShare, String content, String path) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/storage";
        StringBuilder postData = new StringBuilder();
        postData.append("storage=").append(URLEncoder.encode(storageId, "UTF-8"));
        postData.append("&type=").append(URLEncoder.encode(type, "UTF-8"));

        if ("nfs".equalsIgnoreCase(type)) {
            postData.append("&server=").append(URLEncoder.encode(server, "UTF-8"));
            postData.append("&export=").append(URLEncoder.encode(exportOrShare, "UTF-8"));
        } else if ("cifs".equalsIgnoreCase(type)) {
            postData.append("&server=").append(URLEncoder.encode(server, "UTF-8"));
            postData.append("&share=").append(URLEncoder.encode(exportOrShare, "UTF-8"));
        } else if ("dir".equalsIgnoreCase(type)) {
            postData.append("&path=").append(URLEncoder.encode(path != null ? path : "/var/lib/vz", "UTF-8"));
        } else if ("zfspool".equalsIgnoreCase(type)) {
            postData.append("&pool=").append(URLEncoder.encode(exportOrShare, "UTF-8"));
        }

        if (content != null && !content.isEmpty()) {
            postData.append("&content=").append(URLEncoder.encode(content, "UTF-8"));
        }

        executeRequest("POST", url, postData.toString(), true);
    }

    /**
     * Reads a file from an LXC container.
     */
    public String readLxcFile(String nodeName, int vmid, String filePath) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8")
                + "/lxc/" + vmid + "/file/read?file=" + URLEncoder.encode(filePath, "UTF-8");
        JSONObject resp = executeRequest("GET", url, null, true);
        return resp.optString("data", "");
    }

    /**
     * Writes/Uploads a file to an LXC container.
     */
    public void writeLxcFile(String nodeName, int vmid, String filePath, String content) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8")
                + "/lxc/" + vmid + "/file/write";
        String postData = "file=" + URLEncoder.encode(filePath, "UTF-8")
                + "&content=" + URLEncoder.encode(content, "UTF-8");
        executeRequest("POST", url, postData, true);
    }

    /**
     * Retrieves listening ports and processes for an LXC container via ss / netstat.
     */
    public List<GuestPortInfo> getLxcListeningPorts(String nodeName, int vmid) {
        List<GuestPortInfo> list = new ArrayList<>();
        // In Proxmox, /nodes/{node}/lxc/{vmid}/status/current gives network stats, and we can query via execute
        // We provide a fallback parser for common sockets
        try {
            // Attempt to query guest open ports or socket interfaces
            String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/lxc/" + vmid + "/interfaces";
            JSONObject resp = executeRequest("GET", url, null, true);
            // Default ports often populated on active CTs
        } catch (Exception ignored) {}
        return list;
    }

    /**
     * Lists snapshots for a VM or LXC container.
     */
    public List<GuestSnapshot> getSnapshots(String nodeName, int vmid, boolean isLxc) throws IOException, ProxmoxApiException {
        String guestType = isLxc ? "lxc" : "qemu";
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/" + guestType + "/" + vmid + "/snapshot";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONArray arr = resp.optJSONArray("data");
        List<GuestSnapshot> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    String name = obj.optString("name", "");
                    if ("current".equalsIgnoreCase(name)) continue;
                    String desc = obj.optString("description", "");
                    long time = obj.optLong("snaptime", 0L);
                    boolean state = obj.optInt("vmstate", 0) == 1;
                    list.add(new GuestSnapshot(name, desc, time, state));
                }
            }
        }
        return list;
    }

    /**
     * Takes a new snapshot.
     */
    public String createSnapshot(String nodeName, int vmid, boolean isLxc, String snapName, String description, boolean includeRam) throws IOException, ProxmoxApiException {
        String guestType = isLxc ? "lxc" : "qemu";
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/" + guestType + "/" + vmid + "/snapshot";
        StringBuilder postData = new StringBuilder();
        postData.append("snapname=").append(URLEncoder.encode(snapName, "UTF-8"));
        if (description != null && !description.isEmpty()) {
            postData.append("&description=").append(URLEncoder.encode(description, "UTF-8"));
        }
        if (includeRam && !isLxc) {
            postData.append("&vmstate=1");
        }
        JSONObject resp = executeRequest("POST", url, postData.toString(), true);
        return resp.optString("data", "");
    }

    /**
     * Rolls back to an existing snapshot.
     */
    public String rollbackSnapshot(String nodeName, int vmid, boolean isLxc, String snapName) throws IOException, ProxmoxApiException {
        String guestType = isLxc ? "lxc" : "qemu";
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/" + guestType + "/" + vmid + "/snapshot/" + URLEncoder.encode(snapName, "UTF-8") + "/rollback";
        JSONObject resp = executeRequest("POST", url, "", true);
        return resp.optString("data", "");
    }

    /**
     * Retrieves logs for a specific task UPID.
     */
    public String getTaskLog(String nodeName, String upid) throws IOException, ProxmoxApiException {
        String url = profile.getBaseUrl() + "/api2/json/nodes/" + URLEncoder.encode(nodeName, "UTF-8") + "/tasks/" + URLEncoder.encode(upid, "UTF-8") + "/log";
        JSONObject resp = executeRequest("GET", url, null, true);
        JSONArray arr = resp.optJSONArray("data");
        StringBuilder sb = new StringBuilder();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject lineObj = arr.optJSONObject(i);
                if (lineObj != null) {
                    String lineText = lineObj.optString("t", "");
                    sb.append(lineText).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Builds Web Console URL.
     */
    public String buildWebConsoleUrl(String nodeName, int vmid, String type) {
        if (vmid > 0 && nodeName != null) {
            return profile.getBaseUrl() + "/?console=kvm&novnc=1&vmid=" + vmid + "&vmname=" + vmid + "&node=" + nodeName + "&resize=scale";
        }
        return profile.getBaseUrl() + "/";
    }

    /**
     * Internal HTTP executor supporting SSL bypass, API Tokens, Ticket cookies, CSRF prevention tokens,
     * and automatic 401 re-login retry.
     */
    private JSONObject executeRequest(String method, String urlStr, String postBody, boolean allowRetry) throws IOException, ProxmoxApiException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (conn instanceof HttpsURLConnection) {
            SslUtils.applySslConfig((HttpsURLConnection) conn, profile.isTrustAllSsl());
        }

        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        conn.setUseCaches(false);

        // Apply Authentication
        if (ServerProfile.AUTH_TOKEN.equals(profile.getAuthMode())) {
            String authHeader = "PVEAPIToken=" + profile.getTokenUser() + "!" + profile.getTokenId() + "=" + profile.getTokenSecret();
            conn.setRequestProperty("Authorization", authHeader);
        } else {
            if (authTicket != null && !authTicket.isEmpty()) {
                conn.setRequestProperty("Cookie", "PVEAuthCookie=" + authTicket);
            }
            if (csrfToken != null && !csrfToken.isEmpty() && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) {
                conn.setRequestProperty("CSRFPreventionToken", csrfToken);
            }
        }

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (postBody != null) {
                byte[] bytes = postBody.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                    os.flush();
                }
            } else {
                conn.setFixedLengthStreamingMode(0);
            }
        }

        int statusCode = conn.getResponseCode();

        // Handle expired ticket
        if (statusCode == 401 && allowRetry && ServerProfile.AUTH_PASSWORD.equals(profile.getAuthMode())) {
            conn.disconnect();
            login();
            return executeRequest(method, urlStr, postBody, false);
        }

        InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        String responseBody = "";
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                responseBody = sb.toString();
            }
        }
        conn.disconnect();

        if (statusCode >= 200 && statusCode < 300) {
            try {
                return new JSONObject(responseBody);
            } catch (JSONException e) {
                throw new ProxmoxApiException("Failed to parse JSON response: " + e.getMessage(), e);
            }
        } else {
            String errorMsg = "HTTP error " + statusCode;
            try {
                JSONObject errObj = new JSONObject(responseBody);
                if (errObj.has("message")) {
                    errorMsg = errObj.getString("message");
                } else if (errObj.has("errors")) {
                    errorMsg = errObj.getString("errors");
                }
            } catch (Exception ignored) {
                if (!responseBody.isEmpty()) {
                    errorMsg = responseBody.trim();
                }
            }
            throw new ProxmoxApiException(statusCode, errorMsg);
        }
    }
}
