package org.hrana.hafez.net;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.EncryptedAttachment;
import org.hrana.hafez.model.EncryptedReport;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.presenter.ReportSubmissionPresenter;
import org.hrana.hafez.presenter.contract.ICryptoPresenter;
import org.hrana.hafez.presenter.contract.IMediaPresenter;
import org.hrana.hafez.presenter.contract.IViewContract;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;

import okhttp3.RequestBody;
import retrofit2.http.Field;
import retrofit2.http.Part;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.fail;
import static org.hrana.hafez.Constants.SIMPLE_DATE_FORMAT;

/**
 * Test that chain of observables is passing and handling correct objects.
 * Stub all network functionality out, just test interaction of objects on different threads.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ReportAttachmentChainTest {
    private Context context;
    private ReportSubmissionPresenter testPresenter;
    private static final String FAKE_BLOB = "fakeblobtest1234";
    private static final String FAKE_ID = "12345678abcde";

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        IViewContract.SubmitReportView stubView = new StubReportView();
        IApiEndpoint stubApi = new StubEndpoint();

        testPresenter = new ReportSubmissionPresenter(context, stubApi, new ICryptoPresenter() {
            @Override
            public EncryptedReport encryptReport(Report report) {
                return null;
            }

            @Override
            public EncryptedAttachment encryptAttachment(File dir, SecretKey attachmentKey, Attachment attachment, InputStream mediaStream) throws FileNotFoundException {
                return null;
            }

            @Override
            public SecretKey generateAttachmentKey() {
                return null;
            }
        }, new IMediaPresenter() {
            @Override
            public List<Attachment> toAttachments(Report parent, List<Uri> uris, SecretKey key) {
                return new ArrayList<>();
            }

            @Override
            public InputStream openFile(Attachment attachment) {
                return null;
            }

            @Override
            public String getContentType(Uri uri) {
                return null;
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        context = null;
        testPresenter = null;
    }

    @Test
    public void testSubmitChainItems() throws Exception {
        List<Report> fakes = fakeReports(4);
        Observable<String> obs = Observable.from(fakes)
                .subscribeOn(Schedulers.computation()) // Encryption on computation thread
                .flatMap(new Func1<Report, Observable<EncryptedReport>>() {
                    @Override
                    public Observable<EncryptedReport> call(Report report) {
                        return Observable.just(EncryptedReport.builder().encrypted_blob(FAKE_ID).encryption_key_id(FAKE_ID).build());
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                    }
                })
                .observeOn(Schedulers.io()) // Networking on IO thread
                .concatMap(new Func1<EncryptedReport, Observable<String>>() {
                    @Override
                    public Observable<String> call(EncryptedReport reportBlob) {
                        return testPresenter.submit(reportBlob);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //@todo
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) // Observe success (or not) on main
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                    }
                });

    }

    @Test
    public void testSubmitEncryptReport() throws Exception {
        Observable<String> o = Observable.just(Report.builder().build())
                .subscribeOn(Schedulers.computation()) // Encryption on computation thread
                .flatMap(new Func1<Report, Observable<EncryptedReport>>() {
                    @Override
                    public Observable<EncryptedReport> call(Report report) {
                        return Observable.just(EncryptedReport.builder().encrypted_blob(FAKE_ID).encryption_key_id(FAKE_ID).build());
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                    }
                })
                .observeOn(Schedulers.io()) // Networking on IO thread
                .concatMap(new Func1<EncryptedReport, Observable<String>>() {
                    @Override
                    public Observable<String> call(EncryptedReport reportBlob) {
                        return testPresenter.submit(reportBlob);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //@todo
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) // Observe success (or not) on main
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                    }
                });

    }

    @Test
    public void testSubmissionReport() throws Exception {
        Observable<String> resp = testPresenter.submit(EncryptedReport.builder().encrypted_blob(FAKE_ID).encryption_key_id(FAKE_ID).build());
        Assert.assertNotNull(resp);
    }

    private List<Report> fakeReports(int num) {
        List<Report> reports = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            reports.add(Report.builder().build()); // fake
        }
        return reports;
    }

    private File makeFile(int prefix) throws Exception {
        InputStream realFileInputStream = InstrumentationRegistry.getContext().getAssets().open("picture.png");
        File tempAttachment = folder.newFile("picture" + prefix + ".png");
        FileOutputStream fos = new FileOutputStream(tempAttachment);

        byte[] buff = new byte[1024];
        int len;
        while ((len = realFileInputStream.read(buff)) != -1) {
            fos.write(buff, 0, len);
        }

        fos.close();
        return tempAttachment;
    }

    private List<EncryptedAttachment> makeFakeAttachments(int count) throws Exception {
        List<EncryptedAttachment> fakeAttachments = new ArrayList<>();

        // Fakes
        for (int i = 0; i < count; i++) {
            File f = makeFile(i);
            EncryptedAttachment a = EncryptedAttachment.builder()
                    //.attachmentId(FAKE_ID)
                    .timestamp(new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.US).format(new Date()))
                    .securityToken("")
                    //.clientId(FAKE_ID)
                    .encryptionId(FAKE_ID)
                    .file(f)
                    .build();
            fakeAttachments.add(a);
        }
        return fakeAttachments;
    }

    @Test
    public void testStubSubmit() throws Exception {
        EncryptedAttachment fakeAttachment = makeFakeAttachments(1).get(0);
        Observable<String> obs = testPresenter.submit(fakeAttachment);
        obs.subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                fail(e.getMessage());
            }

            @Override
            public void onNext(String s) {

            }
        });
    }

    @Test
    public void testStubManyAttachments() throws Exception {
        List<EncryptedAttachment> fakes = makeFakeAttachments(3);
        for (EncryptedAttachment a : fakes) {
            Observable<String> o = testPresenter.submit(a);
            Assert.assertNotNull(o);
            o.doOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    fail("onError " + throwable.getMessage());
                }
            }).doOnCompleted(new Action0() {
                @Override
                public void call() {
                }
            })
                    .subscribe();
        }
    }

    @Test
    public void testStubConcatManyAttachments() throws Exception {
        List<EncryptedAttachment> fakes = makeFakeAttachments(3);
        Observable<String> observable = Observable.from(fakes)
                .concatMap(new Func1<EncryptedAttachment, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(EncryptedAttachment attachment) {
                        return testPresenter.submit(attachment);
                    }
                }).doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        fail("onError " + throwable.getMessage());
                    }
                }).doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        Log.d("testMany", "Completed");
                    }
                });
        observable.subscribe();

    }

    @Test // Make sure that the correct objects make in through the observable chain, even as stubs.
    public void testChainedAttachments() throws Exception {
        List<EncryptedAttachment> fakes = makeFakeAttachments(3);

        Observable<String> obs = Observable.from(fakes)
                .observeOn(Schedulers.io()) // Networking on IO thread
                .concatMap(new Func1<EncryptedAttachment, Observable<String>>() {
                    @Override
                    public Observable<String> call(EncryptedAttachment attachment) {
                        Assert.assertNotNull(attachment);
                        Assert.assertNotNull(attachment.getFile());
                        Log.d("AttachmentTest", "Attachment filename is " + attachment.getFile().getName()
                        );
                        return testPresenter.submit(attachment);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        fail("(fake) Submission error on IO");
                        Log.e("ReportChainTest", "(fake) submission error");
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        fail("Error on main thread");
                        Log.e("AttachmentChainTest", " Observing on main thread: completion error");
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        Log.d("AttachmentChainTest", "doOnCompleted");
                    }
                });
        obs.subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                fail(e.getMessage());
            }

            @Override
            public void onNext(String s) {

            }
        });
    }


    /*
     * Stub endpoint class to test setup of observables, mocking out network calls.
     */
    private class StubEndpoint implements IApiEndpoint {
        @Override
        public Observable<String> postEncryptedAttachment(@Part("client_version") RequestBody clientVersion, @Part("submission_time") RequestBody time, @Part("encryption_key_id") RequestBody encryptionKeyId, @Part("security_token") RequestBody token, @Part("encrypted_blob") RequestBody blob, @Part("attachment_data\"; filename=\"attachment_data\"") RequestBody attachment) {
            return Observable.just("FAKED");
        }

        @Override
        public Observable<String> postEncryptedReport(@Field("client_version") String clientVersion, @Field("submission_time") String time, @Field("encryption_key_id") String encryptionKeyId, @Field("security_token") String token, @Field("encrypted_blob") String blob) {
            return Observable.just("FAKED");
        }
    }

    /*
     * Stub reportview class for ensuring success and error messages are delivered
     */
    private class StubReportView implements IViewContract.SubmitReportView {

        @Override
        public void submit() {

        }

        @Override
        public void showSuccess() {

        }


        @Override
        public void showError() {

        }

        @Override
        public void showNoRetryError() {

        }

        @Override
        public void handleForbidden() {

        }
    }


}
