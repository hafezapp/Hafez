package org.hrana.hafez.view.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.hrana.hafez.Constants;
import org.hrana.hafez.R;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Fragment to display legal case content material.
 * Populates a WebView with locally-stored file to display information.
 */
public class LegalCaseFragment extends Fragment {
    @BindView(R.id.web_view) WebView webView;
    private Unbinder unbinder;

    public static LegalCaseFragment newInstance() {
        return new LegalCaseFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_legal_cases, container, false);
        unbinder = ButterKnife.bind(this, view);

        // Set up webview
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        webView.setPadding(0,0,0,0);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);

        // Allowing javascript in webview in order to properly display content.
        // Note re XSS: webpage to display is local page loaded from file,
        // and there are no outbound links in the content.
        webView.getSettings().setJavaScriptEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(false);
        }

        // If for some reason there were any links, make sure to open them in a new browser.
        // This only applies to links clicked within the webview.
        WebViewClient restrictiveClient = new WebViewClient() {

            // Android M and below
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (isInternalUrl(url)) {
                    return false;
                } else {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
            }

            // Android N and above only
            @RequiresApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (isInternalUrl(request.getUrl().toString())) {
                    return false; // load this url in current webview (it's ours)
                } else {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    startActivity(intent);
                    return true;
                }
            }

            // Check if url is pointing to internal resource
            private boolean isInternalUrl(String url) {
                return (url.startsWith("file:///android_asset/web")
                        || url.startsWith("file://" + getActivity().getFilesDir().getAbsolutePath() + "/" + Constants.DOWNLOAD_CASE_HISTORY_KEY)) && !url.contains("../");
            }
        };

        webView.setWebViewClient(restrictiveClient);

        File file = new File(getActivity().getFilesDir() + "/" + Constants.DOWNLOAD_CASE_HISTORY_KEY, "index.html");

        if (!file.exists()) { // Can't find the files we downloaded; use the backup, which is a local version but may not have most up-to-date content.
            webView.loadUrl("file:///android_asset/web/index.html");
        } else {
            // load the url
            webView.loadUrl("file://" + file.getAbsolutePath());
        }

        return view;
    }


    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

}
