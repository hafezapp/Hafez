package org.hrana.hafez.model;

import android.net.Uri;

import lombok.Data;
import lombok.experimental.Builder;

/**
 * Media attachment object.
 */
@Builder
@Data
public class Attachment {
    private Uri uri;
    private String reportId, attachmentId, clientId;
    private String timestamp; // yyyy-MM-ddTHH:mm:ss.SSSS
    private String mimeType;
    private String encryptionId;
    private String securityToken = ""; // if using Captcha etc
    private byte[] key;
    private byte[] iv;
}
