package org.hrana.hafez.submission;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.hrana.hafez.Constants;
import org.hrana.hafez.model.EncryptedAttachment;
import org.hrana.hafez.model.EncryptedReport;
import org.hrana.hafez.net.S3MetadataAdapter;
import org.hrana.hafez.util.AmazonAwsUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SecureRandom;

import javax.annotation.Nullable;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.fail;
import static org.hrana.hafez.Constants.CHARSET;
import static org.hrana.hafez.Constants.SUBMIT_ATTACHMENT_KEY;

/**
 * Test submission of reports and attachments on S3 only.
 */

@Ignore // Don't need to submit files to s3
//@RunWith(AndroidJUnit4.class)
public class S3SubmissionTest {
    private Context cxt;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    // can change these to anything you need for testing
    private static final String SECURITY_TOKEN = "",
            CLIENT_VERSION = "0.1",
            CLIENT_ID = "123456:7890--abcde", // This would normally be a GCM auth token
            ENCRYPTION_KEY_ID = "2a52e945917cc0a729de4eda52f5d7ac3168247aa301b6a6814de51daf07994a",
            ATTACHMENT_ID = "123456789ABC",
            SUBMISSION_TIME = "2017-05-01T12:34:56",
            REPORT_DETAILS = "aAkSDeXCZc%2BTjVMe%2BFhw2L28TX3gXICQw5pi4FVSaSQCiBR2Axuv%2B%2FApxSf2sw%2BbwXycNLNqtxaxDf4OHC1jXip8o0kSodS1dhj6Qrzy72fjx2JQBAcN6PsC%2FzyCRIuoO0ym2lEXbZW1r9qy3q6XfosQsuFBsz0%2F4Lh0VZDBFNptZVYfQ2S4b4cXtTDOkT1ZvsC9o7FesD2OdkgIDKyRWjX%2F550s2HThYP%2Ftl%2FXneVZmK%2BGC%2B51vPNetoxq4dRUTZDQelmXo3s4BqHyKgCtVkuQsHJ0D9w%2Fq21XRDG8%2Bm%2F2J1ueUXexEuiG78%2FoKPSPlmkh%2BD86p5cHT5YpK88tSwATTu%2F1VNiFQSRZe%2BdrHjZaur5SVP35qPeBYXqMLMH%2FR1u8LdnOQzuCurUWcsaVaU6HGGEpgmEdclw%3D%3D",
            TEST_FILE_NAME = "test";

    private static final String ENCRYPTED_ATTACHMENT_DETAILS = REPORT_DETAILS; // Add fake encrypted attachment details
    private static final byte[] FAKE_ENCRYPTED_BINARY_FILE = REPORT_DETAILS.getBytes(Charset.forName("UTF-8")); // Add fake encrypted attachment

    @Before
    public void setUp() throws Exception {
        cxt = InstrumentationRegistry.getTargetContext();
    }

    @After
    public void tearDown() throws Exception {
        cxt = null;
    }

