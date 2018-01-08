package org.hrana.hafez.service.register;

import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Stub GCM Listener service class.
 * At the moment the app does not act on push messages, it only registers for the purpose of validating tokens.
 */
public class HafezGcmListenerService extends GcmListenerService {
    @Override
    public void onMessageReceived(String sender, Bundle bundle) {
        super.onMessageReceived(sender, bundle);
    }
}
