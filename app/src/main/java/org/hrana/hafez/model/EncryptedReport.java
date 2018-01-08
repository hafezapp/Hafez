package org.hrana.hafez.model;


import lombok.Getter;
import lombok.experimental.Builder;

/**
 * Encrypted Report object. Contains all attributes for report POST.
 */

@Builder
public class EncryptedReport implements Encryptable {
    @Getter private String submission_time, encryption_key_id, client_version, encrypted_blob;
    @Getter private String security_token; // @Todo @Improvement for later version option to add eg auth via captcha

}
