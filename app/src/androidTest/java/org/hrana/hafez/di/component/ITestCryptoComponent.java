package org.hrana.hafez.di.component;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.hrana.hafez.di.module.TestCryptoModule;
import org.hrana.hafez.di.scope.ReportScope;
import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.presenter.CryptoPresenter;
import org.hrana.hafez.nacl.NaclCryptoPresenterTest;
import org.hrana.hafez.net.MetadataAdapter;
import org.hrana.hafez.net.ReportAdapter;
import org.libsodium.jni.keys.SigningKey;

import javax.inject.Named;

import dagger.Component;

/**
 * Test component
 */

@Component(modules = {TestCryptoModule.class})
@ReportScope
public interface ITestCryptoComponent {
    CryptoPresenter cryptoPresenter();

    ReportAdapter reportAdapter();
    JsonAdapter<Report> jsonAdapter();
    MetadataAdapter mediaAdapter();
    JsonAdapter<Attachment> jsonAttachmentAdapter();
    SigningKey key();
    @Named("EncryptReport")
    Moshi encryptionMoshi();

    void inject(NaclCryptoPresenterTest test);
}
