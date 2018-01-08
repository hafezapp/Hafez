package org.hrana.hafez.util;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Set locale
 * { https://stackoverflow.com/users/1188571/roberto-b }
 */
public class LocaleUtils {
    private static Locale sLocale;

    public static void setLocale(Locale locale) {
        if (locale != null) {
            sLocale = locale;
            Locale.setDefault(sLocale);
        }
    }

    public static void updateLocale(ContextThemeWrapper wrapper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && sLocale != null) {
            Configuration configuration = new Configuration();
            configuration.setLocale(sLocale);

            // config.locale deprecated in Android N
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLayoutDirection(configuration.getLocales().get(0));

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLayoutDirection(configuration.locale);
            }
            wrapper.applyOverrideConfiguration(configuration);
        }
    }

    public static void updateLocale(Application app, Configuration configuration) {
        if (sLocale != null && Build.VERSION.SDK_INT
                < Build.VERSION_CODES.JELLY_BEAN_MR1) {

            //Wrapping the configuration to avoid Activity endless loop
            Configuration config = new Configuration(configuration);
            config.setLocale(sLocale);
            Resources res = app.getBaseContext().getResources();
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
    }

    /**
     * Get relative time span.
     *
     * @param time Time.
     * @return String of relative time span.
     */
    public static CharSequence getRelativeTimeSpan(String time) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            setLocale(new Locale("fa", "ir"));
            return DateUtils.getRelativeTimeSpanString(dateFormat.parse(time).getTime(), System.currentTimeMillis(), 0L,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
        } catch (ParseException exception) {
            Log.e("EntryUtil", exception.getMessage());
        }
        return null;
    }
}