package org.hrana.hafez.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public final class MediaUtil {

    /*
         * Create a new File object pointed at the correct storage directory.
         * Default to a known directory on internal storage if desired directory does
         * not exist (i.e is not mounted).
         * @param   extension   File extension (.mp3, .mp4, .jpeg)
         * @param   directory   Desired directory for file storage
         * @param   defaultDir  Fallback directory if default directory is not available
         *
         */
    public static File createMediaFile(String extension, File externalDirectory, File defaultDir) throws IOException {

        // Create an image file name
        String fileName = Long.toHexString(new Random().nextLong());
        File storageDir;

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            storageDir = externalDirectory;
        } else {
            Log.e("MediaUtil", "External storage not mounted--default to app-only storage");
            storageDir = defaultDir;
        }
        if (null == storageDir || !storageDir.exists()) {
            storageDir.mkdirs();
        }

        return new File(storageDir, fileName + extension);
    }
}
