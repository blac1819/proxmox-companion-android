package com.antigravity.proxmox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.antigravity.proxmox.api.ProxmoxClient;
import com.antigravity.proxmox.model.ServerProfile;
import com.antigravity.proxmox.storage.ProfileManager;
import java.util.Arrays;
import java.util.List;

public class StorageCreateActivity extends Activity {
    private ProxmoxClient client;

    private Spinner spinnerType;
    private EditText editId;
    private EditText editServer;
    private EditText editExport;
    private EditText editContent;
    private TextView txtServerLabel;
    private TextView txtExportLabel;
    private ProgressBar progressBar;
    private Button btnSave;

    private final List<String> storageTypes = Arrays.asList("nfs", "cifs", "dir", "zfspool");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_create);

        ProfileManager pm = new ProfileManager(this);
        ServerProfile profile = pm.getActiveProfile();
        if (profile == null) {
            Toast.makeText(this, "No active server profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        client = new ProxmoxClient(profile);

        spinnerType = findViewById(R.id.spinner_storage_type);
        editId = findViewById(R.id.edit_storage_id);
        editServer = findViewById(R.id.edit_storage_server);
        editExport = findViewById(R.id.edit_storage_export);
        editContent = findViewById(R.id.edit_storage_content);
        txtServerLabel = findViewById(R.id.txt_label_storage_server);
        txtExportLabel = findViewById(R.id.txt_label_storage_export);
        progressBar = findViewById(R.id.progress_add_storage);
        btnSave = findViewById(R.id.btn_save_storage);

        findViewById(R.id.btn_storage_add_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, storageTypes);
        spinnerType.setAdapter(adapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = storageTypes.get(position);
                if ("dir".equals(type)) {
                    txtServerLabel.setVisibility(View.GONE);
                    editServer.setVisibility(View.GONE);
                    txtExportLabel.setText("Directory Path");
                    editExport.setHint("/mnt/storage");
                } else if ("zfspool".equals(type)) {
                    txtServerLabel.setVisibility(View.GONE);
                    editServer.setVisibility(View.GONE);
                    txtExportLabel.setText("ZFS Pool Name");
                    editExport.setHint("rpool or data-pool");
                } else {
                    txtServerLabel.setVisibility(View.VISIBLE);
                    editServer.setVisibility(View.VISIBLE);
                    txtExportLabel.setText(type.equals("nfs") ? "NFS Export Path" : "SMB Share Name");
                    editExport.setHint(type.equals("nfs") ? "/volume1/nfs" : "proxmox_share");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStorage();
            }
        });
    }

    private void saveStorage() {
        final String storageId = editId.getText().toString().trim();
        final String type = storageTypes.get(spinnerType.getSelectedItemPosition());
        final String server = editServer.getText().toString().trim();
        final String exportOrPath = editExport.getText().toString().trim();
        final String content = editContent.getText().toString().trim();

        if (storageId.isEmpty()) {
            Toast.makeText(this, "Storage ID is required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.addStorage(storageId, type, server, exportOrPath, content, exportOrPath);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(StorageCreateActivity.this, "Storage pool '" + storageId + "' added!", Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(StorageCreateActivity.this, "Failed adding storage: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}
