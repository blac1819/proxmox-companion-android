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
import java.util.List;

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

        setupFileTransferAndSnapshots();
        loadLxcDetails();
    }

    private TextView txtPorts;
    private EditText editFilePath;
    private LinearLayout layoutSnapshots;

    private void setupFileTransferAndSnapshots() {
        txtPorts = findViewById(R.id.txt_lxc_ports);
        editFilePath = findViewById(R.id.edit_lxc_file_path);
        layoutSnapshots = findViewById(R.id.layout_lxc_snapshots);

        Button btnDownload = findViewById(R.id.btn_lxc_file_download);
        Button btnUpload = findViewById(R.id.btn_lxc_file_upload);
        Button btnNewSnapshot = findViewById(R.id.btn_lxc_create_snapshot);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String path = editFilePath.getText().toString().trim();
                if (path.isEmpty()) {
                    Toast.makeText(LxcDetailActivity.this, "Enter container file path", Toast.LENGTH_SHORT).show();
                    return;
                }
                progressLoading.setVisibility(View.VISIBLE);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String content = client.readLxcFile(nodeName, vmid, path);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressLoading.setVisibility(View.GONE);
                                    showFileContentDialog(path, content);
                                }
                            });
                        } catch (final Exception e) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressLoading.setVisibility(View.GONE);
                                    Toast.makeText(LxcDetailActivity.this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String path = editFilePath.getText().toString().trim();
                if (path.isEmpty()) {
                    Toast.makeText(LxcDetailActivity.this, "Enter target file path", Toast.LENGTH_SHORT).show();
                    return;
                }
                showUploadDialog(path);
            }
        });

        btnNewSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateSnapshotDialog();
            }
        });
    }

    private void showFileContentDialog(final String path, final String content) {
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(content);
        tv.setFontFeatureSettings("monospace");
        tv.setTextColor(0xFFEDEDED);
        tv.setTextIsSelectable(true);
        tv.setPadding(30, 20, 30, 20);
        sv.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("📄 " + path)
                .setView(sv)
                .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("File Content", content));
                        Toast.makeText(LxcDetailActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showUploadDialog(final String path) {
        final EditText input = new EditText(this);
        input.setHint("Paste or type text to write into " + path);
        input.setTextColor(0xFFFFFFFF);
        input.setPadding(30, 30, 30, 30);
        input.setMinLines(5);

        new AlertDialog.Builder(this)
                .setTitle("⬆ Write to Container: " + path)
                .setView(input)
                .setPositiveButton("Write", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String data = input.getText().toString();
                        progressLoading.setVisibility(View.VISIBLE);
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    client.writeLxcFile(nodeName, vmid, path, data);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressLoading.setVisibility(View.GONE);
                                            Toast.makeText(LxcDetailActivity.this, "File written successfully!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } catch (final Exception e) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressLoading.setVisibility(View.GONE);
                                            Toast.makeText(LxcDetailActivity.this, "Write failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateSnapshotDialog() {
        final EditText input = new EditText(this);
        input.setHint("Snapshot Name (e.g. backup_before_update)");
        input.setTextColor(0xFFFFFFFF);
        input.setPadding(30, 30, 30, 30);

        new AlertDialog.Builder(this)
                .setTitle("📸 Take Snapshot")
                .setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String snapName = input.getText().toString().trim();
                        if (snapName.isEmpty()) return;
                        progressLoading.setVisibility(View.VISIBLE);
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    client.createSnapshot(nodeName, vmid, true, snapName, "Created from Android app", false);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressLoading.setVisibility(View.GONE);
                                            Toast.makeText(LxcDetailActivity.this, "Snapshot created!", Toast.LENGTH_SHORT).show();
                                            loadSnapshots();
                                        }
                                    });
                                } catch (final Exception e) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressLoading.setVisibility(View.GONE);
                                            Toast.makeText(LxcDetailActivity.this, "Snapshot failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadSnapshots() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<com.antigravity.proxmox.model.GuestSnapshot> snaps = client.getSnapshots(nodeName, vmid, true);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            renderSnapshots(snaps);
                        }
                    });
                } catch (Exception ignored) {}
            }
        });
    }

    private void renderSnapshots(List<com.antigravity.proxmox.model.GuestSnapshot> snaps) {
        layoutSnapshots.removeAllViews();
        if (snaps == null || snaps.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No snapshots taken yet.");
            empty.setTextColor(0xFF888888);
            empty.setTextSize(11f);
            layoutSnapshots.addView(empty);
            return;
        }

        for (final com.antigravity.proxmox.model.GuestSnapshot s : snaps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView txt = new TextView(this);
            txt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            txt.setText(s.getName() + "\n" + com.antigravity.proxmox.util.FormatUtils.formatTimestamp(s.getTimestamp()));
            txt.setTextColor(0xFFEDEDED);
            txt.setTextSize(11f);
            row.addView(txt);

            Button btnRollback = new Button(this);
            btnRollback.setText("↺ Rollback");
            btnRollback.setTextSize(10f);
            btnRollback.setBackgroundResource(R.drawable.btn_secondary);
            btnRollback.setTextColor(0xFFFFFFFF);
            btnRollback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmRollback(s.getName());
                }
            });
            row.addView(btnRollback);

            layoutSnapshots.addView(row);
        }
    }

    private void confirmRollback(final String snapName) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Rollback")
                .setMessage("Rollback container " + vmid + " to snapshot '" + snapName + "'?")
                .setPositiveButton("Rollback", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    client.rollbackSnapshot(nodeName, vmid, true, snapName);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LxcDetailActivity.this, "Rollback triggered!", Toast.LENGTH_SHORT).show();
                                            loadLxcDetails();
                                        }
                                    });
                                } catch (final Exception e) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LxcDetailActivity.this, "Rollback failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();

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
