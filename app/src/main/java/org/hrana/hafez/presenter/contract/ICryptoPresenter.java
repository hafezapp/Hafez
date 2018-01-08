package org.hrana.hafez.presenter.contract;

import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.model.EncryptedAttachment;
import org.hrana.hafez.model.EncryptedReport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

/**
 * Presenter class for encryption
 */

public interface ICryptoPresenter {
    EncryptedReport encryptReport(Report report);
    EncryptedAttachment encryptAttachment(File dir,
                                          SecretKey attachmentKey,
                                          Attachment attachment,
                                          InputStream mediaStream) throws FileNotFoundException;
    SecretKey generateAttachmentKey() throws NoSuchAlgorithmException;
}
