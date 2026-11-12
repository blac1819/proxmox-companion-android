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

    private EditText editGeminiKey;
    private Spinner spinnerModel;
    private Spinner spinnerPoll;
    private EditText editTimeout;
    private Button btnSave;

    private final List<String> models = Arrays.asList(
            "gemini-2.5-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
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

        editGeminiKey = findViewById(R.id.edit_gemini_key);
        spinnerModel = findViewById(R.id.spinner_gemini_model);
        spinnerPoll = findViewById(R.id.spinner_poll_interval);
        editTimeout = findViewById(R.id.edit_timeout_sec);
        btnSave = findViewById(R.id.btn_save_settings);

        findViewById(R.id.btn_settings_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Setup Model Spinner
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, models);
        spinnerModel.setAdapter(modelAdapter);
        int selectedModelIdx = models.indexOf(settings.getGeminiModel());
        if (selectedModelIdx >= 0) spinnerModel.setSelection(selectedModelIdx);

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
        editTimeout.setText(String.valueOf(settings.getTimeoutSec()));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        settings.setGeminiApiKey(editGeminiKey.getText().toString());
        settings.setGeminiModel(models.get(spinnerModel.getSelectedItemPosition()));

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
