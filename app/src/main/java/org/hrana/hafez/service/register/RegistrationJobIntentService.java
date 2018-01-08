package org.hrana.hafez.service.register;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;


import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.hrana.hafez.Constants;
import org.hrana.hafez.R;
import org.hrana.hafez.util.DeviceUtil;

/**
 * Generate a new InstanceID/token and save token. This token is sent with reports both
 * as a means of indicating that reports come from an Android device, and as a means of limiting
 * spam submissions.
 * Tokens are not a guarantee of validity and can be revoked/rejected,
 * in particular if they are abused. Submissions with an invalid token may be ignored.
 */
public class RegistrationJobIntentService extends JobIntentService {
    SharedPreferences preferences;

    private static final String TAG = "RegIntentService";

    public static final int JOB_ID = 1011;

    @Override
    public void onCreate() {
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        super.onCreate();
    }

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, RegistrationJobIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            if (!DeviceUtil.hasEmulatorFlags()) {
                final InstanceID instanceId = InstanceID.getInstance(this);
                final String token = instanceId.getToken(getString(R.string.gcm_defaultSenderId), // firebase projectID
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null); //"GCM" or any scope

                preferences.edit()
                        .putString(Constants.CLIENT_TOKEN_KEY, token) // Temporary auth token, can be validated against GCM services by server, and revoked if misused.
                        .apply();
            } else {
                Log.w(TAG, "Device returned true for one or more emulator flags -- no client ID stored.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to save token");
        }
    }

}
