package org.hrana.hafez.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.hrana.hafez.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.hrana.hafez.Constants.EULA_HTML;

/**
 * User agreement.
 */

public class EuaActivity extends BaseActivity {
    private boolean isBackEnabledAction;

    @BindView(R.id.accept)
    Button accept;
    @BindView(R.id.reject) Button reject;
    @BindView(R.id.eua_textbox)
    TextView fullText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eua);
        setTitle(R.string.user_license_agreement);

        ButterKnife.bind(this);
        StringBuilder s = new StringBuilder();
        String line;
        try {
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader
                            (this.getAssets().open(EULA_HTML)));
            while ((line = reader.readLine()) != null) {
                s.append(line);
            }
        } catch (IOException ex) {
            Log.e("EUA", "Error opening EUA");
        }

        // format HTML
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            fullText.setText(Html.fromHtml(s.toString(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            fullText.setText(Html.fromHtml(s.toString()));
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.eua_accepted_key), false)) {
            isBackEnabledAction = true;
        }

        if (isBackEnabledAction) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @OnClick(R.id.reject)
    public void rejectEua() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(getString(R.string.eua_accepted_key), false)
                .apply();
        try {
            Toast.makeText(this, getString(R.string.eua_rejected), Toast.LENGTH_SHORT).show();
            Thread.sleep(400);
            finish();
        } catch (InterruptedException ex) {
            // It was not important to wait, it was only to be less jarring.
            finish();
        }
    }

    @OnClick(R.id.accept)
    public void acceptEua() {
        Intent intent = new Intent(this, MainActivity.class);
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(getString(R.string.eua_accepted_key), true)
                .apply();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        this.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isBackEnabledAction && (item.getItemId() == android.R.id.home)) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
