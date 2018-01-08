package org.hrana.hafez.view.fragment;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.hrana.hafez.BuildConfig;
import org.hrana.hafez.R;
import org.hrana.hafez.di.BaseApplication;
import org.hrana.hafez.di.component.DaggerISubmissionComponent;
import org.hrana.hafez.di.module.CryptoModule;
import org.hrana.hafez.di.module.NetModule;
import org.hrana.hafez.di.module.SubmissionModule;
import org.hrana.hafez.enums.CustomEnum;
import org.hrana.hafez.exception.FileReadException;
import org.hrana.hafez.exception.InvalidClientIdException;
import org.hrana.hafez.exception.OversizeFileException;
import org.hrana.hafez.model.Report;
import org.hrana.hafez.presenter.contract.IMediaPresenter;
import org.hrana.hafez.presenter.contract.IReportSubmissionPresenter;
import org.hrana.hafez.presenter.contract.IViewContract;
import org.hrana.hafez.view.activity.BaseActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import okhttp3.OkHttpClient;

import static org.hrana.hafez.Constants.ACTION_ERROR;
import static org.hrana.hafez.Constants.ACTION_INTERNAL;
import static org.hrana.hafez.Constants.ACTION_VIEW;
import static org.hrana.hafez.Constants.CAN_ATTEMPT_ORBOT;
import static org.hrana.hafez.Constants.CATEGORY_ERROR;
import static org.hrana.hafez.Constants.CATEGORY_INTERNAL;
import static org.hrana.hafez.Constants.CLIENT_TOKEN_KEY;
import static org.hrana.hafez.Constants.INTENT_ACTION_ENCRYPTION_FINISHED;
import static org.hrana.hafez.Constants.INTENT_ACTION_SUBMISSION_STATUS;
import static org.hrana.hafez.Constants.INTENT_ACTION_UPDATE_PROGRESS;
import static org.hrana.hafez.Constants.IS_SUBMISSION_SUCCESS;
import static org.hrana.hafez.Constants.MAX_SUBMISSION_BYTES;
import static org.hrana.hafez.Constants.PROD_SERVER_URL;
import static org.hrana.hafez.Constants.SIMPLE_DATE_FORMAT;
import static org.hrana.hafez.Constants.UPLOAD_PROGRESS;

/**
 * ReportSubmission
 */
