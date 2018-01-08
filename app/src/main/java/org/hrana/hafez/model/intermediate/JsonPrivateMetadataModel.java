package org.hrana.hafez.model.intermediate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Model for packaging and sending JSON containing encrypted media file.
 */

@Builder
@Data
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
public class JsonPrivateMetadataModel {

    private String attachment_type,
            submission_time,
            encryption_key,
            encryption_iv,
            attachment_id, // == filename
            client_id,
            report_id,
            filename;

}
