package com.antigravity.proxmox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.antigravity.proxmox.api.ProxmoxClient;
import com.antigravity.proxmox.model.NodeInfo;
import com.antigravity.proxmox.model.ServerProfile;
import com.antigravity.proxmox.storage.ProfileManager;
import java.util.ArrayList;
import java.util.List;

public class CreateGuestActivity extends Activity {
    private ProxmoxClient client;
    private List<String> nodeNames = new ArrayList<>();

    private RadioGroup rgType;
    private RadioButton rbLxc;
    private Spinner spinnerNode;
    private EditText editVmid;
    private EditText editName;
    private TextView txtLabelOs;
    private EditText editOs;
    private EditText editCores;
    private EditText editMemory;
    private EditText editStorage;
    private EditText editDisk;
    private View layoutPassword;
    private EditText editPassword;
    private ProgressBar progressBar;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_guest);

        ProfileManager pm = new ProfileManager(this);
        ServerProfile profile = pm.getActiveProfile();
        if (profile == null) {
            Toast.makeText(this, "No active server profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        client = new ProxmoxClient(profile);

        rgType = findViewById(R.id.rg_guest_type);
        rbLxc = findViewById(R.id.rb_type_lxc);
        spinnerNode = findViewById(R.id.spinner_create_node);
        editVmid = findViewById(R.id.edit_create_vmid);
        editName = findViewById(R.id.edit_create_name);
        txtLabelOs = findViewById(R.id.txt_label_os);
        editOs = findViewById(R.id.edit_create_os);
        editCores = findViewById(R.id.edit_create_cores);
        editMemory = findViewById(R.id.edit_create_memory);
        editStorage = findViewById(R.id.edit_create_storage);
        editDisk = findViewById(R.id.edit_create_disk);
        layoutPassword = findViewById(R.id.layout_lxc_password);
        editPassword = findViewById(R.id.edit_create_password);
        progressBar = findViewById(R.id.progress_create);
        btnSubmit = findViewById(R.id.btn_submit_create);

        findViewById(R.id.btn_create_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_type_lxc) {
                    txtLabelOs.setText("LXC OS Template");
                    editOs.setHint("local:vztmpl/debian-12-standard_12.7-1_amd64.tar.zst");
                    layoutPassword.setVisibility(View.VISIBLE);
                } else {
                    txtLabelOs.setText("ISO Image / Storage");
                    editOs.setHint("local:iso/ubuntu-24.04-live-server-amd64.iso");
                    layoutPassword.setVisibility(View.GONE);
                }
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCreation();
            }
        });

        loadNodes();
    }

    private void loadNodes() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<NodeInfo> nodes = client.getNodes();
                    nodeNames.clear();
                    for (NodeInfo n : nodes) {
                        nodeNames.add(n.getName());
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateGuestActivity.this, android.R.layout.simple_spinner_dropdown_item, nodeNames);
                            spinnerNode.setAdapter(adapter);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(CreateGuestActivity.this, "Failed loading nodes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void submitCreation() {
        if (nodeNames.isEmpty()) {
            Toast.makeText(this, "No node available", Toast.LENGTH_SHORT).show();
            return;
        }

        final String node = nodeNames.get(spinnerNode.getSelectedItemPosition());
        final String vmidStr = editVmid.getText().toString().trim();
        final String name = editName.getText().toString().trim();
        final String os = editOs.getText().toString().trim();
        final String storage = editStorage.getText().toString().trim();
        final boolean isLxc = rbLxc.isChecked();

        if (vmidStr.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "VMID and Name are required", Toast.LENGTH_SHORT).show();
            return;
        }

        final int vmid = Integer.parseInt(vmidStr);
        final int cores = Integer.parseInt(editCores.getText().toString().trim());
        final long memory = Long.parseLong(editMemory.getText().toString().trim());
        final int disk = Integer.parseInt(editDisk.getText().toString().trim());
        final String pass = editPassword.getText().toString();

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String upid;
                    if (isLxc) {
                        upid = client.createLxc(node, vmid, name, os, cores, memory, 512, storage, disk, pass);
                    } else {
                        upid = client.createVm(node, vmid, name, cores, memory, "l26", os, storage, disk);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            Toast.makeText(CreateGuestActivity.this, "Creation started (" + upid + ")", Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            Toast.makeText(CreateGuestActivity.this, "Creation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}
