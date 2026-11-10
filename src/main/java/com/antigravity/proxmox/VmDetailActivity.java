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
import com.antigravity.proxmox.model.ServerProfile;
import com.antigravity.proxmox.model.VmInfo;
import com.antigravity.proxmox.storage.ProfileManager;
import com.antigravity.proxmox.util.FormatUtils;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VmDetailActivity extends Activity {
    public static final String EXTRA_VM_ID = "vmid";
    public static final String EXTRA_NODE_NAME = "node";
    public static final String EXTRA_VM_NAME = "vm_name";

    private String nodeName;
    private int vmid;
    private String vmName;
    private ProxmoxClient client;

    private TextView txtTitle;
    private TextView txtSubtitle;
    private TextView badgeStatus;
    private ProgressBar progressLoading;
    private TextView txtCpuVal;
    private ProgressBar barCpu;
    private TextView txtMemVal;
    private ProgressBar barMem;
    private TextView txtUptime;
    private TextView txtNet;
    private TextView txtDiskIo;
    private TextView txtConfig;

    private Button btnStart;
    private Button btnShutdown;
    private Button btnStop;
    private Button btnReboot;
    private Button btnReset;
    private Button btnConsole;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vm_detail);

        vmid = getIntent().getIntExtra(EXTRA_VM_ID, 0);
        nodeName = getIntent().getStringExtra(EXTRA_NODE_NAME);
        vmName = getIntent().getStringExtra(EXTRA_VM_NAME);

        ProfileManager pm = new ProfileManager(this);
        ServerProfile profile = pm.getActiveProfile();
        if (profile == null) {
            Toast.makeText(this, "No active server profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        client = new ProxmoxClient(profile);

        txtTitle = findViewById(R.id.txt_vm_title);
        txtSubtitle = findViewById(R.id.txt_vm_subtitle);
        badgeStatus = findViewById(R.id.badge_vm_status);
        progressLoading = findViewById(R.id.progress_vm_loading);
        txtCpuVal = findViewById(R.id.txt_vm_cpu_val);
        barCpu = findViewById(R.id.bar_vm_cpu);
        txtMemVal = findViewById(R.id.txt_vm_mem_val);
        barMem = findViewById(R.id.bar_vm_mem);
        txtUptime = findViewById(R.id.txt_vm_uptime);
        txtNet = findViewById(R.id.txt_vm_net);
        txtDiskIo = findViewById(R.id.txt_vm_diskio);
        txtConfig = findViewById(R.id.txt_vm_config_details);

        btnStart = findViewById(R.id.btn_power_start);
        btnShutdown = findViewById(R.id.btn_power_shutdown);
        btnStop = findViewById(R.id.btn_power_stop);
        btnReboot = findViewById(R.id.btn_power_reboot);
        btnReset = findViewById(R.id.btn_power_reset);
        btnConsole = findViewById(R.id.btn_open_console);

        txtTitle.setText(vmName != null ? vmName : "VM " + vmid);
        txtSubtitle.setText("Node: " + nodeName + " | ID: " + vmid);

        findViewById(R.id.btn_vm_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("start", "Start VM " + vmid + "?");
            }
        });

        btnShutdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("shutdown", getString(R.string.confirm_shutdown_msg, vmName, String.valueOf(vmid)));
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("stop", getString(R.string.confirm_stop_msg, vmName, String.valueOf(vmid)));
            }
        });

        btnReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("reboot", getString(R.string.confirm_reboot_msg, vmName, String.valueOf(vmid)));
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePowerAction("reset", getString(R.string.confirm_reset_msg, vmName, String.valueOf(vmid)));
            }
        });

        btnConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String consoleUrl = client.buildWebConsoleUrl(nodeName, vmid, "qemu");
                Intent intent = new Intent(VmDetailActivity.this, ConsoleActivity.class);
                intent.putExtra(ConsoleActivity.EXTRA_URL, consoleUrl);
                startActivity(intent);
            }
        });

        loadVmDetails();
    }

    private void loadVmDetails() {
        progressLoading.setVisibility(View.VISIBLE);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final VmInfo vm = client.getVmStatus(nodeName, vmid);
                    final JSONObject config = client.getVmConfig(nodeName, vmid);

                    if (isDestroyed) return;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateUi(vm, config);
                        }
                    });
                } catch (final Exception e) {
                    if (isDestroyed) return;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            Toast.makeText(VmDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void updateUi(VmInfo vm, JSONObject config) {
        progressLoading.setVisibility(View.GONE);
        if (vm == null) return;

        txtTitle.setText(vm.getName());
        txtSubtitle.setText("Node: " + nodeName + " | ID: " + vm.getVmid());

        badgeStatus.setText(vm.getStatus().toUpperCase());
        if (vm.isRunning()) {
            badgeStatus.setTextColor(0xFF2ECC71);
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_running);
            btnStart.setEnabled(false);
            btnShutdown.setEnabled(true);
            btnStop.setEnabled(true);
            btnReboot.setEnabled(true);
            btnReset.setEnabled(true);
        } else if (vm.isPaused()) {
            badgeStatus.setTextColor(0xFFF39C12);
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_paused);
            btnStart.setEnabled(true);
            btnShutdown.setEnabled(false);
            btnStop.setEnabled(true);
            btnReboot.setEnabled(false);
            btnReset.setEnabled(false);
        } else {
            badgeStatus.setTextColor(0xFF7F8C8D);
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_stopped);
            btnStart.setEnabled(true);
            btnShutdown.setEnabled(false);
            btnStop.setEnabled(false);
            btnReboot.setEnabled(false);
            btnReset.setEnabled(false);
        }

        txtCpuVal.setText(String.format("%.1f%% (%d vCPUs)", vm.getCpu() * 100.0, vm.getCpus()));
        barCpu.setProgress(vm.getCpuPercent());

        txtMemVal.setText(String.format("%s / %s (%d%%)", FormatUtils.formatBytes(vm.getMem()), FormatUtils.formatBytes(vm.getMaxMem()), vm.getMemPercent()));
        barMem.setProgress(vm.getMemPercent());

        txtUptime.setText(FormatUtils.formatUptime(vm.getUptime()));
        txtNet.setText(FormatUtils.formatBytes(vm.getNetIn()) + " in / " + FormatUtils.formatBytes(vm.getNetOut()) + " out");
        txtDiskIo.setText(FormatUtils.formatBytes(vm.getDiskRead()) + " read / " + FormatUtils.formatBytes(vm.getDiskWrite()) + " write");

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
                                    final String upid = client.vmAction(nodeName, vmid, action);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(VmDetailActivity.this, "Action '" + action + "' triggered", Toast.LENGTH_SHORT).show();
                                            // Refresh after 2 seconds
                                            mainHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    loadVmDetails();
                                                }
                                            }, 2000);
                                        }
                                    });
                                } catch (final Exception e) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressLoading.setVisibility(View.GONE);
                                            Toast.makeText(VmDetailActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
