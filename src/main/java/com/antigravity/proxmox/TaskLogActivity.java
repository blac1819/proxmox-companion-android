package com.antigravity.proxmox;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import com.antigravity.proxmox.api.ProxmoxClient;
import com.antigravity.proxmox.model.ServerProfile;
import com.antigravity.proxmox.storage.ProfileManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskLogActivity extends Activity {
    public static final String EXTRA_UPID = "upid";
    public static final String EXTRA_NODE_NAME = "node";
    public static final String EXTRA_ACTION_NAME = "action";
    public static final String EXTRA_STATUS = "status";

    private String upid;
    private String nodeName;
    private String actionName;
    private String status;
    private ProxmoxClient client;

    private TextView txtAction;
    private TextView txtUpid;
    private TextView badgeStatus;
    private TextView txtTerminal;
    private ProgressBar progressLoading;
    private ScrollView scrollTerminal;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_log);

        upid = getIntent().getStringExtra(EXTRA_UPID);
        nodeName = getIntent().getStringExtra(EXTRA_NODE_NAME);
        actionName = getIntent().getStringExtra(EXTRA_ACTION_NAME);
        status = getIntent().getStringExtra(EXTRA_STATUS);

        ProfileManager pm = new ProfileManager(this);
        ServerProfile profile = pm.getActiveProfile();
        if (profile == null) {
            Toast.makeText(this, "No active server profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        client = new ProxmoxClient(profile);

        txtAction = findViewById(R.id.txt_task_action);
        txtUpid = findViewById(R.id.txt_task_upid);
        badgeStatus = findViewById(R.id.badge_task_status);
        txtTerminal = findViewById(R.id.txt_terminal_log);
        progressLoading = findViewById(R.id.progress_task_loading);
        scrollTerminal = findViewById(R.id.scroll_terminal);

        txtAction.setText(actionName != null ? actionName : "Task Log");
        txtUpid.setText(upid != null ? upid : "");

        updateStatusBadge(status);

        findViewById(R.id.btn_task_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btn_task_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchLog();
            }
        });

        findViewById(R.id.btn_task_ai).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzeLogWithAi();
            }
        });

        fetchLog();
    }

    private void updateStatusBadge(String st) {
        if ("OK".equalsIgnoreCase(st)) {
            badgeStatus.setText("OK");
            badgeStatus.setTextColor(0xFF2ECC71);
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_running);
        } else if (st == null || st.isEmpty() || "running".equalsIgnoreCase(st)) {
            badgeStatus.setText("RUNNING");
            badgeStatus.setTextColor(0xFFF39C12);
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_paused);
        } else {
            badgeStatus.setText(st.toUpperCase());
            badgeStatus.setTextColor(0xFFE74C3C);
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_stopped);
        }
    }

    private void fetchLog() {
        if (upid == null || nodeName == null) {
            txtTerminal.setText("Missing task UPID or node name.");
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String logContent = client.getTaskLog(nodeName, upid);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            if (logContent.isEmpty()) {
                                txtTerminal.setText("(Empty log output or task still starting...)");
                            } else {
                                txtTerminal.setText(logContent);
                                scrollTerminal.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollTerminal.fullScroll(ScrollView.FOCUS_DOWN);
                                    }
                                });
                            }
                        }
                    });
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            txtTerminal.setText("Error retrieving task log:\n" + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void analyzeLogWithAi() {
        final String currentLog = txtTerminal.getText().toString();
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_ai_assistant, null);
        final EditText editPrompt = dialogView.findViewById(R.id.edit_ai_prompt);
        final TextView txtResponse = dialogView.findViewById(R.id.txt_ai_response);
        final ProgressBar progressBar = dialogView.findViewById(R.id.progress_ai);
        final Button btnAsk = dialogView.findViewById(R.id.btn_ai_ask);

        editPrompt.setText("Analyze this Proxmox task execution output, diagnose any failures, and provide the command to fix it.");

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("🤖 Proxmox Task Diagnostic")
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        btnAsk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String prompt = editPrompt.getText().toString().trim();
                progressBar.setVisibility(View.VISIBLE);
                btnAsk.setEnabled(false);
                txtResponse.setText("Analyzing task logs with Gemini AI...");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        com.antigravity.proxmox.storage.AppSettings settings = new com.antigravity.proxmox.storage.AppSettings(TaskLogActivity.this);
                        com.antigravity.proxmox.api.GeminiClient gemini = new com.antigravity.proxmox.api.GeminiClient(settings.getGeminiApiKey(), settings.getGeminiModel());
                        try {
                            final String answer = gemini.askAssistant(prompt, "Task: " + actionName + " (Status: " + status + ")\nLog:\n" + currentLog);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    btnAsk.setEnabled(true);
                                    txtResponse.setText(answer);
                                }
                            });
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    btnAsk.setEnabled(true);
                                    txtResponse.setText("Error: " + e.getMessage());
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
