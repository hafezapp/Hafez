package org.hrana.hafez.util;

import android.content.Context;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import org.hrana.hafez.Constants;

import java.io.File;

/**
 * Utility methods to interact with Amazon Web Services (AWS).
 */
public class AmazonAwsUtil {

    private static final int MAX_RETRIES = 3;
    private static final int MAX_TIMEOUT_INTERVAL_SERVER = 60000;
    private static AmazonS3Client s3Client;
    private static CognitoCachingCredentialsProvider credentialsProvider;
    private static TransferUtility transferUtility;

    // Don't instantiate this class
    private AmazonAwsUtil() {
    }

    /*
     * @param appContext: ApplicationContext
     */
    private static CognitoCachingCredentialsProvider getCredentialProvider(Context appContext) {
        if (credentialsProvider == null) {
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    appContext.getApplicationContext(),
                    Constants.COGNITO_POOL_ID,
                    Regions.US_EAST_1);
        }
        return credentialsProvider;
    }

    /*
     * Instance of S3 Client
     * @param appContext: ApplicationContext
     */
    public static AmazonS3Client getS3Client(Context appContext) {
        if (s3Client == null) {
            s3Client = new AmazonS3Client(getCredentialProvider(appContext),
                    new ClientConfiguration().withConnectionTimeout(MAX_TIMEOUT_INTERVAL_SERVER)
                            .withSocketTimeout(MAX_TIMEOUT_INTERVAL_SERVER)
                            .withMaxErrorRetry(MAX_RETRIES));
            s3Client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        }

        return s3Client;
    }

    /*
     * Upload and download files from S3.
     * @param appContext: ApplicationContext
     */
    public static TransferUtility getTransferUtility(Context appContext) {
        if (transferUtility == null) {
            transferUtility = new TransferUtility(getS3Client(appContext),
                    appContext.getApplicationContext());
        }

        return transferUtility;
    }

    public static String getCognitoId(Context context) {
        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider = getCredentialProvider(context);
        return cognitoCachingCredentialsProvider.getIdentityId();
    }

    /*
     * Manage download and location via TransferObserver object. Starts download and returns observer.
     * @param   Context     Application context
     * @param   bucket      String representing bucket name
     * @param   whichKey    Key representing filename in bucket
     * @param   targetFile  File to download to
     * @returns             TransferObserver object
     */
    public static TransferObserver getDownloadObserver(Context context, String whichKey, File targetFile) {
        return getTransferUtility(context).download(
                Constants.DOWNLOAD_BUCKET,   /* The bucket to download from */
                whichKey,           /* The key for the object to download */
                targetFile          /* The file to download the object to */
        );
    }

    public static TransferObserver getUploadObserver(Context appContext, String which, File uploadFile) {
        return getTransferUtility(appContext)
                .upload(Constants.SUBMISSION_BUCKET,
                        which,
                        uploadFile);
    }

}
