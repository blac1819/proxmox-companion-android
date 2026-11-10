package com.antigravity.proxmox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import com.antigravity.proxmox.api.ProxmoxClient;
import com.antigravity.proxmox.model.LxcInfo;
import com.antigravity.proxmox.model.ServerProfile;
import com.antigravity.proxmox.storage.ProfileManager;
import com.antigravity.proxmox.util.FormatUtils;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LxcDetailActivity extends Activity {
    public static final String EXTRA_LXC_ID = "vmid";
    public static final String EXTRA_NODE_NAME = "node";
    public static final String EXTRA_LXC_NAME = "lxc_name";

    private String nodeName;
    private int vmid;
    private String lxcName;
    private ProxmoxClient client;

    private TextView txtTitle;
    private TextView txtSubtitle;
    private TextView badgeStatus;
    private ProgressBar progressLoading;
    private TextView txtCpuVal;
    private ProgressBar barCpu;
    private TextView txtMemVal;
    private ProgressBar barMem;
    private TextView txtSwapVal;
    private ProgressBar barSwap;
    private TextView txtDiskVal;
    private ProgressBar barDisk;
    private TextView txtUptime;
    private TextView txtConfig;

    private Button btnStart;
    private Button btnShutdown;
    private Button btnStop;
    private Button btnReboot;
    private Button btnConsole;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lxc_detail);

        vmid = getIntent().getIntExtra(EXTRA_LXC_ID, 0);
        nodeName = getIntent().getStringExtra(EXTRA_NODE_NAME);
        lxcName = getIntent().getStringExtra(EXTRA_LXC_NAME);

        ProfileManager pm = new ProfileManager(this);
        ServerProfile profile = pm.getActiveProfile();
        if (profile == null) {
            Toast.makeText(this, "No active server profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        client = new ProxmoxClient(profile);

        txtTitle = findViewById(R.id.txt_lxc_title);
        txtSubtitle = findViewById(R.id.txt_lxc_subtitle);
        badgeStatus = findViewById(R.id.badge_lxc_status);
        progressLoading = findViewById(R.id.progress_lxc_loading);
        txtCpuVal = findViewById(R.id.txt_lxc_cpu_val);
        barCpu = findViewById(R.id.bar_lxc_cpu);
        txtMemVal = findViewById(R.id.txt_lxc_mem_val);
        barMem = findViewById(R.id.bar_lxc_mem);
        txtSwapVal = findViewById(R.id.txt_lxc_swap_val);
        barSwap = findViewById(R.id.bar_lxc_swap);
        txtDiskVal = findViewById(R.id.txt_lxc_disk_val);
        barDisk = findViewById(R.id.bar_lxc_disk);
        txtUptime = findViewById(R.id.txt_lxc_uptime);
        txtConfig = findViewById(R.id.txt_lxc_config_details);

        btnStart = findViewById(R.id.btn_lxc_start);
        btnShutdown = findViewById(R.id.btn_lxc_shutdown);
        btnStop = findViewById(R.id.btn_lxc_stop);
        btnReboot = findViewById(R.id.btn_lxc_reboot);
        btnConsole = findViewById(R.id.btn_lxc_console);

        txtTitle.setText(lxcName != null ? lxcName : "CT " + vmid);
        txtSubtitle.setText("Node: " + nodeName + " | ID: " + vmid);

        findViewById(R.id.btn_lxc_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("start", "Start Container " + vmid + "?");
            }
        });

        btnShutdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("shutdown", "Gracefully shutdown Container " + vmid + "?");
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("stop", "Forcibly stop Container " + vmid + "?");
            }
        });

        btnReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("reboot", "Reboot Container " + vmid + "?");
            }
        });

        btnConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String consoleUrl = client.buildWebConsoleUrl(nodeName, vmid, "lxc");
                Intent intent = new Intent(LxcDetailActivity.this, ConsoleActivity.class);
                intent.putExtra(ConsoleActivity.EXTRA_URL, consoleUrl);
                startActivity(intent);
            }
        });

        loadLxcDetails();
    }

    private void loadLxcDetails() {
        progressLoading.setVisibility(View.VISIBLE);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final LxcInfo lxc = client.getLxcStatus(nodeName, vmid);
                    final JSONObject config = client.getLxcConfig(nodeName, vmid);

                    if (isDestroyed) return;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateUi(lxc, config);
                        }
                    });
                } catch (final Exception e) {
                    if (isDestroyed) return;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            Toast.makeText(LxcDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void updateUi(LxcInfo lxc, JSONObject config) {
        progressLoading.setVisibility(View.GONE);
        if (lxc == null) return;

        txtTitle.setText(lxc.getName());
        txtSubtitle.setText("Node: " + nodeName + " | ID: " + lxc.getVmid());

        badgeStatus.setText(lxc.getStatus().toUpperCase());
        if (lxc.isRunning()) {
            badgeStatus.setTextColor(0xFF2ECC71);
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_running);
            btnStart.setEnabled(false);
            btnShutdown.setEnabled(true);
            btnStop.setEnabled(true);
            btnReboot.setEnabled(true);
        } else {
            badgeStatus.setTextColor(0xFF7F8C8D);
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_stopped);
            btnStart.setEnabled(true);
            btnShutdown.setEnabled(false);
            btnStop.setEnabled(false);
            btnReboot.setEnabled(false);
        }

        txtCpuVal.setText(String.format("%.1f%% (%d cores)", lxc.getCpu() * 100.0, lxc.getCpus()));
        barCpu.setProgress(lxc.getCpuPercent());

        txtMemVal.setText(String.format("%s / %s (%d%%)", FormatUtils.formatBytes(lxc.getMem()), FormatUtils.formatBytes(lxc.getMaxMem()), lxc.getMemPercent()));
        barMem.setProgress(lxc.getMemPercent());

        txtSwapVal.setText(String.format("%s / %s (%d%%)", FormatUtils.formatBytes(lxc.getSwap()), FormatUtils.formatBytes(lxc.getMaxSwap()), lxc.getSwapPercent()));
        barSwap.setProgress(lxc.getSwapPercent());

        txtDiskVal.setText(String.format("%s / %s (%d%%)", FormatUtils.formatBytes(lxc.getDisk()), FormatUtils.formatBytes(lxc.getMaxDisk()), lxc.getDiskPercent()));
        barDisk.setProgress(lxc.getDiskPercent());

        txtUptime.setText(FormatUtils.formatUptime(lxc.getUptime()));

        if (config != null) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = config.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                sb.append("• ").append(key).append(": ").append(config.optString(key)).append("\n");
            }
            txtConfig.setText(sb.toString().trim());
        }
    }

    private void executePowerAction(final String action, String confirmMessage) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_title)
                .setMessage(confirmMessage)
                .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressLoading.setVisibility(View.VISIBLE);
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final String upid = client.lxcAction(nodeName, vmid, action);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LxcDetailActivity.this, "Action '" + action + "' triggered", Toast.LENGTH_SHORT).show();
                                            mainHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    loadLxcDetails();
                                                }
                                            }, 2000);
                                        }
                                    });
                                } catch (final Exception e) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressLoading.setVisibility(View.GONE);
                                            Toast.makeText(LxcDetailActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        });
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        executor.shutdown();
    }
}
