package org.hrana.hafez.di.module;

import org.hrana.hafez.net.IApiEndpoint;

import dagger.Module;
import okhttp3.RequestBody;
import retrofit2.http.Field;
import retrofit2.http.Part;
import rx.Observable;

/**
 * Test module
 */
@Module
public class StubNetModule extends NetModule {
    private static final  String STUB_URL = "https://my.mockurl.org";

    public StubNetModule() {
        super(STUB_URL);
    }


    /*
     * Stub endpoint class to test setup of observables, mocking out network calls.
     */
    private class StubEndpoint implements IApiEndpoint {
        @Override
        public Observable<String> postEncryptedAttachment(@Part("client_version") RequestBody clientVersion, @Part("submission_time") RequestBody time, @Part("encryption_key_id") RequestBody encryptionKeyId, @Part("security_token") RequestBody token, @Part("encrypted_blob") RequestBody blob, @Part("attachment_data\"; filename=\"attachment_data\"") RequestBody attachment) {
            return Observable.just("CREATED");
        }

        @Override
        public Observable<String> postEncryptedReport(@Field("client_version") String clientVersion, @Field("submission_time") String time, @Field("encryption_key_id") String encryptionKeyId, @Field("security_token") String token, @Field("encrypted_blob") String blob) {
            return Observable.just("CREATED");
        }
    }

}
