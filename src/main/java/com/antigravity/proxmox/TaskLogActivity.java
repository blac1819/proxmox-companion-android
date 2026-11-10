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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
