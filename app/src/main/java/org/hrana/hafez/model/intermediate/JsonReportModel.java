package org.hrana.hafez.model.intermediate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Json object to send encrypted report.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class JsonReportModel {

    private String report_body, name, email, telegram;
    private String report_id, client_id;

    private String tag;

    private String timestamp;

    private byte[] attachmentKey;
}

