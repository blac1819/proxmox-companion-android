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
