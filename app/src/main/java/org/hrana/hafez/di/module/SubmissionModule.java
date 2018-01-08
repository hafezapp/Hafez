package org.hrana.hafez.di.module;


import android.app.Application;
import android.content.ContentResolver;

import com.squareup.moshi.JsonAdapter;

import org.hrana.hafez.di.scope.ReportScope;
import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.presenter.CryptoPresenter;
import org.hrana.hafez.net.IApiEndpoint;
import org.hrana.hafez.presenter.contract.ICryptoPresenter;
import org.hrana.hafez.presenter.contract.IMediaPresenter;
import org.hrana.hafez.presenter.contract.IReportSubmissionPresenter;
import org.hrana.hafez.presenter.MediaPresenter;
import org.hrana.hafez.presenter.ReportSubmissionPresenter;
import org.libsodium.jni.keys.PublicKey;
import org.libsodium.jni.keys.SigningKey;


import dagger.Module;
import dagger.Provides;

/**
 * Module to combine NetModule and CryptoModule dependencies
 */
@Module(includes = {NetModule.class, CryptoModule.class})
public class SubmissionModule {

    @Provides @ReportScope
    ICryptoPresenter provideCryptoPresenter(JsonAdapter<Report> jsonAdapter,
                                            JsonAdapter<Attachment> metadataAdapter,
                                            PublicKey serverKey,
                                            SigningKey signingKey) {
        return new CryptoPresenter(jsonAdapter, metadataAdapter, serverKey, signingKey);
    }

    @Provides @ReportScope
    IReportSubmissionPresenter provideReportPresenter(Application application,
                                                      IApiEndpoint endpoint,
                                                      ICryptoPresenter cryptoPresenter,
                                                      IMediaPresenter mediaPresenter) {
        return new ReportSubmissionPresenter(application.getApplicationContext(), endpoint, cryptoPresenter, mediaPresenter);
    }

    @Provides @ReportScope
    IMediaPresenter provideMediaPresenter(ContentResolver contentResolver) {
        return new MediaPresenter(contentResolver);
    }

}
