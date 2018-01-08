package org.hrana.hafez.service;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.hrana.hafez.Constants;
import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.di.module.TestNetModule;
import org.hrana.hafez.net.IApiEndpoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.fail;

/**
 * Stub class for testing api
 */

@Ignore // @todo use for testing only if reinstate direct submissions to middleware
public class ApiEndpointTest {
    private String testUrl = Constants.PROD_SERVER_URL;
    private TestNetModule module;
    private Context context;
    @Inject IApiEndpoint ep;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();

        module = new TestNetModule(testUrl);
        DaggerITestNetComponent.builder()
                .testNetModule(module)
                .iApplicationComponent(
                        ((BaseApplication)context.getApplicationContext())
                                .getComponent())
                .build()
                .inject(this);

    }

    @Test
    public void testSubmitReportEndpoint() throws Exception {
        Observable<String> observable = ep.postEncryptedReport("12345", "12:34:56T00.0000"
        , "", "", "");
        Subscription s = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!(e instanceof HttpException)) {
                            fail("OnError: " + e);
                        } else { // http exception
                            if (((HttpException) e).code() < 500) {
                                // 400 error
                                fail("Client submission error " + e);
                            }
                        }
                    }

                    @Override
                    public void onNext(String s) {
                    }
                });

    }

    @After
    public void tearDown() throws Exception {

    }
}
