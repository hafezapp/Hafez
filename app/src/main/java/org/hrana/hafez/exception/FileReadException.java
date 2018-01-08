package org.hrana.hafez.exception;

/**
 * {Exception} class thrown when media file attributes (size, type) cannot be read.
 */
public class FileReadException extends Exception {
    public FileReadException(String message) {
        super(message);
    }
}
