package org.hrana.hafez.presenter;

import android.util.Base64;
import android.util.Log;

import com.squareup.moshi.JsonAdapter;

import org.hrana.hafez.exception.OversizeFileException;
import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.EncryptedAttachment;
import org.hrana.hafez.model.EncryptedReport;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.presenter.contract.ICryptoPresenter;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.keys.PublicKey;
import org.libsodium.jni.keys.SigningKey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;

import lombok.Getter;

import static org.hrana.hafez.Constants.CHARSET;
import static org.hrana.hafez.Constants.CLIENT_VERSION;
import static org.hrana.hafez.Constants.MAX_SUBMISSION_BYTES;

/**
 * Encrypt reports for submission.
 */

public class CryptoPresenter implements ICryptoPresenter {
    private static final String TAG = "CryptoPresenter";
    private static final Charset UTF_8 = Charset.forName(CHARSET);
    @Getter
    private JsonAdapter<Report> jsonAdapter;
    @Getter
    private JsonAdapter<Attachment> metadataAdapter;
    private PublicKey serverPublicKey;
    private SigningKey signingKey;

    @Inject
    public CryptoPresenter(JsonAdapter<Report> jsonAdapter,
                           JsonAdapter<Attachment> metadataAdapter,
                           PublicKey serverKey, SigningKey signingKey) {
        this.jsonAdapter = jsonAdapter;
        this.metadataAdapter = metadataAdapter;
        this.serverPublicKey = serverKey;
        this.signingKey = signingKey;
    }

    @Override
    public EncryptedReport encryptReport(Report report) {

        // Step 1: Turn report into JSON string.
        // Behind the scenes: jsonAdapter (using Moshi) converts a Report object to an
        // intermediate JsonReportModel object, then finally to a JSON string.
        // Step 2: encrypt json string using NaCL (libsodium)
        String jsonReportString = jsonAdapter.toJson(report);
        byte[] cipherText = new byte[jsonReportString.getBytes(UTF_8).length
                + Sodium.crypto_box_sealbytes()];
        Sodium.crypto_box_seal(cipherText, jsonReportString.getBytes(UTF_8),
                jsonReportString.getBytes(UTF_8).length, serverPublicKey.toBytes());

        // Step 3: Base64 the ciphertext, and put it in a simple object for transmission.
        return EncryptedReport.builder()
                .client_version(CLIENT_VERSION)
                .submission_time(report.getTimestamp())
                .encryption_key_id(serverPublicKey.toString())
                .security_token(report.getSecurityToken() == null ? "" : report.getSecurityToken()) // @Todo for now: future may implement captcha etc
                .encrypted_blob(Base64.encodeToString(cipherText, Base64.NO_WRAP)) // vs DEFAULT (line breaks/padding)
                .build();
    }

    /*
     * Encrypt {@link Attachment} media file and its related metadata,
     * and return an object containing both the encrypted file and the encrypted metadata.
     *
     * File is encrypted using AES-Counter mode with the Secret Key provided and
     * an IV that is generated using Ciper#getIv().
     *
     * Since these are necessary for decryption, they are packaged in the attachment's metadata,
     * which itself is then encrypted using NaCL's public key encryption (Libsodium#crypto_box_seal()).
     *
     * This means that the metadata must be successfully decrypted server-side in order to obtain the key,
     * IV, and MIME type of (and to then decrypt) the attachment.
     *
     * @param   output      File object into which to write encrypted media binary.
     * @param   secretKey   AES-Key to which to encrypt
     *
     * @returns encryptedAttachment {@link EncryptedAttachment} object
     */
    @Override
    public EncryptedAttachment encryptAttachment(File output, SecretKey secretKey, Attachment attachment,
                                                 InputStream mediaInputStream) throws FileNotFoundException, OversizeFileException {

        CipherInputStream cipherInputStream = null;
        FileOutputStream outputStream = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Get IV used for the media file
            byte[] iv = cipher.getIV();

            if (mediaInputStream == null) {
                throw new FileNotFoundException("Error opening mediaInputStream: stream returned null");

            } else {
                cipherInputStream = new CipherInputStream(mediaInputStream, cipher);

                int consumed = 0;
                byte[] buffer = new byte[3000]; // divide evenly by 3 and 4 re: base64

                //File output = new File(output, name);
                if (!output.getParentFile().exists()) {
                    boolean madeDir = output.getParentFile().mkdir();
                    boolean result = output.createNewFile();
                }

                // Step 1: Private Metadata / Attachment info
                // Convert private attachment details (mimetype, encryption_iv, AES shared secret encryption_key) to json
                attachment.setIv(iv);
                attachment.setEncryptionId(serverPublicKey.toString());
                attachment.setKey(secretKey.getEncoded());

                String jsonAttachmentString = metadataAdapter.toJson(attachment);

                // Encrypt metadata with Libsodium (public-key encryption) to safely transmit
                byte[] cipherText = new byte[jsonAttachmentString.getBytes(UTF_8).length
                        + Sodium.crypto_box_sealbytes()];
                Sodium.crypto_box_seal(cipherText, jsonAttachmentString.getBytes(UTF_8),
                        jsonAttachmentString.getBytes().length, serverPublicKey.toBytes());

                EncryptedAttachment encryptedAttachment = EncryptedAttachment
                        .builder()
                        .clientVersion(CLIENT_VERSION)
                        .encryptionId(attachment.getEncryptionId())
                        .securityToken(attachment.getSecurityToken())
                        .timestamp(attachment.getTimestamp())
                        .privateDetails((Base64.encodeToString(cipherText, Base64.NO_WRAP)))
                        .build();

                // Step 2: Open fileoutput stream. Encrypt media and write to stream.
                outputStream = new FileOutputStream(output);

                while ((consumed = cipherInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, consumed);
                }

                // Step 3: Check filesize. If not too large, put encrypted media file into attachmentblob.
                // Note that file size has already been checked once when user selects the attachment,
                // and this is an extra step.
                if (output.length() > MAX_SUBMISSION_BYTES) {
                    throw new OversizeFileException();
                } else {
                    encryptedAttachment.setFile(output);

                    return encryptedAttachment;
                }
            }

        } catch (GeneralSecurityException | IOException ex) {
            Log.e(TAG, "Exception found during encryption--will return null EncryptedAttachment");
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    Log.e(TAG, "Error closing outputstream");
                }
            }
            if (cipherInputStream != null) {
                try {
                    cipherInputStream.close();
                } catch (IOException ex) {
                    Log.e(TAG, "Error closing cipherInputStream");
                }
            }
            if (mediaInputStream != null) {
                try {
                    mediaInputStream.close();
                } catch (IOException ex) {
                    Log.e(TAG, "Error closing mediaInputStream");
                }
            }
        }
        return null;
    }

    @Override
    public SecretKey generateAttachmentKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        return keyGenerator.generateKey();
    }

}
