package org.hrana.hafez.model;

import org.hrana.hafez.enums.CustomEnum;

import java.security.SecureRandom;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

/**
 * Report model. A report contains attributes such as a reportBody (the text report itself),
 * a timestamp,
 *
 */
@Data
@Builder
@NoArgsConstructor  // NoArgs and AllArgs required to avoid compilation errors with uri and builder
@AllArgsConstructor(suppressConstructorProperties = true)
public class Report {

    protected String reportId, clientId;
    protected String timestamp; // yyyy-MM-ddTHH:mm:ss.SSSS
    private String reportBody;
    private String name;
    private String email;
    private String telegram;
    private String securityToken; //@Todo @Improvement implemented with captcha etc in future version
    private int attachmentNumber;
    private boolean withAttachments;

    private List<CustomEnum.REPORT_TAG> reportTags;

    public Report assignId() {
        long id = new SecureRandom().nextLong();
        reportId = Long.toHexString(id);
        return this;
    }

    public Report assignClientId(String id) {
        this.clientId = id;
        return this;
    }
}
