package org.hrana.hafez.exception;

import java.io.IOException;


/**
 * Exception to be thrown when access to network service is denied.
 */

public class ForbiddenServiceException extends IOException {
    public ForbiddenServiceException(String message) {
        super(message);
    }
}
