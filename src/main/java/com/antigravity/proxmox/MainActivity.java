package com.antigravity.proxmox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.antigravity.proxmox.FormatUtils;
import com.antigravity.proxmox.api.ProxmoxClient;
import com.antigravity.proxmox.model.*;
import com.antigravity.proxmox.storage.ProfileManager;
import com.antigravity.proxmox.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int TAB_NODES = 0;
    private static final int TAB_VMS = 1;
    private static final int TAB_LXC = 2;
    private static final int TAB_STORAGE = 3;
    private static final int TAB_TASKS = 4;

    private int currentTab = TAB_NODES;

    private ProfileManager profileManager;
    private ServerProfile currentProfile;
    private ProxmoxClient client;

    private Spinner spinnerServers;
    private Button btnManageServers;
    private Button btnWebConsole;
    private Button btnRefresh;

    private Button tabNodes;
    private Button tabVms;
    private Button tabLxc;
    private Button tabStorage;
    private Button tabTasks;

    private ProgressBar progressLoading;
    private View layoutErrorBanner;
    private TextView txtErrorBanner;
    private Button btnRetry;

    private TextView txtEmptyState;
    private LinearLayout layoutItems;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isDestroyed = false;

    // Cache of fetched data
    private List<NodeInfo> cachedNodes = new ArrayList<>();
    private List<VmInfo> cachedVms = new ArrayList<>();
    private List<LxcInfo> cachedLxc = new ArrayList<>();
    private List<StorageInfo> cachedStorage = new ArrayList<>();
    private List<TaskInfo> cachedTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        profileManager = new ProfileManager(this);

        spinnerServers = findViewById(R.id.spinner_servers);
        btnManageServers = findViewById(R.id.btn_manage_servers);
        btnWebConsole = findViewById(R.id.btn_web_console);
        btnRefresh = findViewById(R.id.btn_refresh);

        tabNodes = findViewById(R.id.tab_nodes);
        tabVms = findViewById(R.id.tab_vms);
        tabLxc = findViewById(R.id.tab_lxc);
        tabStorage = findViewById(R.id.tab_storage);
        tabTasks = findViewById(R.id.tab_tasks);

        progressLoading = findViewById(R.id.progress_loading);
        layoutErrorBanner = findViewById(R.id.layout_error_banner);
        txtErrorBanner = findViewById(R.id.txt_error_banner);
        btnRetry = findViewById(R.id.btn_retry);

        txtEmptyState = findViewById(R.id.txt_empty_state);
        layoutItems = findViewById(R.id.layout_items);

        setupTabs();
        setupListeners();
        loadServersList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadServersList();
    }

    private void setupTabs() {
        tabNodes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectTab(TAB_NODES); }
        });
        tabVms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectTab(TAB_VMS); }
        });
        tabLxc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectTab(TAB_LXC); }
        });
        tabStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectTab(TAB_STORAGE); }
        });
        tabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectTab(TAB_TASKS); }
        });
    }

    private void selectTab(int tab) {
        currentTab = tab;
        updateTabStyles();
        renderCurrentTab();
    }

    private void updateTabStyles() {
        int activeColor = 0xFFFF8C00;
        int inactiveColor = 0xFFAAAAAA;

        tabNodes.setTextColor(currentTab == TAB_NODES ? activeColor : inactiveColor);
        tabVms.setTextColor(currentTab == TAB_VMS ? activeColor : inactiveColor);
        tabLxc.setTextColor(currentTab == TAB_LXC ? activeColor : inactiveColor);
        tabStorage.setTextColor(currentTab == TAB_STORAGE ? activeColor : inactiveColor);
        tabTasks.setTextColor(currentTab == TAB_TASKS ? activeColor : inactiveColor);
    }

    private void setupListeners() {
        btnManageServers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ServerEditActivity.class);
                if (currentProfile != null) {
                    intent.putExtra(ServerEditActivity.EXTRA_SERVER_ID, currentProfile.getId());
                }
                startActivity(intent);
            }
        });

        btnWebConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentProfile != null) {
                    Intent intent = new Intent(MainActivity.this, ConsoleActivity.class);
                    intent.putExtra(ConsoleActivity.EXTRA_URL, currentProfile.getBaseUrl() + "/");
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Configure a server first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshData();
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshData();
            }
        });
    }

    private void loadServersList() {
        final List<ServerProfile> profiles = profileManager.getProfiles();
        List<String> names = new ArrayList<>();
        int selectedIndex = 0;
        String activeId = profileManager.getActiveProfileId();

        for (int i = 0; i < profiles.size(); i++) {
            ServerProfile p = profiles.get(i);
            names.add(p.getName());
            if (p.getId().equals(activeId)) {
                selectedIndex = i;
            }
        }
        names.add("+ Add New Server...");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, names) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(0xFFFFFFFF);
                    ((TextView) v).setTextSize(13f);
                }
                return v;
            }
        };
        spinnerServers.setAdapter(adapter);

        if (!profiles.isEmpty()) {
            spinnerServers.setSelection(selectedIndex);
            currentProfile = profiles.get(selectedIndex);
            client = new ProxmoxClient(currentProfile);
            refreshData();
        } else {
            currentProfile = null;
            client = null;
            showEmpty("No servers configured. Tap + Add New Server to connect.");
        }

        spinnerServers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == profiles.size()) {
                    // Selected "+ Add New Server..."
                    Intent intent = new Intent(MainActivity.this, ServerEditActivity.class);
                    startActivity(intent);
                } else {
                    ServerProfile selected = profiles.get(position);
                    if (currentProfile == null || !currentProfile.getId().equals(selected.getId())) {
                        currentProfile = selected;
                        profileManager.setActiveProfileId(selected.getId());
                        client = new ProxmoxClient(currentProfile);
                        refreshData();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshData() {
        if (client == null) {
            showEmpty(getString(R.string.empty_servers));
            return;
        }

        layoutErrorBanner.setVisibility(View.GONE);
        progressLoading.setVisibility(View.VISIBLE);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Fetch nodes first
                    final List<NodeInfo> nodes = client.getNodes();
                    final List<VmInfo> vms = new ArrayList<>();
                    final List<LxcInfo> lxcs = new ArrayList<>();
                    final List<StorageInfo> storageList = new ArrayList<>();

                    for (NodeInfo n : nodes) {
                        try {
                            NodeInfo detailed = client.getNodeStatus(n.getName());
                            n.copyDetailedFrom(detailed);
                        } catch (Exception ignored) {}

                        try {
                            List<VmInfo> nodeVms = client.getNodeQemuVms(n.getName());
                            vms.addAll(nodeVms);
                        } catch (Exception ignored) {}

                        try {
                            List<LxcInfo> nodeLxcs = client.getNodeLxcContainers(n.getName());
                            lxcs.addAll(nodeLxcs);
                        } catch (Exception ignored) {}

                        try {
                            List<StorageInfo> sList = client.getNodeStorage(n.getName());
                            storageList.addAll(sList);
                        } catch (Exception ignored) {}
                    }

                    List<TaskInfo> tasks = new ArrayList<>();
                    try {
                        tasks = client.getClusterTasks();
                    } catch (Exception ignored) {}

                    final List<TaskInfo> finalTasks = tasks;

                    if (isDestroyed) return;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            cachedNodes = nodes;
                            cachedVms = vms;
                            cachedLxc = lxcs;
                            cachedStorage = storageList;
                            cachedTasks = finalTasks;
                            renderCurrentTab();
                        }
                    });

                } catch (final Exception e) {
                    if (isDestroyed) return;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            showError("Connection failed: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void renderCurrentTab() {
        layoutItems.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        switch (currentTab) {
            case TAB_NODES:
                if (cachedNodes.isEmpty()) {
                    showEmpty(getString(R.string.empty_nodes));
                } else {
                    hideEmpty();
                    for (final NodeInfo node : cachedNodes) {
                        View item = inflater.inflate(R.layout.item_node, layoutItems, false);
                        TextView txtName = item.findViewById(R.id.txt_node_name);
                        TextView badgeStatus = item.findViewById(R.id.badge_node_status);
                        TextView txtCpu = item.findViewById(R.id.txt_node_cpu);
                        ProgressBar barCpu = item.findViewById(R.id.bar_node_cpu);
                        TextView txtMem = item.findViewById(R.id.txt_node_mem);
                        ProgressBar barMem = item.findViewById(R.id.bar_node_mem);
                        TextView txtUptime = item.findViewById(R.id.txt_node_uptime);
                        TextView txtLoad = item.findViewById(R.id.txt_node_loadavg);

                        txtName.setText(node.getName());
                        badgeStatus.setText(node.getStatus().toUpperCase());
                        if (node.isOnline()) {
                            badgeStatus.setTextColor(0xFF2ECC71);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_running);
                        } else {
                            badgeStatus.setTextColor(0xFFE74C3C);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_stopped);
                        }

                        txtCpu.setText(String.format("%.1f%% (%d cores)", node.getCpu() * 100.0, node.getMaxCpu()));
                        barCpu.setProgress(node.getCpuPercent());

                        txtMem.setText(FormatUtils.formatBytes(node.getMem()) + " / " + FormatUtils.formatBytes(node.getMaxMem()));
                        barMem.setProgress(node.getMemPercent());

                        txtUptime.setText("Uptime: " + FormatUtils.formatUptime(node.getUptime()));
                        txtLoad.setText(String.format("Load: %.2f, %.2f, %.2f", node.getLoadAvg1(), node.getLoadAvg5(), node.getLoadAvg15()));

                        layoutItems.addView(item);
                    }
                }
                break;

            case TAB_VMS:
                if (cachedVms.isEmpty()) {
                    showEmpty(getString(R.string.empty_vms));
                } else {
                    hideEmpty();
                    for (final VmInfo vm : cachedVms) {
                        View item = inflater.inflate(R.layout.item_guest, layoutItems, false);
                        TextView badgeVmid = item.findViewById(R.id.badge_guest_vmid);
                        TextView txtName = item.findViewById(R.id.txt_guest_name);
                        TextView badgeStatus = item.findViewById(R.id.badge_guest_status);
                        TextView txtSpecs = item.findViewById(R.id.txt_guest_specs);
                        TextView txtCpuLabel = item.findViewById(R.id.txt_guest_cpu_label);
                        ProgressBar barCpu = item.findViewById(R.id.bar_guest_cpu);
                        TextView txtMemLabel = item.findViewById(R.id.txt_guest_mem_label);
                        ProgressBar barMem = item.findViewById(R.id.bar_guest_mem);
                        TextView txtUptime = item.findViewById(R.id.txt_guest_uptime);
                        Button btnPower = item.findViewById(R.id.btn_guest_quick_power);
                        Button btnDetails = item.findViewById(R.id.btn_guest_details);

                        badgeVmid.setText(String.valueOf(vm.getVmid()));
                        txtName.setText(vm.getName());
                        badgeStatus.setText(vm.getStatus().toUpperCase());

                        if (vm.isRunning()) {
                            badgeStatus.setTextColor(0xFF2ECC71);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_running);
                            btnPower.setText("⏹ Stop");
                            btnPower.setBackgroundResource(R.drawable.btn_danger);
                        } else if (vm.isPaused()) {
                            badgeStatus.setTextColor(0xFFF39C12);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_paused);
                            btnPower.setText("▶ Resume");
                            btnPower.setBackgroundResource(R.drawable.btn_success);
                        } else {
                            badgeStatus.setTextColor(0xFF7F8C8D);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_stopped);
                            btnPower.setText("▶ Start");
                            btnPower.setBackgroundResource(R.drawable.btn_success);
                        }

                        txtSpecs.setText("Node: " + vm.getNode() + " | " + vm.getCpus() + " vCPUs | " + FormatUtils.formatBytes(vm.getMaxMem()) + " RAM");
                        txtCpuLabel.setText(String.format("CPU: %.1f%%", vm.getCpu() * 100.0));
                        barCpu.setProgress(vm.getCpuPercent());

                        txtMemLabel.setText(String.format("RAM: %s (%d%%)", FormatUtils.formatBytes(vm.getMem()), vm.getMemPercent()));
                        barMem.setProgress(vm.getMemPercent());

                        txtUptime.setText("Up: " + FormatUtils.formatUptime(vm.getUptime()));

                        btnPower.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                handleVmQuickPower(vm);
                            }
                        });

                        btnDetails.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MainActivity.this, VmDetailActivity.class);
                                intent.putExtra(VmDetailActivity.EXTRA_VM_ID, vm.getVmid());
                                intent.putExtra(VmDetailActivity.EXTRA_NODE_NAME, vm.getNode());
                                intent.putExtra(VmDetailActivity.EXTRA_VM_NAME, vm.getName());
                                startActivity(intent);
                            }
                        });

                        layoutItems.addView(item);
                    }
                }
                break;

            case TAB_LXC:
                if (cachedLxc.isEmpty()) {
                    showEmpty(getString(R.string.empty_lxc));
                } else {
                    hideEmpty();
                    for (final LxcInfo lxc : cachedLxc) {
                        View item = inflater.inflate(R.layout.item_guest, layoutItems, false);
                        TextView badgeVmid = item.findViewById(R.id.badge_guest_vmid);
                        TextView txtName = item.findViewById(R.id.txt_guest_name);
                        TextView badgeStatus = item.findViewById(R.id.badge_guest_status);
                        TextView txtSpecs = item.findViewById(R.id.txt_guest_specs);
                        TextView txtCpuLabel = item.findViewById(R.id.txt_guest_cpu_label);
                        ProgressBar barCpu = item.findViewById(R.id.bar_guest_cpu);
                        TextView txtMemLabel = item.findViewById(R.id.txt_guest_mem_label);
                        ProgressBar barMem = item.findViewById(R.id.bar_guest_mem);
                        TextView txtUptime = item.findViewById(R.id.txt_guest_uptime);
                        Button btnPower = item.findViewById(R.id.btn_guest_quick_power);
                        Button btnDetails = item.findViewById(R.id.btn_guest_details);

                        badgeVmid.setText(String.valueOf(lxc.getVmid()));
                        txtName.setText(lxc.getName());
                        badgeStatus.setText(lxc.getStatus().toUpperCase());

                        if (lxc.isRunning()) {
                            badgeStatus.setTextColor(0xFF2ECC71);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_running);
                            btnPower.setText("⏹ Stop");
                            btnPower.setBackgroundResource(R.drawable.btn_danger);
                        } else {
                            badgeStatus.setTextColor(0xFF7F8C8D);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_stopped);
                            btnPower.setText("▶ Start");
                            btnPower.setBackgroundResource(R.drawable.btn_success);
                        }

                        txtSpecs.setText("Node: " + lxc.getNode() + " | " + lxc.getCpus() + " cores | " + FormatUtils.formatBytes(lxc.getMaxMem()) + " RAM");
                        txtCpuLabel.setText(String.format("CPU: %.1f%%", lxc.getCpu() * 100.0));
                        barCpu.setProgress(lxc.getCpuPercent());

                        txtMemLabel.setText(String.format("RAM: %s (%d%%)", FormatUtils.formatBytes(lxc.getMem()), lxc.getMemPercent()));
                        barMem.setProgress(lxc.getMemPercent());

                        txtUptime.setText("Up: " + FormatUtils.formatUptime(lxc.getUptime()));

                        btnPower.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                handleLxcQuickPower(lxc);
                            }
                        });

                        btnDetails.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MainActivity.this, LxcDetailActivity.class);
                                intent.putExtra(LxcDetailActivity.EXTRA_LXC_ID, lxc.getVmid());
                                intent.putExtra(LxcDetailActivity.EXTRA_NODE_NAME, lxc.getNode());
                                intent.putExtra(LxcDetailActivity.EXTRA_LXC_NAME, lxc.getName());
                                startActivity(intent);
                            }
                        });

                        layoutItems.addView(item);
                    }
                }
                break;

            case TAB_STORAGE:
                if (cachedStorage.isEmpty()) {
                    showEmpty(getString(R.string.empty_storage));
                } else {
                    hideEmpty();
                    for (StorageInfo s : cachedStorage) {
                        View item = inflater.inflate(R.layout.item_storage, layoutItems, false);
                        TextView txtName = item.findViewById(R.id.txt_storage_name);
                        TextView badgeType = item.findViewById(R.id.badge_storage_type);
                        TextView txtUsage = item.findViewById(R.id.txt_storage_usage);
                        TextView txtPct = item.findViewById(R.id.txt_storage_pct);
                        ProgressBar barUsage = item.findViewById(R.id.bar_storage_usage);
                        TextView txtContent = item.findViewById(R.id.txt_storage_content);

                        txtName.setText(s.getStorage() + " (" + s.getNode() + ")");
                        badgeType.setText(s.getType());
                        txtUsage.setText(FormatUtils.formatBytes(s.getUsed()) + " / " + FormatUtils.formatBytes(s.getTotal()));
                        txtPct.setText(s.getPercent() + "%");
                        barUsage.setProgress(s.getPercent());
                        txtContent.setText("Content: " + (s.getContent().isEmpty() ? "all" : s.getContent()));

                        layoutItems.addView(item);
                    }
                }
                break;

            case TAB_TASKS:
                if (cachedTasks.isEmpty()) {
                    showEmpty(getString(R.string.empty_tasks));
                } else {
                    hideEmpty();
                    for (final TaskInfo t : cachedTasks) {
                        View item = inflater.inflate(R.layout.item_task, layoutItems, false);
                        TextView txtType = item.findViewById(R.id.txt_task_type);
                        TextView badgeStatus = item.findViewById(R.id.badge_task_item_status);
                        TextView txtUser = item.findViewById(R.id.txt_task_item_user);
                        TextView txtTime = item.findViewById(R.id.txt_task_item_time);

                        txtType.setText(t.getDescription());
                        txtUser.setText("Node: " + t.getNode() + " | " + t.getUser());
                        txtTime.setText("Started: " + FormatUtils.formatTimestamp(t.getStarttime()));

                        badgeStatus.setText(t.getStatus().toUpperCase());
                        if (t.isOk()) {
                            badgeStatus.setTextColor(0xFF2ECC71);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_running);
                        } else if (t.isRunning()) {
                            badgeStatus.setTextColor(0xFFF39C12);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_paused);
                        } else {
                            badgeStatus.setTextColor(0xFFE74C3C);
                            badgeStatus.setBackgroundResource(R.drawable.bg_badge_stopped);
                        }

                        item.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MainActivity.this, TaskLogActivity.class);
                                intent.putExtra(TaskLogActivity.EXTRA_UPID, t.getUpid());
                                intent.putExtra(TaskLogActivity.EXTRA_NODE_NAME, t.getNode());
                                intent.putExtra(TaskLogActivity.EXTRA_ACTION_NAME, t.getDescription());
                                intent.putExtra(TaskLogActivity.EXTRA_STATUS, t.getStatus());
                                startActivity(intent);
                            }
                        });

                        layoutItems.addView(item);
                    }
                }
                break;
        }
    }

    private void handleVmQuickPower(final VmInfo vm) {
        final String action = vm.isRunning() ? "stop" : "start";
        String msg = vm.isRunning() ? getString(R.string.confirm_stop_msg, vm.getName(), String.valueOf(vm.getVmid()))
                                    : "Start " + vm.getName() + " (" + vm.getVmid() + ")?";

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_title)
                .setMessage(msg)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressLoading.setVisibility(View.VISIBLE);
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    client.vmAction(vm.getNode(), vm.getVmid(), action);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "Action triggered", Toast.LENGTH_SHORT).show();
                                            mainHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    refreshData();
                                                }
                                            }, 2000);
                                        }
                                    });
                                } catch (final Exception e) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressLoading.setVisibility(View.GONE);
                                            Toast.makeText(MainActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void handleLxcQuickPower(final LxcInfo lxc) {
        final String action = lxc.isRunning() ? "stop" : "start";
        String msg = lxc.isRunning() ? "Stop container " + lxc.getName() + " (" + lxc.getVmid() + ")?"
                                     : "Start container " + lxc.getName() + " (" + lxc.getVmid() + ")?";

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_title)
                .setMessage(msg)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressLoading.setVisibility(View.VISIBLE);
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    client.lxcAction(lxc.getNode(), lxc.getVmid(), action);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "Action triggered", Toast.LENGTH_SHORT).show();
                                            mainHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    refreshData();
                                                }
                                            }, 2000);
                                        }
                                    });
                                } catch (final Exception e) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressLoading.setVisibility(View.GONE);
                                            Toast.makeText(MainActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void showEmpty(String message) {
        txtEmptyState.setVisibility(View.VISIBLE);
        txtEmptyState.setText(message);
        layoutItems.removeAllViews();
    }

    private void hideEmpty() {
        txtEmptyState.setVisibility(View.GONE);
    }

    private void showError(String message) {
        layoutErrorBanner.setVisibility(View.VISIBLE);
        txtErrorBanner.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        executor.shutdown();
    }
}
