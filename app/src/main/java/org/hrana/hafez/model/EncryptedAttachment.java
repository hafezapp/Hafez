package org.hrana.hafez.model;

import java.io.File;

import lombok.Data;
import lombok.experimental.Builder;

/**
 * EncryptedAttachment contains all necessary attributes to POST an attachment.
 */
@Builder
@Data
public class EncryptedAttachment implements Encryptable {
    private String timestamp, encryptionId, securityToken, privateDetails, clientVersion;
    private File file;
}
