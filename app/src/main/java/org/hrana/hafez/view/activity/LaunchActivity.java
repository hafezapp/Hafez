package org.hrana.hafez.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Launch {@link MainActivity}. {@link MainActivity} requires history (android:noHistory=false)
 * to be able to import and capture media, but in order to insure that a half-finished report is
 * not recreated if the user presses the HOME screen and then re-launches the app, a launcher activity
 * which
 */
public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent it = new Intent(this, MainActivity.class);
        startActivity(it);
        this.finish();
    }
}
