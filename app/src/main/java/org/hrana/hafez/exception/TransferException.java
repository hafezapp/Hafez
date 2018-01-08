package org.hrana.hafez.exception;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

/**
 * Exception thrown when AWS S3 transfer reaches a failed {@link TransferState}.
 */

public class TransferException extends AmazonServiceException {
    public TransferException(String errorMessage) {
        super(errorMessage);
    }
}
