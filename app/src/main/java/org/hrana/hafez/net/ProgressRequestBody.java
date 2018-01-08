package org.hrana.hafez.net;

import android.os.Handler;
import android.os.Looper;

import org.hrana.hafez.presenter.contract.IViewContract;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {
    private IViewContract.UploadListener mListener;
    private File mFile;

    public ProgressRequestBody(File mFile, IViewContract.UploadListener mListener) {
        this.mListener = mListener;
        this.mFile = mFile;
    }

    // Convenience to initialize in api call the same way as regular RequestBody.
    public static ProgressRequestBody create(File file, IViewContract.UploadListener listener) {
        return new ProgressRequestBody(file, listener);
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("multipart/form-data");
    }

    // thanks to https://stackoverflow.com/questions/33338181/is-it-possible-to-show-progress-bar-when-upload-image-via-retrofit-2
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = mFile.length();
        byte[] buffer = new byte[4096];
        FileInputStream in = new FileInputStream(mFile);
        long uploaded = 0;

        try {
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {
                uploaded += read;
                sink.write(buffer, 0, read);

                // update progress on UI thread
                handler.post(new ProgressUpdater(uploaded, fileLength));
            }
        } finally {
            in.close();
        }

    }

    private class ProgressUpdater implements Runnable {
        private long mUploaded;
        private long mTotal;

        public ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }

        @Override
        public void run() {
            mListener.update((int) (100 * mUploaded / mTotal));
        }
    }
}
