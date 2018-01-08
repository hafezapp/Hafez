package org.hrana.hafez.net;

import android.util.Base64;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.intermediate.JsonPrivateMetadataModel;

/**
 * Adapt media attachment to JSON file
 */

public class MetadataAdapter {

    // Won't be used since just POSTing
    @FromJson
    Attachment metadataFromJson(JsonPrivateMetadataModel metadata) {
        return Attachment.builder()
                .key(Base64.decode(metadata.getEncryption_key(), Base64.NO_WRAP))
                .iv(Base64.decode(metadata.getEncryption_iv(), Base64.NO_WRAP))
                .mimeType(metadata.getAttachment_type())
                .build();
    }

    @ToJson
    JsonPrivateMetadataModel metadataToJson(Attachment metadata) {
        return JsonPrivateMetadataModel.builder()
                .attachment_type(metadata.getMimeType())
                .encryption_key(Base64.encodeToString(metadata.getKey(), Base64.NO_WRAP))
                .encryption_iv(Base64.encodeToString(metadata.getIv(), Base64.NO_WRAP))
                .attachment_id(metadata.getAttachmentId())
                .submission_time(metadata.getTimestamp())
                .report_id(metadata.getReportId())
                .client_id(metadata.getClientId())
                .build();
    }
}
