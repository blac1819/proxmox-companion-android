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

        txtUrl.setText(currentUrl);

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
