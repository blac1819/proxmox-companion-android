package com.antigravity.proxmox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class ServerEditActivity extends Activity {
    public static final String EXTRA_SERVER_ID = "server_id";

    private ProfileManager profileManager;
    private ServerProfile currentProfile;
    private boolean isEditing = false;

    private EditText editName;
    private EditText editHost;
    private EditText editPort;
    private RadioGroup rgAuthMode;
    private RadioButton rbAuthToken;
    private RadioButton rbAuthPassword;
    private View layoutTokenFields;
    private View layoutPasswordFields;
    private EditText editTokenUser;
    private EditText editTokenId;
    private EditText editTokenSecret;
    private EditText editUsername;
    private EditText editRealm;
    private EditText editPassword;
    private CheckBox cbTrustSsl;
    private Button btnTest;
    private TextView txtTestResult;
    private Button btnSave;
    private Button btnDelete;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_edit);

        profileManager = new ProfileManager(this);

        editName = findViewById(R.id.edit_name);
        editHost = findViewById(R.id.edit_host);
        editPort = findViewById(R.id.edit_port);
        rgAuthMode = findViewById(R.id.rg_auth_mode);
        rbAuthToken = findViewById(R.id.rb_auth_token);
        rbAuthPassword = findViewById(R.id.rb_auth_password);
        layoutTokenFields = findViewById(R.id.layout_token_fields);
        layoutPasswordFields = findViewById(R.id.layout_password_fields);
        editTokenUser = findViewById(R.id.edit_token_user);
        editTokenId = findViewById(R.id.edit_token_id);
        editTokenSecret = findViewById(R.id.edit_token_secret);
        editUsername = findViewById(R.id.edit_username);
        editRealm = findViewById(R.id.edit_realm);
        editPassword = findViewById(R.id.edit_password);
        cbTrustSsl = findViewById(R.id.cb_trust_ssl);
        btnTest = findViewById(R.id.btn_test_connection);
        txtTestResult = findViewById(R.id.txt_test_result);
        btnSave = findViewById(R.id.btn_save_server);
        btnDelete = findViewById(R.id.btn_delete_server);

        TextView txtTitle = findViewById(R.id.txt_edit_title);

        String serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        if (serverId != null && !serverId.isEmpty()) {
            currentProfile = profileManager.getProfileById(serverId);
            if (currentProfile != null) {
                isEditing = true;
                txtTitle.setText(R.string.title_edit_server);
                btnDelete.setVisibility(View.VISIBLE);
                populateFields(currentProfile);
            }
        }

        if (currentProfile == null) {
            currentProfile = new ServerProfile();
        }

        rgAuthMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_auth_token) {
                    layoutTokenFields.setVisibility(View.VISIBLE);
                    layoutPasswordFields.setVisibility(View.GONE);
                } else {
                    layoutTokenFields.setVisibility(View.GONE);
                    layoutPasswordFields.setVisibility(View.VISIBLE);
                }
            }
        });

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void populateFields(ServerProfile p) {
        editName.setText(p.getName());
        editHost.setText(p.getHost());
        editPort.setText(String.valueOf(p.getPort()));

        if (ServerProfile.AUTH_PASSWORD.equals(p.getAuthMode())) {
            rbAuthPassword.setChecked(true);
            layoutTokenFields.setVisibility(View.GONE);
            layoutPasswordFields.setVisibility(View.VISIBLE);
        } else {
            rbAuthToken.setChecked(true);
            layoutTokenFields.setVisibility(View.VISIBLE);
            layoutPasswordFields.setVisibility(View.GONE);
        }

        editTokenUser.setText(p.getTokenUser());
        editTokenId.setText(p.getTokenId());
        editTokenSecret.setText(p.getTokenSecret());

        editUsername.setText(p.getUsername());
        editRealm.setText(p.getRealm());
        editPassword.setText(p.getPassword());

        cbTrustSsl.setChecked(p.isTrustAllSsl());
    }

    private ServerProfile buildProfileFromFields() {
        ServerProfile p = new ServerProfile();
        if (isEditing && currentProfile != null) {
            p.setId(currentProfile.getId());
        }

        p.setName(editName.getText().toString().trim());
        p.setHost(editHost.getText().toString().trim());

        String portStr = editPort.getText().toString().trim();
        int port = 8006;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ignored) {}
        p.setPort(port);

        if (rbAuthPassword.isChecked()) {
            p.setAuthMode(ServerProfile.AUTH_PASSWORD);
            p.setUsername(editUsername.getText().toString().trim());
            p.setRealm(editRealm.getText().toString().trim());
            p.setPassword(editPassword.getText().toString());
        } else {
            p.setAuthMode(ServerProfile.AUTH_TOKEN);
            p.setTokenUser(editTokenUser.getText().toString().trim());
            p.setTokenId(editTokenId.getText().toString().trim());
            p.setTokenSecret(editTokenSecret.getText().toString().trim());
        }

        p.setTrustAllSsl(cbTrustSsl.isChecked());
        return p;
    }

    private void testConnection() {
        final ServerProfile tempProfile = buildProfileFromFields();
        if (tempProfile.getHost().isEmpty()) {
            Toast.makeText(this, "Please enter a valid Host/IP", Toast.LENGTH_SHORT).show();
            return;
        }

        txtTestResult.setVisibility(View.VISIBLE);
        txtTestResult.setText("Connecting to " + tempProfile.getBaseUrl() + "...");
        txtTestResult.setTextColor(0xFFCCCCCC);
        btnTest.setEnabled(false);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                ProxmoxClient client = new ProxmoxClient(tempProfile);
                try {
                    final String version = client.testConnection();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            btnTest.setEnabled(true);
                            txtTestResult.setText("✓ Connection successful!\n" + version);
                            txtTestResult.setTextColor(0xFF2ECC71);
                        }
                    });
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            btnTest.setEnabled(true);
                            txtTestResult.setText("✗ Connection failed:\n" + e.getMessage());
                            txtTestResult.setTextColor(0xFFE74C3C);
                        }
                    });
                }
            }
        });
    }

    private void saveProfile() {
        ServerProfile profile = buildProfileFromFields();
        if (profile.getHost().isEmpty()) {
            Toast.makeText(this, "Please enter Host/IP address", Toast.LENGTH_SHORT).show();
            return;
        }

        profileManager.saveProfile(profile);
        profileManager.setActiveProfileId(profile.getId());
        Toast.makeText(this, "Server profile saved", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_title)
                .setMessage(getString(R.string.confirm_delete_server_msg, currentProfile.getName()))
                .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        profileManager.deleteProfile(currentProfile.getId());
                        Toast.makeText(ServerEditActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
