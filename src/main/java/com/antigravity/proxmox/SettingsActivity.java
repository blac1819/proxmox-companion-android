package com.antigravity.proxmox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.antigravity.proxmox.storage.AppSettings;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends Activity {
    private AppSettings settings;

    private RadioGroup rgBackend;
    private RadioButton rbBackendGemini;
    private RadioButton rbBackendLocal;
    private View layoutGeminiConfig;
    private View layoutLocalConfig;

    private EditText editGeminiKey;
    private Spinner spinnerModel;
    private EditText editLocalAiUrl;
    private Spinner spinnerPoll;
    private EditText editTimeout;
    private Button btnSave;

    private final List<String> models = Arrays.asList(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash"
    );

    private final List<String> pollIntervals = Arrays.asList(
            "Disabled (Manual refresh only)",
            "Every 5 seconds",
            "Every 10 seconds",
            "Every 30 seconds",
            "Every 60 seconds"
    );
    private final int[] pollSeconds = {0, 5, 10, 30, 60};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = new AppSettings(this);

        rgBackend = findViewById(R.id.rg_ai_backend);
        rbBackendGemini = findViewById(R.id.rb_backend_gemini);
        rbBackendLocal = findViewById(R.id.rb_backend_local);
        layoutGeminiConfig = findViewById(R.id.layout_gemini_config);
        layoutLocalConfig = findViewById(R.id.layout_local_config);

        editGeminiKey = findViewById(R.id.edit_gemini_key);
        spinnerModel = findViewById(R.id.spinner_gemini_model);
        editLocalAiUrl = findViewById(R.id.edit_local_ai_url);
        spinnerPoll = findViewById(R.id.spinner_poll_interval);
        editTimeout = findViewById(R.id.edit_timeout_sec);
        btnSave = findViewById(R.id.btn_save_settings);

        findViewById(R.id.btn_settings_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Setup AI Backend toggle
        String currentBackend = settings.getAiBackend();
        if (AppSettings.BACKEND_LOCAL.equalsIgnoreCase(currentBackend)) {
            rbBackendLocal.setChecked(true);
            layoutGeminiConfig.setVisibility(View.GONE);
            layoutLocalConfig.setVisibility(View.VISIBLE);
        } else {
            rbBackendGemini.setChecked(true);
            layoutGeminiConfig.setVisibility(View.VISIBLE);
            layoutLocalConfig.setVisibility(View.GONE);
        }

        rgBackend.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_backend_local) {
                    layoutGeminiConfig.setVisibility(View.GONE);
                    layoutLocalConfig.setVisibility(View.VISIBLE);
                } else {
                    layoutGeminiConfig.setVisibility(View.VISIBLE);
                    layoutLocalConfig.setVisibility(View.GONE);
                }
            }
        });

        // Setup Model Spinner with supported 2.5 models
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, models);
        spinnerModel.setAdapter(modelAdapter);
        int selectedModelIdx = models.indexOf(settings.getGeminiModel());
        if (selectedModelIdx >= 0) {
            spinnerModel.setSelection(selectedModelIdx);
        } else {
            spinnerModel.setSelection(0); // default gemini-2.5-flash
        }

        // Setup Poll Spinner
        ArrayAdapter<String> pollAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pollIntervals);
        spinnerPoll.setAdapter(pollAdapter);
        int curPoll = settings.getPollIntervalSec();
        for (int i = 0; i < pollSeconds.length; i++) {
            if (pollSeconds[i] == curPoll) {
                spinnerPoll.setSelection(i);
                break;
            }
        }

        editGeminiKey.setText(settings.getGeminiApiKey());
        editLocalAiUrl.setText(settings.getLocalAiUrl());
        editTimeout.setText(String.valueOf(settings.getTimeoutSec()));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        String chosenBackend = rbBackendLocal.isChecked() ? AppSettings.BACKEND_LOCAL : AppSettings.BACKEND_GEMINI;
        settings.setAiBackend(chosenBackend);

        settings.setGeminiApiKey(editGeminiKey.getText().toString());
        settings.setGeminiModel(models.get(spinnerModel.getSelectedItemPosition()));
        settings.setLocalAiUrl(editLocalAiUrl.getText().toString());

        int selectedPoll = pollSeconds[spinnerPoll.getSelectedItemPosition()];
        settings.setPollIntervalSec(selectedPoll);

        int timeout = 15;
        try {
            timeout = Integer.parseInt(editTimeout.getText().toString().trim());
        } catch (NumberFormatException ignored) {}
        settings.setTimeoutSec(timeout);

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
