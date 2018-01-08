package org.hrana.hafez.view.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import org.hrana.hafez.R;
import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.di.component.DaggerIPresenterComponent;
import org.hrana.hafez.di.module.PresenterModule;

import javax.inject.Inject;


/**
 * App settings
 */

public class SettingsActivity extends BaseActivity {
    @Inject SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings);

        // DI
        DaggerIPresenterComponent.builder()
                .iApplicationComponent(BaseApplication.get(this).getComponent())
                .presenterModule(new PresenterModule())
                .build()
                .inject(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);


        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new FirstPreferenceFragment())
                .commit();

    }

    public static class FirstPreferenceFragment extends PreferenceFragment
            implements Preference.OnPreferenceClickListener,
            Preference.OnPreferenceChangeListener {
        @Inject
        SharedPreferences prefs;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_fragment_first);
            // DI
            DaggerIPresenterComponent.builder()
                    .iApplicationComponent(BaseApplication.get(getActivity()).getComponent())
                    .presenterModule(new PresenterModule())
                    .build()
                    .inject(this);

            Preference onboardingPref = findPreference(getString(R.string.onboarding_complete_key));
            onboardingPref.setOnPreferenceClickListener(this);
            Preference uninstallPref = findPreference(getString(R.string.uninstall_options_key));
            uninstallPref.setOnPreferenceClickListener(this);

            Preference[] explainPrefs = new Preference[]{findPreference(getString(R.string.restrict_access_tor_key)),
                    findPreference(getString(R.string.upload_over_data_key)),
                    findPreference(getString(R.string.report_timeout_key))};
            for (Preference p : explainPrefs) {
                if (p != null) {
                    p.setOnPreferenceChangeListener(this);
                }
            }

        }


        @Override
        public boolean onPreferenceClick(final Preference preference) {
            if (preference.getKey().equals(getString(R.string.onboarding_complete_key))) {
                prefs.edit()
                        .putBoolean(getString(R.string.onboarding_complete_key), false)
                        .apply();
                Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                startActivity(mainIntent);
                getActivity().finish();
                return true;
            } else if (preference.getKey().equals(getString(R.string.uninstall_options_key))) {
                int which = prefs.getInt(getString(R.string.uninstall_options_value), 0);
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.uninstall_options))
                        .setSingleChoiceItems(R.array.array_uninstall_options, which,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        prefs.edit().putInt(getString(R.string.uninstall_options_value), which).apply();
                                        dialog.dismiss();
                                    }
                                })
                        .create()
                        .show();
                return true;
            }
            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean showDialog = false;
            String title = null, message = null;

            if (preference.getKey().equals
                    (getString(R.string.upload_over_data_key))
                    && !prefs.getBoolean(preference.getKey(), false)) {
                showDialog = true;
                title = getString(R.string.upload_over_data);
                message = getString(R.string.upload_over_data_explanation);
            }
            // Disabled for now--we aren't using these pereferences currently.

//            } else if (preference.getKey().equals(getString(R.string.enable_gallery_storage_mode_key))
//                && prefs.getBoolean(preference.getKey(), true)) {
//                showDialog = true;
//                title = getString(R.string.title_disable_gallery_storage);
//                message = getString(R.string.message_disable_gallery_storage);

//            else if (preference.getKey().equals(getString(R.string.report_timeout_key)) // not currently in use
//                    && !prefs.getBoolean(preference.getKey(), false)) {
//                showDialog = true;
//                title = getString(R.string.report_timeout);
//                message = getString(R.string.timeout_detail_explanation);
//            } else if (preference.getKey().equals
//                    (getString(R.string.restrict_access_tor_key)) //  not in use.
//                    && !prefs.getBoolean(preference.getKey(), false)) {
//                showDialog = true;
//                title = getString(R.string.restrict_access_tor);
//                message = getString(R.string.restrict_access_tor_explanation);


            if (showDialog && (title != null && message != null)) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setMessage(message)
                        .setNeutralButton(getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            }
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (checkCompletedPreferences()) {
                showWarningDialog(new DialogClosedCallback() {
                    @Override
                    public void onDismiss() {
                        startHomeActivity();
                    }
                });
            } else {
                startHomeActivity();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void startHomeActivity() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (checkCompletedPreferences()) {
            showWarningDialog(new DialogClosedCallback() {
                @Override
                public void onDismiss() {
                    SettingsActivity.super.onBackPressed();
                }
            });
        } else {
            SettingsActivity.super.onBackPressed();
        }
    }

    private void showWarningDialog(final DialogClosedCallback callback) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.report_timeout))
                .setMessage(getString(R.string.error_timeout_default_time))
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit()
                                .putString(getString(R.string.report_timeout_value_key), "10")
                                .apply();
                        dialog.dismiss();
                        callback.onDismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();

    }

    private boolean checkCompletedPreferences() {
        return (prefs.getBoolean(getString(R.string.report_timeout_key), false) &&
                prefs.getString(getString(R.string.report_timeout_value_key), "None").equals("None"));
    }

    private interface DialogClosedCallback {
        void onDismiss();
    }
}
