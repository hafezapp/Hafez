package org.hrana.hafez.presenter.contract;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;


/**
 * View interface contracts
 */
public interface IViewContract {

    /*
     * MainActivity's view contract
     */
    interface MainMediaView {
        void setCurrentMediaPath(String path);
        String getCurrentMediaPath();
        void allowReporting(boolean isAllowed);
        void registerChild(ReviewMediaView child);
        void unregisterChild();
    }

    /*
     * NewsFragment view contract
     */
    interface RssView {
        void handleFeedClick(final String url);
        void loadFeedFallback(final String url);
        void showError();
    }

    /*
     * View contract for review submission activity
     */
    interface ReviewMediaView {
        boolean isInProgress();
        void clearProgressWithWarning();
        void addAttachment(Uri uri);
        void addAttachmentFromFilePath(String filepath);
        void previewMedia(Uri uri);
        void removeAttachmentCallback(Uri target);
        @NonNull String getContentType(Uri uri);
        void onRequestSubmission();
    }

    /*
     * ReportSubmission contract
     */
    interface SubmitReportView {
        void submit();
        void showSuccess();
        void showError();
        void showNoRetryError();
        void handleForbidden();
    }


    interface LegalContactsView {
        void launchExternalIntent(Intent intent);
    }

    interface UploadListener {
        void update(int percentProgress);
    }

    interface DialogCallbacks {
        void onBeginSending();
        void bindViewToParent();
        void unBind();
    }

}
