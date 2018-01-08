package org.hrana.hafez.net;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import org.hrana.hafez.model.EncryptedAttachment;
import org.hrana.hafez.model.EncryptedReport;

import static org.hrana.hafez.Constants.CLIENT_VERSION;

/*
 * Adapter class to adapt {@link EncryptedAttachment}s into metadata-only.
 */
public class S3MetadataAdapter {

    // Won't be used since just POSTing
    @FromJson
    EncryptedAttachment  metadataFromJson(EncryptedReport metadata) {
        return EncryptedAttachment.builder()
                .build();
    }

    @ToJson
    EncryptedReport metadataToJson(EncryptedAttachment metadata) {
        return EncryptedReport.builder()
                .client_version(CLIENT_VERSION)
                .submission_time(metadata.getTimestamp())
                .security_token(metadata.getSecurityToken())
                .encryption_key_id(metadata.getEncryptionId())
                .encrypted_blob(metadata.getPrivateDetails())
                .build();
    }
}
