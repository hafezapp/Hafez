package org.hrana.hafez.presenter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.hrana.hafez.Constants;
import org.hrana.hafez.model.LegalContact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide legal contacts.
 */

public class LegalContactPresenter {
    private static final int SCHEM_LEN = 10;
    private static final String TAG = "ContactPresenter";

    public List<LegalContact> parseContacts(Context context) {
        List<LegalContact> contacts = new ArrayList<>();
        BufferedReader reader = null;
        try {
            File f = new File(context.getFilesDir().getAbsolutePath() + "/" + Constants.DOWNLOAD_LAWYER_LIST_KEY + Constants.LAWYERS_CSV);
            if (!f.exists()) {

                Log.i(TAG, "Could not locate updated lawyers file--reading from backup...");

                // There's a backup file
                reader = new BufferedReader
                        (new InputStreamReader
                                (context.getAssets().open(Constants.LAWYERS_CSV)));
            } else {
                Log.d(TAG, "Found updated contact file.");
                reader = new BufferedReader(new FileReader(f));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",");

                if (row.length == SCHEM_LEN) { // Correct number of columns
                    contacts.add(LegalContact.builder()
                            .name(row[9])
                            .address(row[2])
                            .phone(row[0])
                            .mobile(row[1])
                            .email(row[4])
                            .lawType(getLawTypes(new String[]{row[8], row[7], row[6], row[5], row[4], row[3]}))
                    .build());
                }
            }
        }
        catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
            // handle exception
        }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException ex) {
                Log.e(TAG, ex.getMessage());
                // handle exception
            }
        }
        return contacts;
    }

    private List<LegalContact.LAW_TYPE> getLawTypes(String[] row) {
        List<LegalContact.LAW_TYPE> mList = new ArrayList<>();
        for (int i = 0; i < row.length; i++) {
            if (!TextUtils.isEmpty(row[i])) {
                mList.add(LegalContact.LAW_TYPE.byValue(i + 1));
            }
        }
        return mList;
    }
}
