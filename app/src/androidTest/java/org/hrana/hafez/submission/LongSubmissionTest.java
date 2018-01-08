package org.hrana.hafez.submission;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.hrana.hafez.model.EncryptedAttachment;
import org.hrana.hafez.model.EncryptedReport;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.presenter.contract.IReportSubmissionPresenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.fail;

/**
 * Test long-running submission activities.
 */
@RunWith(AndroidJUnit4.class)
public class LongSubmissionTest {
    private IReportSubmissionPresenter testPresenter;

    @Test
    public void testSubmitLongReport() throws Exception {
        testPresenter.submit(EncryptedReport.builder().build());
    }

    @Test
    public void testSubmitLongAttachment() throws Exception {
        testPresenter.submit(EncryptedAttachment.builder().build());
    }

    @Before
    public void setUp() throws Exception {

        testPresenter = new IReportSubmissionPresenter() {

            @Override
            public void setTotalBytes(long totalBytes) {

            }

            @Override
            public Observable<String> submit(EncryptedReport encrypted) {

                return Observable.just(encrypted)
                        .flatMap(new Func1<EncryptedReport, Observable<String>>() {
                            @Override
                            public Observable<String> call(EncryptedReport encryptedReport) {
                                for (int i = 0; i < 5; i++) {
                                    Log.d("TestSubmitReport", "simulating long encryption...." + i);
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException ex) {
                                        fail("Interrupted sleep: " + ex.getMessage());
                                    }
                                }
                                return Observable.just("ENCRYPTED");
                            }
                        })
                        .subscribeOn(Schedulers.computation())
                        .flatMap(new Func1<String, Observable<String>>() {
                            @Override
                            public Observable<String> call(String s) {
                                // pretend upload
                                for (int i = 0; i < 20; i++) {
                                    Log.d("TestSubmitReport", "simulating long upload...." + i);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
                                        fail("Interrupted sleep: " + ex.getMessage());
                                    }
                                }
                                return Observable.just("CREATED");
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread());
            }

            @Override
            public void cancelSubmission() {

            }

            @Override
            public Observable<String> submit(EncryptedAttachment attachment) {
                return Observable.just(attachment)
                        .flatMap(new Func1<EncryptedAttachment, Observable<String>>() {
                            @Override
                            public Observable<String> call(EncryptedAttachment a) {
                                // waiting
                                for (int i = 0; i < 20; i++) {
                                    Log.d("TestSubmitAttachment", "simulating long submission...." + i);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
                                        fail("Interrupted sleep: " + ex.getMessage());
                                    }
                                }
                                return Observable.just("CREATED");
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }

            @Override
            public void setUseBackupAttempt(boolean shouldUseBackup) {

            }

            @Override
            public void sendSubmission(Report report, List<Uri> uris) {

            }
        };

    }
}
