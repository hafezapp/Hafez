package org.hrana.hafez.di.component;

import android.content.ContentResolver;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.hrana.hafez.di.module.SubmissionModule;
import org.hrana.hafez.di.scope.ReportScope;
import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.net.IApiEndpoint;
import org.hrana.hafez.net.MetadataAdapter;
import org.hrana.hafez.net.ReportAdapter;
import org.hrana.hafez.presenter.contract.ICryptoPresenter;
import org.hrana.hafez.presenter.contract.IMediaPresenter;
import org.hrana.hafez.presenter.contract.IReportSubmissionPresenter;
import org.hrana.hafez.view.fragment.ReportSubmissionFragment;
import org.libsodium.jni.keys.PublicKey;
import org.libsodium.jni.keys.SigningKey;

import javax.inject.Named;

import dagger.Component;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

/**
 * Expose dependencies of SubmissionModule
 */

@Component(modules = {SubmissionModule.class}, dependencies = {IApplicationComponent.class})
@ReportScope
public interface ISubmissionComponent {
    ICryptoPresenter cryptoPresenter();
    IReportSubmissionPresenter reportSubmissionPresenter();
    IMediaPresenter mediaPresenter();
    ContentResolver contentResolver();
    Retrofit retrofit();
    RxJavaCallAdapterFactory factory();
    IApiEndpoint endpoint();
    ReportAdapter reportAdapter();
    JsonAdapter<Report> jsonAdapter();
    MetadataAdapter mediaAdapter();
    JsonAdapter<Attachment> jsonAttachmentAdapter();
    SigningKey key();
    @Named("EncryptReport") Moshi encryptionMoshi();
    @Named("SendBlob") Moshi submissionMoshi();
    PublicKey publicKey();
    OkHttpClient client();

    void inject(ReportSubmissionFragment fragment);

}
