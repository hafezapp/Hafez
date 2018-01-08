package org.hrana.hafez.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import org.hrana.hafez.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.hrana.hafez.Constants.ABOUT_US_HTML;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(R.string.about);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView aboutView = findViewById(R.id.about_textView);
        StringBuilder s = new StringBuilder();
        String line;
        try {
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader
                            (this.getAssets().open(ABOUT_US_HTML)));
            while ((line = reader.readLine()) != null) {
                s.append(line);
            }
        } catch (IOException ex) {
            Log.e("EUA", "Error opening EUA");
        }

        // format HTML
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            aboutView.setText(Html.fromHtml(s.toString(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            aboutView.setText(Html.fromHtml(s.toString()));
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
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
