package org.hrana.hafez.presenter;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.hrana.hafez.model.Attachment;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.presenter.contract.IMediaPresenter;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;

/**
 *
 */
public class MediaPresenter implements IMediaPresenter {
    private static final String TAG = "MediaPresenter";
    private ContentResolver contentResolver;

    public MediaPresenter(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    @Override @NonNull
    public List<Attachment> toAttachments(Report parent, List<Uri> uris, SecretKey key) {
        List<Attachment> attachmentList = new ArrayList<>();
        for (Uri uri : uris) {
            attachmentList.add(Attachment.builder()
                    .uri(uri)
                    .reportId(parent.getReportId())
                    .timestamp(parent.getTimestamp())
                    .mimeType(getContentType(uri))
                    .clientId(parent.getClientId())
                    .securityToken("")
                    .attachmentId(generateId())
                    .key(key.getEncoded())
                    .build());
        }
        return attachmentList;
    }

    @Override
    public @NonNull String getContentType(Uri uri) throws TypeNotPresentException {
        String result = contentResolver.getType(uri);
        if (result != null) {
            return result;
        } else { // try to get a type
            result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.toString())).toLowerCase(Locale.US);
            if ("".equals(result) || null == result) {
                throw new TypeNotPresentException("MimeType could not be found",
                        new NullPointerException("getContentType for " + uri + "returned null"));
            } else {
                return result;
            }
        }
    }

    /*
     * Return open inputstream for media attachment, or null if stream is unavailable.
     */
    public InputStream openFile(Attachment attachment) {
        try {
            return contentResolver.openInputStream(attachment.getUri());
        } catch (FileNotFoundException ex) {
            Log.e(TAG, ex.getMessage());
        }
        return null;
    }
    
    private String generateId() {
        long id = new SecureRandom().nextLong();
        return Long.toHexString(id);
    }
}
