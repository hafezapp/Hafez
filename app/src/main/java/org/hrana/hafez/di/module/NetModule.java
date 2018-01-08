package org.hrana.hafez.di.module;

import android.app.Application;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.moshi.Moshi;

import org.hrana.hafez.BuildConfig;
import org.hrana.hafez.R;
import org.hrana.hafez.di.scope.ReportScope;
import org.hrana.hafez.net.IApiEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import dagger.Module;
import dagger.Provides;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.schedulers.Schedulers;

import static org.hrana.hafez.Constants.PROD_HOSTNAME;
import static org.hrana.hafez.Constants.SELF_SIGNED_CERT;

/**
 * Net module dependencies.
 */
@Module
public class NetModule {
    private static final String TAG = "NetModule";
    private String baseUrl;
    private OkHttpClient client;

    public NetModule(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Provides @ReportScope @Named("SendBlob")
    Moshi provideMoshi() {
        return new Moshi.Builder()
                .build();
    }

    @Provides @ReportScope
    Retrofit provideRetrofit(@Named("SendBlob") Moshi moshi, OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build();
    }

    @Provides @ReportScope
    RxJavaCallAdapterFactory provideRxAdapterFactory() {
        return RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
    }

    @Provides @ReportScope
    IApiEndpoint provideApiEndpoint(Retrofit retrofit) {
        return retrofit.create(IApiEndpoint.class);
    }

    public void setClient(OkHttpClient newClient) {
        this.client = newClient;
    }

    @Provides
    @ReportScope
    OkHttpClient provideOkHttpClient(Application application) throws IllegalStateException {
        if (client != null) {
            return client;

        } else {
            if (PreferenceManager.getDefaultSharedPreferences
                    (application.getApplicationContext()).getBoolean(SELF_SIGNED_CERT, false)) {

                // Self-signed certificate
                InputStream inputCert = null;
                Certificate certificate;
                try {
                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509"); // or whatever type
                    inputCert = application.getApplicationContext()
                            .getResources().openRawResource(R.raw.cert);
                    certificate = certFactory.generateCertificate(inputCert);

                    // Keystore for certificate
                    KeyStore keyStore;
                    String keyStoreType = KeyStore.getDefaultType();
                    keyStore = KeyStore.getInstance(keyStoreType);
                    keyStore.load(null, null);
                    keyStore.setCertificateEntry("ca", certificate);

                    // TrustManager to trust our CAs in our KeyStore
                    String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
                    trustManagerFactory.init(keyStore);

                    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

                    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                        throw new IllegalStateException(TAG + "IllegalStateException: Unexpected default trust manager(s)");
                    }

                    X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                    // creating an SSLSocketFactory that uses our TrustManager
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustManagers, null);
                    client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .certificatePinner(new CertificatePinner.Builder()
                                    .add(PROD_HOSTNAME, "sha256/V0jFwAenKAMt/1pM3JWqT29kq+6L6u1TX8JMLCFHu7A=")
                                    .build())
                            .addInterceptor(new HttpLoggingInterceptor()
                                    .setLevel(BuildConfig.DEBUG
                                            ? HttpLoggingInterceptor.Level.BASIC
                                            : HttpLoggingInterceptor.Level.NONE))
                            .connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS))
                            .sslSocketFactory(sslContext.getSocketFactory(),
                                    trustManager).build();
                } catch (IOException ex) {
                    Log.e(TAG, "IOException: Issue reading certificate");
                } catch (NoSuchAlgorithmException ex) {
                    Log.e(TAG, "SSLContext: No such algorithm Exception");
                } catch (KeyStoreException | KeyManagementException | CertificateException ex) {
                    Log.e(TAG, "Key management or certificate exception");
                } finally {
                    if (inputCert != null) {
                        try {
                            inputCert.close();
                        } catch (IOException ex) {
                            Log.e(TAG, "Error closing inputCertStream");
                        }
                    }
                }
            }

            if (client == null) {
                Log.d(TAG, "OkHttp client null: Returning default configuration");

                client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .addInterceptor(new HttpLoggingInterceptor()
                                .setLevel(BuildConfig.DEBUG
                                        ? HttpLoggingInterceptor.Level.BASIC
                                        : HttpLoggingInterceptor.Level.NONE))
                        .certificatePinner(new CertificatePinner.Builder()
                                .add(PROD_HOSTNAME, "sha256/V0jFwAenKAMt/1pM3JWqT29kq+6L6u1TX8JMLCFHu7A=")
                                .build())
                        .connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS)) // no cleartext, no older TLS fallbacks.
                        .build();
            }
            return client;
        }
    }

}
