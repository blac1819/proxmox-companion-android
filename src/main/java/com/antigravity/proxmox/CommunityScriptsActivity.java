package com.antigravity.proxmox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.antigravity.proxmox.api.GeminiClient;
import com.antigravity.proxmox.model.CommunityScript;
import com.antigravity.proxmox.storage.AppSettings;
import com.antigravity.proxmox.storage.CommunityScriptCatalog;
import java.util.Arrays;
import java.util.List;

public class CommunityScriptsActivity extends Activity {
    private EditText editSearch;
    private Spinner spinnerCategory;
    private LinearLayout layoutList;
    private AppSettings settings;

    private final List<String> categories = Arrays.asList("All", "LXC", "VM", "Tools");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_scripts);

        settings = new AppSettings(this);

        editSearch = findViewById(R.id.edit_search_scripts);
        spinnerCategory = findViewById(R.id.spinner_category);
        layoutList = findViewById(R.id.layout_scripts_list);

        findViewById(R.id.btn_scripts_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btn_scripts_ai).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAiAssistantDialog(null);
            }
        });

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterAndRender();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndRender();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        filterAndRender();
    }

    private void filterAndRender() {
        layoutList.removeAllViews();
        String query = editSearch.getText().toString();
        String cat = categories.get(spinnerCategory.getSelectedItemPosition());
        List<CommunityScript> scripts = CommunityScriptCatalog.filter(query, cat);

        if (scripts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No helper scripts match your filter.");
            empty.setTextColor(0xFF888888);
            empty.setPadding(30, 60, 30, 30);
            empty.setGravity(android.view.Gravity.CENTER);
            layoutList.addView(empty);
            return;
        }

        for (final CommunityScript script : scripts) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_card_script, layoutList, false);

            TextView txtTitle = card.findViewById(R.id.txt_script_title);
            TextView badgeCat = card.findViewById(R.id.badge_script_category);
            TextView txtDesc = card.findViewById(R.id.txt_script_desc);
            TextView txtCmd = card.findViewById(R.id.txt_script_command);
            Button btnCopy = card.findViewById(R.id.btn_script_copy);
            Button btnExplain = card.findViewById(R.id.btn_script_explain);
            Button btnDocs = card.findViewById(R.id.btn_script_docs);

            txtTitle.setText(script.getName());
            badgeCat.setText(script.getCategory());
            txtDesc.setText(script.getDescription());
            txtCmd.setText(script.getCommand());

            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Proxmox Command", script.getCommand());
                    cm.setPrimaryClip(clip);
                    Toast.makeText(CommunityScriptsActivity.this, "Command copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            });

            btnExplain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAiAssistantDialog(script);
                }
            });

            btnDocs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (script.getDocumentationUrl() != null && !script.getDocumentationUrl().isEmpty()) {
                        Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(script.getDocumentationUrl()));
                        startActivity(browser);
                    }
                }
            });

            layoutList.addView(card);
        }
    }

    private void showAiAssistantDialog(final CommunityScript script) {
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_assistant, null);
        final EditText editPrompt = dialogView.findViewById(R.id.edit_ai_prompt);
        final TextView txtResponse = dialogView.findViewById(R.id.txt_ai_response);
        final ProgressBar progressBar = dialogView.findViewById(R.id.progress_ai);
        final Button btnAsk = dialogView.findViewById(R.id.btn_ai_ask);

        if (script != null) {
            editPrompt.setText("What does the " + script.getName() + " helper script do and what are its system prerequisites?");
        } else {
            editPrompt.setHint("e.g. How do I configure PCI passthrough or fix a failed ZFS scrub?");
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🤖 Proxmox AI Terminal Assistant")
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        btnAsk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String prompt = editPrompt.getText().toString().trim();
                if (prompt.isEmpty()) return;

                progressBar.setVisibility(View.VISIBLE);
                btnAsk.setEnabled(false);
                txtResponse.setText("Consulting Gemini AI...");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        GeminiClient gemini = new GeminiClient(settings.getGeminiApiKey(), settings.getGeminiModel());
                        String context = script != null ? "Script: " + script.getName() + "\nCommand: " + script.getCommand() : "";
                        try {
                            final String answer = gemini.askAssistant(prompt, context);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    btnAsk.setEnabled(true);
                                    txtResponse.setText(answer);
                                }
                            });
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
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
