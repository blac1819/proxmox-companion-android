package com.antigravity.proxmox.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.antigravity.proxmox.R;
import com.antigravity.proxmox.api.GeminiClient;
import com.antigravity.proxmox.storage.AppSettings;

public class AiAssistantDialog {

    public static void show(final Activity activity, final String defaultPrompt, final String contextInfo) {
        if (activity == null || activity.isFinishing()) return;

        final View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_ai_assistant, null);
        final EditText editPrompt = dialogView.findViewById(R.id.edit_ai_prompt);
        final TextView txtResponse = dialogView.findViewById(R.id.txt_ai_response);
        final ProgressBar progressBar = dialogView.findViewById(R.id.progress_ai);
        final Button btnAsk = dialogView.findViewById(R.id.btn_ai_ask);

        final AppSettings settings = new AppSettings(activity);

        if (defaultPrompt != null && !defaultPrompt.trim().isEmpty()) {
            editPrompt.setText(defaultPrompt);
        } else {
            editPrompt.setHint("e.g. How to install Docker in LXC, fix storage ZFS error, or generate qm command...");
        }

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🤖 Proxmox AI Assistant")
                .setView(dialogView)
                .setPositiveButton("Copy Response", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String resp = txtResponse.getText().toString();
                        if (!resp.isEmpty()) {
                            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                            if (cm != null) {
                                cm.setPrimaryClip(ClipData.newPlainText("AI Response", resp));
                                Toast.makeText(activity, "Response copied to clipboard", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .setNegativeButton("Close", null)
                .create();

        btnAsk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String prompt = editPrompt.getText().toString().trim();
                if (prompt.isEmpty()) return;

                progressBar.setVisibility(View.VISIBLE);
                btnAsk.setEnabled(false);
                txtResponse.setText("Generating response with AI...");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        GeminiClient gemini = new GeminiClient(
                                settings.getGeminiApiKey(),
                                settings.getGeminiModel(),
                                settings.getAiBackend(),
                                settings.getLocalAiUrl()
                        );
                        try {
                            final String answer = gemini.askAssistant(prompt, contextInfo != null ? contextInfo : "");
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    btnAsk.setEnabled(true);
                                    txtResponse.setText(answer);
                                }
                            });
                        } catch (final Exception e) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    btnAsk.setEnabled(true);
                                    txtResponse.setText("Error: " + e.getMessage());
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        dialog.show();
    }
}
