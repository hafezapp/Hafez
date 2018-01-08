package org.hrana.hafez.net;


import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import org.hrana.hafez.enums.CustomEnum;
import org.hrana.hafez.model.intermediate.JsonReportModel;
import org.hrana.hafez.model.Report;

import java.util.List;

/**
 * Adapt Report class to or from Json-formatted object.
 */

public class ReportAdapter {

    // won't be used since just POSTing
    @FromJson Report reportFromJson(JsonReportModel jsonReport) {
        return Report.builder()
                .reportBody(jsonReport.getReport_body())
                .build();
    }

    @ToJson JsonReportModel reportToJson(Report report) {
        String mName, mEmail, mTelegram, mTags;
        return JsonReportModel.builder()
                .report_body(report.getReportBody())
                .report_id(report.getReportId())
                .client_id(report.getClientId())
                .name((mName = report.getName()) == null ? "" : mName)
                .email((mEmail = report.getEmail()) == null ? "" : mEmail)
                .telegram((mTelegram = report.getTelegram()) == null ? "" : mTelegram)
                .tag((mTags = getTags(report)) == null ? "" : mTags) //@todo serverside if accept tag, city, etc
                .build();
    }

    private String getTags(Report report) {
        List<CustomEnum.REPORT_TAG> tags = report.getReportTags();
        StringBuilder builder = new StringBuilder();
        if (tags != null && tags.size() > 0) {
            for (CustomEnum.REPORT_TAG t : tags) {
                builder.append(",");
                builder.append(t.toString());
            }
            return builder.toString().replace(",", ""); // the first comma
        }
        return null;
    }

}