public class ReportSubmissionFragment extends Fragment
        implements IViewContract.SubmitReportView,
        IViewContract.ReviewMediaView,
        StrongOkHttpClientBuilder.Callback<OkHttpClient>,
        IViewContract.UploadListener {

    private final static String TAG = "ReviewSubmission",
            ATTEMPT = "Attempt", ATTACHMENT_SIZE = "AttachmentSize";
    private static ArrayList<Uri> uris;
    private String email, city;
    private List<CustomEnum.REPORT_TAG> tags;
    private boolean hasDoneSecondAttempt;
    private NetModule netModule;
    private long allAttachmentBytes = 0; // keep track of how large the files that have been added are.
    private Unbinder unbinder;
    private IViewContract.MainMediaView parent;
    private BroadcastReceiver mReceiver;
    private View.OnClickListener cancelListener, dismissOnSuccessListener; // for dialog overlay

    @Inject SharedPreferences preferences;
    @Inject ConnectivityManager connectivityManager;

    @BindView(R.id.reportBody)
    EditText reportBody;
    @BindView(R.id.media_recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.gray_loading)
    View loadingView;
    @BindView(R.id.progress_layout)
    LinearLayout progressWindowLayout;
    @BindView(R.id.progress_text_header)
    TextView progressTextHeader;
    @BindView(R.id.progress_action_button)
    Button progressActionButton;
    @BindView(R.id.progress_text_status)
    TextView progressTextStatus;

    @Inject
    IReportSubmissionPresenter submissionPresenter;
    @Inject
    IMediaPresenter mediaPresenter;

    public static ReportSubmissionFragment newInstance() {
        return new ReportSubmissionFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof IViewContract.MainMediaView) {
            parent = (IViewContract.MainMediaView) getActivity();
            parent.registerChild(this);
        } else {
            Log.d(TAG, "Well that didn't work out--Activity is not a MediaViewer");
        }

        uris = new ArrayList<>();
        tags = new ArrayList<>();
        netModule = new NetModule(PROD_SERVER_URL);

        // DI
        DaggerISubmissionComponent.builder()
                .submissionModule(new SubmissionModule())
                .netModule(netModule)
                .cryptoModule(new CryptoModule())
                .iApplicationComponent(BaseApplication.get(getActivity())
                        .getComponent())
                .build()
                .inject(this);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(INTENT_ACTION_SUBMISSION_STATUS)) {
                    boolean success = intent.getBooleanExtra(IS_SUBMISSION_SUCCESS, false);
                    if (success) {
                        showSuccess();
                    } else {
                        showError();
                    }
                } else if (intent.getAction().equals(INTENT_ACTION_UPDATE_PROGRESS)) {
                    int progressPercent = intent.getIntExtra(UPLOAD_PROGRESS, -1);
                    if (progressPercent != -1) {
                        update(progressPercent);
                    }
                } else if (intent.getAction().equals(INTENT_ACTION_ENCRYPTION_FINISHED)) {
                    progressTextHeader.setText(getString(R.string.progress_message_send));
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_ACTION_SUBMISSION_STATUS);
        intentFilter.addAction(INTENT_ACTION_UPDATE_PROGRESS);
        intentFilter.addAction(INTENT_ACTION_ENCRYPTION_FINISHED);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, intentFilter);

        // Check bundle
        if (savedInstanceState != null) {
            hasDoneSecondAttempt = savedInstanceState.getBoolean(ATTEMPT);
            allAttachmentBytes = savedInstanceState.getLong(ATTACHMENT_SIZE);
        }

        // Create listeners for overlay
        // Just dismiss overlay
        dismissOnSuccessListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSubmission();
                setIsSending(false);

                // Reset dialog view for next time
                resetProgressView();
            }
        };

        // Cancel submission and dismiss overlay
        cancelListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submissionPresenter.cancelSubmission();
                setIsSending(false);

                // Reset dialog view for next time
                resetProgressView();
            }
        };
    }

    /*
     * Clear current submission
     */
    private void clearSubmission() {
        // Empty UI
        uris.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
        allAttachmentBytes = 0;

        reportBody.setText(null);
    }

    /*
     * Get a file URI for this attachment,
     * then call #addAttachment(Uri uri).
     */
    @Override
    public void addAttachmentFromFilePath(String filePath) {
        File file = new File(filePath);
        Uri uri = FileProvider.getUriForFile(getActivity(),
                BuildConfig.APPLICATION_ID + ".provider",
                file);
        addAttachment(uri);
    }

    /*
     * Check to make sure there is space for this attachment within submission quota, and add it.
     */
    @Override
    public void addAttachment(Uri uri) {
        try {
            addUriWithSizeCheck(uri);
        } catch (OversizeFileException ex) {
            showOversizeFileAlert();
        } catch (FileReadException ex) {
            showFileError();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_review_submission, container, false);

        // UI
        unbinder = ButterKnife.bind(this, view);

        // Recyclerview for media attachmentNumber
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(5);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setAdapter(new AttachmentAdapter(this, uris));

        return view;
    }

    /*
     * AlertDialog: Confirm navigate away from submission
     */
    public void showCancelDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.title_leave_submission))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                uris.clear();
                                reportBody.setText(null);
                            }
                        })
                .setNeutralButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                .show();
    }

    @Override
    public void onConnected(OkHttpClient connectedClient) {
        netModule.setClient(connectedClient); // make sure new client is used
        submissionPresenter.sendSubmission(generateReport(), uris);
    }

    @Override
    public void onConnectionException(Exception e) {
        Log.e(TAG, "connectionException");
        promptSubmissionOnUi();
    }

    @Override
    public void onTimeout() {
        // No Orbot, or couldn't connect to Orbot: decide if submit normally, prompt, or not
        Log.e(TAG, "Orbot Connection Timeout");
        promptSubmissionOnUi();
    }

    @Override
    public void onInvalid() {
        Log.e(TAG, "Invalid Request");
        promptSubmissionOnUi();
    }

    // Launch submission options dialog if Orbot connection fails. Enforce UI thread b/c not guaranteed that callbacks will be on UI thread.
    private void promptSubmissionOnUi() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                requestNonOrbotSubmission();
            }
        });
    }

    /*
     * Check if Orbot is available, then submit report and attachment. Currently each item (report, attachment)
     * are encrypted then submitted in separate POST request.
     *
     * @TODO: currently Orbot detection/automatic use of Tor is not supported due to prohibitively slow upload speeds.
     * However, users can still configure apps such as Orbot themselves to tunnel traffic from other apps.
     */
    @Override
    public void submit() {

        // GA
        Tracker tracker = ((BaseApplication) getActivity().getApplication()).getDefaultTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setAction(ACTION_INTERNAL)
                .setCategory("Report Specs")
                .setLabel("Attachment Number")
                .setValue(uris.size())
                .build());

        tracker.send(new HitBuilders.EventBuilder()
                .setAction(ACTION_INTERNAL)
                .setCategory("Report Specs")
                .setLabel("Submission Size")
                .setValue(allAttachmentBytes)
                .build());

        try {
            Report report = generateReport();

            submissionPresenter.setTotalBytes(allAttachmentBytes);
            progressActionButton.setOnClickListener(cancelListener);
            progressActionButton.setText(getString(R.string.cancel));

            // If there are no attachments, don't show progressbar (for now)
            if (uris.size() == 0) {
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
            }

            // Stop editing
            setIsSending(true);

            if (OrbotHelper.isOrbotInstalled(getActivity()) && CAN_ATTEMPT_ORBOT) {
                try {
                    StrongOkHttpClientBuilder.forMaxSecurity(getActivity())
                            .withTorValidation()
                            .build(this);
                } catch (Exception ex) {
                    requestNonOrbotSubmission();
                }
            } else { // Don't use Tor/Orbot
                submissionPresenter.sendSubmission(report, uris);
            }
        } catch (InvalidClientIdException ex) {
            Log.e(TAG, "ClientIdError");
            showClientError();
        }
    }

    /*
     * If a client ID cannot be found, a report cannot be submitted.
     * This could potentially occur if user erases shared preferences file by clearing the app
     * cache in the middle of using the app, and does not re-register their ID with AWS and store
     * in shared preferences.
     */
    private void showClientError() {

        // GA
        ((BaseActivity) getActivity()).sendAnalyticsHitEvent(ACTION_VIEW, CATEGORY_INTERNAL, "Report error");

        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.error_client_id_title))
                .setMessage(getString(R.string.error_client_id_message))
                .setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void requestNonOrbotSubmission() {
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.error_orbot_title))
                .setMessage(getString(R.string.error_orbot_message))
                .setPositiveButton(getString(R.string.submit_without_orbot), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // prepare UI for sending ...
                        setIsSending(true);

                        submissionPresenter.sendSubmission(generateReport(), uris);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private Report generateReport() throws InvalidClientIdException {
        return Report.builder()
                .reportBody(getReportText())
                .attachmentNumber(uris.size())
                .withAttachments(uris.size() > 0)
                .email(email)
                .timestamp(new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.US)
                        .format(new Date()))
                .reportTags(tags)
                .build() // build first, then assign id
                .assignId() // hex-formatted secure random long
                .assignClientId(getClientId());
    }

    private String getReportText() {
        String reportString = reportBody.getText().toString();
        return (reportString.isEmpty()
                ? getString(R.string.no_report_body_supplied)
                : reportString);
    }

    private String getClientId() throws InvalidClientIdException {
        String clientId = preferences.getString(CLIENT_TOKEN_KEY, "");
        if (clientId.isEmpty()) {
            throw new InvalidClientIdException();
        } else {
            return clientId;
        }
    }

    /*
     * Set progress view to original
     */
    private void resetProgressView() {
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        progressTextStatus.setVisibility(View.GONE);
        progressTextHeader.setText(getString(R.string.progress_message_send));
        progressActionButton.setText(getString(R.string.cancel));
    }

    // If sending, prevent edits
    private void setIsSending(boolean isSending) {
        if (isSending) {
            parent.allowReporting(false); // temporarily disable while in the middle of sending
            loadingView.setVisibility(View.VISIBLE);
            progressWindowLayout.setVisibility(View.VISIBLE);
            reportBody.setInputType(InputType.TYPE_NULL);
        } else {
            parent.allowReporting(true);
            loadingView.setVisibility(View.GONE);
            progressWindowLayout.setVisibility(View.GONE);
            reportBody.setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }

    @Override
    public void showSuccess() {
        Log.d(TAG, "Success");
        progressBar.setVisibility(View.GONE);
        progressTextHeader.setText(getString(R.string.success));
        progressTextStatus.setText(getString(R.string.report_sent_confirmation));
        progressTextStatus.setVisibility(View.VISIBLE);
        progressActionButton.setText(getString(R.string.ok));
        progressActionButton.setOnClickListener(dismissOnSuccessListener);
    }

    /*
     * Show error with the option to retry. This is shown in cases of client-side error,
     * such as dropped connection.
     */
    @Override
    public void showError() {
        progressBar.setVisibility(View.GONE);
        progressTextHeader.setText(getString(R.string.error));
        progressTextStatus.setText(getString(R.string.error_submission));
        progressTextStatus.setVisibility(View.VISIBLE);
        progressActionButton.setOnClickListener(cancelListener);

        ((BaseApplication) getActivity().getApplication()).getDefaultTracker()
                .send(new HitBuilders.EventBuilder()
                        .setAction(ACTION_ERROR)
                        .setCategory(CATEGORY_ERROR)
                        .setLabel("Submission Error - no further retries")
                        .build());
    }

    /*
     * Show error message without offering a quick retry option. This is displayed
     * in case of server error or after multiple attempts have been made.
     */
    @Override
    public void showNoRetryError() {
        setIsSending(false);
    }

    /*
     * Make a second attempt at submitting report to S3 if unable to submit
     * to middleware.
     */
    @Override
    public void handleForbidden() {

        if (!hasDoneSecondAttempt) {
            Log.d(TAG, "Backup attempt...");
            submissionPresenter.setUseBackupAttempt(true);

            // Retrying... @Todo a better way to resubmit than to do all those calculations again
            submissionPresenter.sendSubmission(generateReport(), uris);
            hasDoneSecondAttempt = true;
            ((BaseApplication) getActivity().getApplication()).getDefaultTracker()
                    .send(new HitBuilders.EventBuilder()
                            .setAction(ACTION_ERROR)
                            .setCategory(CATEGORY_ERROR)
                            .setLabel("Submission Error - retrying")
                            .build());
        } else {
            Log.e(TAG, "Forbidden service even after second attempt");
            showNoRetryError(); // Show error without offering retry.
        }
    }

    @Override
    public void previewMedia(Uri uri) {
        int viewrequestCode = 3000;
        String type = null; // :(
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            type = getActivity().getContentResolver().getType(uri);
        }
        if (type == null) { // Try to find type from file extension if couldn't find it otherwise
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.toString())).toLowerCase(Locale.US);
        }
        intent.setDataAndType(uri, type);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, viewrequestCode);
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_no_preview_capable),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public String getContentType(Uri uri) {
        return mediaPresenter.getContentType(uri);
    }

    @Override
    public void removeAttachmentCallback(@NonNull Uri target) {
        try {
            long attachmentSize = getAttachmentSize(target);
            uris.remove(target);
            allAttachmentBytes -= attachmentSize;
            recyclerView.getAdapter().notifyDataSetChanged();
        } catch (FileReadException ex) {

            // The filesize was successfully determined when adding the file, so this is unexpected.
            // Remove problem file and recalculate total attachment size.
            uris.remove(target);
            recyclerView.getAdapter().notifyDataSetChanged();
            try {
                int newTotalBytes = 0;
                for (Uri uri : uris) {
                    newTotalBytes += getAttachmentSize(uri);
                }
                allAttachmentBytes = newTotalBytes;
            } catch (FileReadException e) {
                Log.e(TAG, "Unexpected error getting file size");
            }
        }
    }

    /*
     * Check if max attachment quota has been reached.
     *
     * @return      false if attachment quota has been reached, true otherwise.
     */
    private boolean canStillAddAttachments() {
        return allAttachmentBytes <= MAX_SUBMISSION_BYTES;
    }

    private long getAttachmentSize(@NonNull Uri target) throws FileReadException {
        if (target.getScheme().equals("content") || target.getScheme().equals("file")) {
            Cursor cursor = getActivity().getContentResolver().query(target, null, null, null, null);
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                boolean isColumn = cursor.moveToFirst();
                if (!isColumn) {
                    Log.e(TAG, "Could not resolve size column in target");
                    cursor.close();
                } else {
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();
                    return size;
                }
            }
            // Null cursor or unavailable column
            throw new FileReadException("Cannot determine file size");
        } else {
            // wrong type of URI
            Log.e(TAG, "Wrong type of URI--cannot determine size");
            throw new IllegalArgumentException("Wrong URI type " + target.getScheme());
        }
    }

    @Override
    public void onRequestSubmission() {
        if (hasNetworkAccess())
            showSubmissionDialog();
        else {
            showNoNetworkAlert();
        }
    }

    /*
     * True if network access detected and access complies with user preferences (i.e. restrictions on mobile data use)
     */
    private boolean hasNetworkAccess() {
        boolean canUseData = preferences.getBoolean(getString(R.string.upload_over_data_key), true);

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null
                && activeNetworkInfo.isConnectedOrConnecting()
                && (canUseData || (activeNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE));
    }

    private void showNoNetworkAlert() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.no_network_access_title))
                .setMessage(getString(R.string.no_network_access_message))
                .setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void showSubmissionDialog() {
        final View v = LayoutInflater.from(getActivity()).inflate(R.layout.report_details, null, false);
        final Button tagHr = v.findViewById(R.id.tag_hr);
        final Button tagNews = v.findViewById(R.id.tag_news);

        tagHr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setSelected(!v.isSelected());
            }
        });
        tagNews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setSelected(!v.isSelected());
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.confirm_submit_report))
                .setView(v)
                .setPositiveButton(getString(R.string.send_now), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        email = ((EditText) v.findViewById(R.id.edittext_email))
                                .getText()
                                .toString();
                        if (tagHr.isSelected()) {
                            tags.add(CustomEnum.REPORT_TAG.HUMAN_RIGHTS);
                        }
                        if (tagNews.isSelected()) {
                            tags.add(CustomEnum.REPORT_TAG.NEWS);
                        }
                        dialog.dismiss();
                        submit();
                    }
                })
                .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }

    /*
     * Check if file to add fits in file size requirements. If so add it; if not, throw exception.
     */
    private void addUriWithSizeCheck(Uri targetUri) throws OversizeFileException, FileReadException {
        if (targetUri != null) {
            long currentFileBytes = getAttachmentSize(targetUri);
            long checkSize = allAttachmentBytes + currentFileBytes;
            if (checkSize < MAX_SUBMISSION_BYTES) {
                allAttachmentBytes += currentFileBytes;
                uris.add(targetUri);
                recyclerView.getAdapter().notifyDataSetChanged();
            } else {
                throw new OversizeFileException();
            }
        }
    }

    /*
     * Display an alert message notifying users that they've reached their max submission size.
     */
    private void showOversizeFileAlert() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.error))
                .setMessage(getString(R.string.error_oversize_submission_message))
                .setNeutralButton(getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    /*
    * Display an alert message notifying users that they've reached their max submission size.
    */
    private void showFileError() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.error))
                .setMessage(getString(R.string.error_invalid_media_type))
                .setNeutralButton(getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.MediaHolder> {
        private IViewContract.ReviewMediaView reviewMediaView;
        private List<Uri> uris;

        public AttachmentAdapter(IViewContract.ReviewMediaView view, List<Uri> uris) {
            this.reviewMediaView = view;
            this.uris = uris;
        }

        @Override
        public MediaHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_attachment_pager, parent, false);
            final MediaHolder holder = new MediaHolder(view);

            holder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {

                    final PopupMenu menu = new PopupMenu(v.getContext(), holder.image);
                    menu.inflate(R.menu.popup_menu);
                    menu.show();
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Context context = v.getContext();
                            if (item.getTitle().equals
                                    (context.getString(R.string.remove_attachment))) {
                                reviewMediaView.removeAttachmentCallback
                                        (uris.get(holder.getAdapterPosition()));
                                menu.dismiss();
                                return true;
                            } else if (item.getTitle().equals
                                    (context.getString(R.string.dismiss))) {
                                menu.dismiss();
                                return true;
                            } else if (item.getTitle().equals
                                    (context.getString(R.string.view_attachment))) {
                                reviewMediaView.previewMedia
                                        (uris.get(holder.getAdapterPosition()));
                                menu.dismiss();
                                return true;
                            }
                            return false;
                        }
                    });
                }
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(final MediaHolder holder, final int position) {
            try {
                String type = reviewMediaView.getContentType(uris.get(position));
                if (type.contains("image")) {
                    holder.image.setImageResource(R.drawable.ic_camera_dark_24dp);
                } else if (type.contains("video")) {
                    holder.image.setImageResource(R.drawable.ic_videocam_black_24dp);
                } else if (type.contains("audio")) {
                    holder.image.setImageResource(R.drawable.ic_mic_black_24dp);
                } else {
                    holder.image.setImageResource(R.drawable.ic_attach_file_black_24dp);
                }
            } catch (TypeNotPresentException ex) {
                Log.e(TAG, "MimeType not present--will be able to preview but not send this file."); //@todo
                holder.image.setImageResource(R.drawable.ic_attach_file_black_24dp);
            }
        }

        @Override
        public int getItemCount() {
            return uris.size();
        }

        protected static class MediaHolder extends RecyclerView.ViewHolder {
            protected ImageView image;

            public MediaHolder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.imageview_media);
            }
        }
    }

    @Override
    public boolean isInProgress() {
        return (!reportBody.getText().toString().isEmpty() || uris.size() > 0);
    }

    @Override
    public void clearProgressWithWarning() {
        showCancelDialog();
    }


    @Override
    public void update(int percentProgress) {
        Log.d(TAG, "Progress: " + percentProgress + "%");
        if (progressBar != null) {
            progressBar.setProgress(percentProgress);
        }
    }

    @OnClick(R.id.progress_action_button)
    public void onClickCancel() {
        submissionPresenter.cancelSubmission();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ATTEMPT, hasDoneSecondAttempt);
        outState.putLong(ATTACHMENT_SIZE, allAttachmentBytes);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        parent.unregisterChild();
        parent = null;
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }
}
