package org.hrana.hafez.presenter.contract;

import android.net.Uri;

import org.hrana.hafez.model.EncryptedAttachment;
import org.hrana.hafez.model.EncryptedReport;
import org.hrana.hafez.model.Report;

import java.util.List;

import rx.Observable;

/**
 * Presenter contract for submitting reports.
 */
public interface IReportSubmissionPresenter {

    Observable<String> submit(EncryptedReport encrypted);
    Observable<String> submit(EncryptedAttachment attachment);
    void setUseBackupAttempt(boolean shouldUseBackup);
    void sendSubmission(final Report report, final List<Uri> uris);
    void cancelSubmission();
    void setTotalBytes(long totalBytes);
}
