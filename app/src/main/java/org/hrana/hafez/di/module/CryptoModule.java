package org.hrana.hafez.di.module;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.hrana.hafez.di.scope.ReportScope;
import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.net.MetadataAdapter;
import org.hrana.hafez.net.ReportAdapter;
import org.libsodium.jni.keys.PublicKey;
import org.libsodium.jni.keys.SigningKey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Crypto dependency module.
 */

@Module
public class CryptoModule {
    private static final String TAG = "CryptoModule", KEY_FILE = "key.pub";

    // Only used to convert POJOs to Json for encryption; not used for POSTing
    // (see @Named("SendBlob") Moshi)
    @Provides @ReportScope
    @Named("EncryptReport")
    Moshi moshi() {
        return new Moshi.Builder()
                .add(new ReportAdapter())
                .add(new MetadataAdapter())
                .build();
    }

    @Provides @ReportScope
    SigningKey signingKey() {
        return new SigningKey();
    }

    @Provides @ReportScope
    PublicKey provideServerPubKey(Application application) throws RuntimeException {
        String pubKey = null;
        Context context = application.getApplicationContext();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader
                    (new InputStreamReader(context.getAssets()
                            .open(KEY_FILE)));
            String line = reader.readLine();
            Pattern pattern = Pattern.compile("([0-9a-fA-F" +
                    "]+)"); // Hex string
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                pubKey = matcher.group(1);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error reading server public key");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                Log.e(TAG, "Error closing reader");
            }
        }

        // If couldn't parse public encryption_key, error
        if (pubKey == null || TextUtils.isEmpty(pubKey)) {
            throw new RuntimeException(TAG + ": " + "Failed to load server public encryption_key");
        } else {
            return new PublicKey(pubKey);
        }
    }

    @Provides @ReportScope
    ReportAdapter provideReportAdapter() {
        return new ReportAdapter();
    }

    @Provides @ReportScope
    JsonAdapter<Report> provideReportJsonAdapter(@Named("EncryptReport") Moshi moshi) {
        return moshi.adapter(Report.class);
    }

    @Provides @ReportScope
    MetadataAdapter provideAttachmentAdapter() { return new MetadataAdapter(); }

    @Provides @ReportScope
    JsonAdapter<Attachment> provideMediaAttachmentAdapter(@Named("EncryptReport") Moshi moshi) {
        return moshi.adapter(Attachment.class);
    }

    @Provides @ReportScope
    ContentResolver provideContentResolver(Application application) {
        return application.getContentResolver();
    }

}
