package com.antigravity.proxmox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.antigravity.proxmox.model.ServerProfile;
import com.antigravity.proxmox.storage.ProfileManager;

public class ConsoleActivity extends Activity {
    public static final String EXTRA_URL = "console_url";

    private WebView webView;
    private ProgressBar progressBar;
    private TextView txtUrl;
    private String currentUrl;
    private boolean trustAllSsl = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        currentUrl = getIntent().getStringExtra(EXTRA_URL);
        ProfileManager pm = new ProfileManager(this);
        ServerProfile profile = pm.getActiveProfile();
        if (profile != null) {
            trustAllSsl = profile.isTrustAllSsl();
            if (currentUrl == null || currentUrl.isEmpty()) {
                currentUrl = profile.getBaseUrl() + "/";
            }
        }

        webView = findViewById(R.id.webview_console);
        progressBar = findViewById(R.id.progress_console);
        txtUrl = findViewById(R.id.txt_console_url);

        Button btnBack = findViewById(R.id.btn_console_back);
        Button btnRefresh = findViewById(R.id.btn_console_refresh);
        Button btnExternal = findViewById(R.id.btn_console_external);
        Button btnAi = findViewById(R.id.btn_console_ai);

        txtUrl.setText(currentUrl);

        btnAi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAiDialog();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        btnExternal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl() != null ? webView.getUrl() : currentUrl));
                startActivity(browserIntent);
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (trustAllSsl) {
                    handler.proceed(); // Allow self-signed certificates used by Proxmox VE
                } else {
                    super.onReceivedSslError(view, handler, error);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                txtUrl.setText(url);
            }
        });

        if (currentUrl != null && !currentUrl.isEmpty()) {
            webView.loadUrl(currentUrl);
        }
    }

    private void showAiDialog() {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_ai_assistant, null);
        final EditText editPrompt = dialogView.findViewById(R.id.edit_ai_prompt);
        final TextView txtResponse = dialogView.findViewById(R.id.txt_ai_response);
        final ProgressBar progressBar = dialogView.findViewById(R.id.progress_ai);
        final Button btnAsk = dialogView.findViewById(R.id.btn_ai_ask);

        editPrompt.setHint("e.g. Generate command to install Caddy, or fix failed package manager lock");

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("🤖 Terminal AI Assistant")
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
                txtResponse.setText("Generating command / solution...");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        com.antigravity.proxmox.storage.AppSettings settings = new com.antigravity.proxmox.storage.AppSettings(ConsoleActivity.this);
                        com.antigravity.proxmox.api.GeminiClient gemini = new com.antigravity.proxmox.api.GeminiClient(settings.getGeminiApiKey(), settings.getGeminiModel());
                        try {
                            final String answer = gemini.askAssistant(prompt, "Active Console URL: " + currentUrl);
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

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.destroy();
        }
        super.onDestroy();
    }
}
