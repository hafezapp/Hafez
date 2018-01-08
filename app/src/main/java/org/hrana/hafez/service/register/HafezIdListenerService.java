package org.hrana.hafez.service.register;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

import static org.hrana.hafez.service.register.RegistrationJobIntentService.enqueueWork;

/**
 * Request a new Instance ID token from the Registration Service.
 */
public class HafezIdListenerService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        Intent refreshIntent = new Intent();
        enqueueWork(this, refreshIntent);
    }

}
