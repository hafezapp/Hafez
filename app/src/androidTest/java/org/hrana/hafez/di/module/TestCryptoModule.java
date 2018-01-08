package org.hrana.hafez.di.module;

import com.squareup.moshi.JsonAdapter;

import org.hrana.hafez.di.scope.ReportScope;
import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.presenter.CryptoPresenter;
import org.libsodium.jni.keys.PublicKey;
import org.libsodium.jni.keys.SigningKey;


import dagger.Module;
import dagger.Provides;

/**
 * Test module
 */
@Module
public class TestCryptoModule extends CryptoModule {
    private PublicKey publicKey;

    public TestCryptoModule(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Provides @ReportScope
    CryptoPresenter providecryptopresenter(JsonAdapter<Report> jsonAdapter,
                                           JsonAdapter<Attachment> metadataAdapter,
                                           SigningKey signingKey) {
        return new CryptoPresenter(jsonAdapter, metadataAdapter, publicKey, signingKey);
    }

}
