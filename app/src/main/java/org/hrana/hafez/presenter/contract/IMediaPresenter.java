package org.hrana.hafez.presenter.contract;

import android.net.Uri;

import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;

import java.io.InputStream;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Presenter to handle media files in report.
 */

public interface IMediaPresenter {
    List<Attachment> toAttachments(Report parent, List<Uri> uris, SecretKey key);
    InputStream openFile(Attachment attachment);
    String getContentType(Uri uri);
}
