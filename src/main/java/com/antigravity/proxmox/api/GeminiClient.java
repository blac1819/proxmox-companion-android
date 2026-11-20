package com.antigravity.proxmox.api;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiClient {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String model;
    private final String backend; // "gemini" or "local"
    private final String localUrl;

    public GeminiClient(String apiKey, String model) {
        this(apiKey, model, "gemini", "http://127.0.0.1:11434/v1/chat/completions");
    }

    public GeminiClient(String apiKey, String model, String backend, String localUrl) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        String m = (model != null && !model.isEmpty()) ? model.trim() : "gemini-2.5-flash";
        // Migrate legacy deprecated models to current valid Gemini 2.5 models
        if ("gemini-1.5-flash".equals(m) || "gemini-1.5-pro".equals(m)) {
            m = "gemini-2.5-flash";
        }
        this.model = m;
        this.backend = backend != null ? backend : "gemini";
        this.localUrl = (localUrl != null && !localUrl.trim().isEmpty())
                ? localUrl.trim()
                : "http://127.0.0.1:11434/v1/chat/completions";
    }

    public boolean isConfigured() {
        if ("local".equalsIgnoreCase(backend)) {
            return !localUrl.isEmpty();
        }
        return !apiKey.isEmpty();
    }

    /**
     * Prompts the AI (either Google Gemini API or Local LLM) for Proxmox VE troubleshooting & guidance.
     */
    public String askAssistant(String userPrompt, String context) throws Exception {
        if ("local".equalsIgnoreCase(backend)) {
            return askLocalModel(userPrompt, context);
        } else {
            return askGemini(userPrompt, context);
        }
    }

    /**
     * Calls Google Gemini API (gemini-2.5-flash / gemini-2.5-pro).
     */
    private String askGemini(String userPrompt, String context) throws Exception {
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Gemini API Key is not configured. Please add it in App Settings (⚙).");
        }

        String endpoint = GEMINI_API_URL + model + ":generateContent?key=" + apiKey;
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        String systemInstruction = "You are an expert Proxmox VE (Virtual Environment) systems administrator and DevOps assistant. " +
                "You help users manage Proxmox nodes, QEMU VMs, LXC containers, ZFS/Ceph/NFS storage, and terminal tasks. " +
                "Provide direct, concise, and safe shell commands (e.g., using pvesm, qm, pct, pveam, zpool, zfs, systemctl). " +
                "If commands are dangerous, clearly warn the user before the command.";

        JSONObject requestJson = new JSONObject();

        // System Instruction
        JSONObject sysInstructionObj = new JSONObject();
        JSONArray sysPartsArr = new JSONArray();
        JSONObject sysTextObj = new JSONObject();
        sysTextObj.put("text", systemInstruction + (context != null && !context.isEmpty() ? "\n\nCURRENT CONTEXT:\n" + context : ""));
        sysPartsArr.put(sysTextObj);
        sysInstructionObj.put("parts", sysPartsArr);
        requestJson.put("system_instruction", sysInstructionObj);

        // User Content
        JSONArray contentsArr = new JSONArray();
        JSONObject userContent = new JSONObject();
        userContent.put("role", "user");
        JSONArray userParts = new JSONArray();
        JSONObject userText = new JSONObject();
        userText.put("text", userPrompt);
        userParts.put(userText);
        userContent.put("parts", userParts);
        contentsArr.put(userContent);
        requestJson.put("contents", contentsArr);

        // Generation Config
        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", 0.4);
        genConfig.put("maxOutputTokens", 2048);
        requestJson.put("generationConfig", genConfig);

        byte[] payload = requestJson.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
            os.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseSb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseSb.append(line).append("\n");
            }
        }
        conn.disconnect();

        if (code >= 200 && code < 300) {
            JSONObject respObj = new JSONObject(responseSb.toString());
            JSONArray candidates = respObj.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.optJSONObject("content");
                if (content != null) {
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "No response text.");
                    }
                }
            }
            return "No response generated.";
        } else {
            String errorMsg = "HTTP " + code;
            try {
                JSONObject errObj = new JSONObject(responseSb.toString());
                JSONObject err = errObj.optJSONObject("error");
                if (err != null && err.has("message")) {
                    errorMsg = err.getString("message");
                }
            } catch (Exception ignored) {}
            throw new Exception("Gemini API Error (" + code + "): " + errorMsg);
        }
    }

    /**
     * Calls a local OpenAI-compatible LLM endpoint (Ollama, llama.cpp, LocalAI) running on device or LAN.
     */
    private String askLocalModel(String userPrompt, String context) throws Exception {
        URL url = new URL(localUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        String systemInstruction = "You are an expert Proxmox VE systems administrator. " +
                "Provide direct, concise, and safe commands for Proxmox (qm, pct, pvesm, systemctl, zfs).";
        if (context != null && !context.isEmpty()) {
            systemInstruction += "\n\nCONTEXT:\n" + context;
        }

        JSONObject requestJson = new JSONObject();
        requestJson.put("model", model.contains("gemini") ? "default" : model);

        JSONArray messages = new JSONArray();

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemInstruction);
        messages.put(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.put(userMsg);

        requestJson.put("messages", messages);
        requestJson.put("temperature", 0.4);

        byte[] payload = requestJson.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
            os.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseSb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseSb.append(line).append("\n");
            }
        }
        conn.disconnect();

        if (code >= 200 && code < 300) {
            JSONObject respObj = new JSONObject(responseSb.toString());
            // OpenAI format: choices[0].message.content
            JSONArray choices = respObj.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject msg = choice.optJSONObject("message");
                if (msg != null && msg.has("content")) {
                    return msg.getString("content");
                }
            }
            // Ollama /api/generate fallback format: { "response": "..." }
            if (respObj.has("response")) {
                return respObj.getString("response");
            }
            return responseSb.toString();
        } else {
            throw new Exception("Local Model Error (HTTP " + code + "): " + responseSb.toString());
        }
    }
}