    // Android doesn't like file access during testing.
    private @Nullable File getTestFile() {
        FileOutputStream fos = null;
        File f = null;
        try {
            f = folder.newFile(TEST_FILE_NAME);
            fos = new FileOutputStream(f);
            fos.write(FAKE_ENCRYPTED_BINARY_FILE, 0, FAKE_ENCRYPTED_BINARY_FILE.length);
        } catch (IOException ex) {
            Log.e("S3Test", ex.getMessage());
            ex.printStackTrace();
            fail();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {

                }
            }
        }
        return f;
    }


    // Fake report and attachments
    private EncryptedAttachment fakeAttachment() {
        return EncryptedAttachment.builder()
                .file(getTestFile()) // This is an encrypted binary file
                .securityToken(SECURITY_TOKEN)
                .privateDetails(ENCRYPTED_ATTACHMENT_DETAILS)
                .timestamp(SUBMISSION_TIME)
                .build();
    }

    private EncryptedReport fakeReport() {
        return EncryptedReport.builder()
                .encryption_key_id(ENCRYPTION_KEY_ID)
                .client_version(CLIENT_VERSION)
                .encrypted_blob(REPORT_DETAILS)
                .security_token(SECURITY_TOKEN)
                .submission_time(SUBMISSION_TIME)
                .build();
    }

    /*
     * Test submit reports directly to S3.
     *
     */
    @Test @Ignore
    public void testSubmitReport() {
        final EncryptedReport encryptedReport = fakeReport();

        final AmazonS3Client s3Client = AmazonAwsUtil.getS3Client(cxt.getApplicationContext());
        final String filename = Long.toHexString(new SecureRandom().nextLong());
        final byte[] reportBytes = reportToBytes(encryptedReport);

        try {
            PutObjectResult response = s3Client.putObject(new PutObjectRequest
                    (Constants.SUBMISSION_BUCKET, // Bucket
                            Constants.SUBMIT_REPORT_KEY + filename, // 'Subdirectory' and filename
                            new ByteArrayInputStream(reportBytes), // report as inputstream
                            generateMetaData(reportBytes.length)));
            Assert.assertNotNull(response); // Not really how it works - response validated elsewhere
        } catch (AmazonClientException ex) {
            fail(ex.getMessage());
        }
    }

    /*
     * New attachment method
     */
    @Test @Ignore // don't need to submit to AWS
    public void testNewAttachmentMethod() {
        final String TAG = "S3AttachmtTest";
        final Context appContext = InstrumentationRegistry.getTargetContext();
        final EncryptedAttachment attachment = fakeAttachment();
        final String filename = attachment.getFile().getName();
        final AmazonS3Client s3Client = AmazonAwsUtil.getS3Client(appContext);

        Observable<String> attachmentObs = Observable.create(new Observable.OnSubscribe<TransferState>() {
            @Override
            public void call(final Subscriber<? super TransferState> subscriber) {

                final TransferObserver uploadObserver = AmazonAwsUtil.getUploadObserver(
                        appContext,
                        SUBMIT_ATTACHMENT_KEY,
                        attachment.getFile());

                uploadObserver.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        Log.d(TAG, "State " + state);
                        subscriber.onNext(state);

                        if (state == TransferState.COMPLETED) {
                            Log.d(TAG, "Attachment upload completed.");
                            subscriber.onCompleted();
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        subscriber.onError(ex);
                    }
                });

            }
        }).subscribeOn(Schedulers.io())
                .doOnNext(new Action1<TransferState>() {
                    @Override
                    public void call(TransferState transferState) {
                        if (android.os.Debug.isDebuggerConnected()) {
                            android.os.Debug.waitForDebugger();
                        }
                        Log.d(TAG, "onNext for TransferState " + transferState);
                    }
                })
                .filter(new Func1<TransferState, Boolean>() {
                    @Override
                    public Boolean call(TransferState transferState) {
                        Log.d(TAG, "State " + transferState.toString());
                        return transferState == TransferState.COMPLETED;
                    }
                })
                .observeOn(Schedulers.io())
                .concatMap(new Func1<TransferState, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(TransferState transferState) {
                        Log.d(TAG, "Sending metadata...");

                        // Then upload metadata
                        byte[] metadata = metadataToBytes(attachment);

                        // returns nonnull result (success) or throws.
                        PutObjectResult result = s3Client.putObject(new PutObjectRequest(
                                Constants.SUBMISSION_BUCKET,
                                Constants.SUBMIT_MEDATADA_KEY + filename,
                                new ByteArrayInputStream(metadata),
                                generateMetaData(metadata.length)));

                        return Observable.just(result.toString())
                                .observeOn(AndroidSchedulers.mainThread());
                    }
                });
        String result = attachmentObs.toBlocking().firstOrDefault(null);
        Assert.assertNotNull(result);
    }

    /*
     * Test submit attachments to S3.
     */
    @Test @Ignore
    public void testSubmitAttachment() {
        final EncryptedAttachment attachment = fakeAttachment();
        final String filename = attachment.getFile().getName();
        final AmazonS3Client s3Client = AmazonAwsUtil.getS3Client(cxt.getApplicationContext());

        try {
            s3Client.putObject(new PutObjectRequest
                    (Constants.SUBMISSION_BUCKET, // Bucket
                            Constants.SUBMIT_ATTACHMENT_KEY + filename, // 'Subdirectory' and filename
                            attachment.getFile()));

            byte[] metadata = metadataToBytes(attachment);

            PutObjectResult result = s3Client.putObject(new PutObjectRequest(
                    Constants.SUBMISSION_BUCKET,
                    Constants.SUBMIT_MEDATADA_KEY + filename,
                    new ByteArrayInputStream(metadata),
                    generateMetaData(metadata.length)));
            Assert.assertNotNull(result);
        } catch (AmazonClientException ex) {
            fail(ex.getMessage());
        }
    }


    /*
         * Metadata for objects in S3 bucket. Metadata is autogenerated for files, but
         * required for non file types.
         *
         * All objects are encoded with default charset CHARSET, and application/json content type.
         *
         * @param   length      number of bytes in file/array
         * @returns ObjectMetadata
         */

    private ObjectMetadata generateMetaData(long length) {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentLength(length);
        metaData.setContentType("application/json");
        metaData.setContentEncoding(CHARSET);

        return metaData;
    }

    /*
     * Convert report into a JSON object and return this object in a byte[]. This
     * avoids the creation of temporary report files, but should only be used for small
     * files such as text-based reports. Larger files should not be kept in memory.
     *
     */
    private byte[] reportToBytes(EncryptedReport encryptedReport) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<EncryptedReport> adapter = moshi.adapter(EncryptedReport.class);
        String mfile = adapter.toJson(encryptedReport);
        return mfile.getBytes(Charset.forName(CHARSET));
    }

    /*
     * Convert metadata into a JSON object and return this object in a byte[].
     * This JSON contains the unencrypted metadata information (client version, etc) as
     * well as the encrypted information.
     *
     */
    private byte[] metadataToBytes(EncryptedAttachment attachment) {

        // Packaging the encrypted and non-encrypted metadata in one Json
        Moshi moshi = new Moshi.Builder().add(new S3MetadataAdapter()).build();
        JsonAdapter<EncryptedAttachment> adapter = moshi.adapter(EncryptedAttachment.class);

        return adapter.toJson(attachment).getBytes(Charset.forName(CHARSET));
    }


}
