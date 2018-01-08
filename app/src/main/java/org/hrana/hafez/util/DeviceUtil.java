package org.hrana.hafez.util;

import android.os.Build;
import android.util.Log;

/**
 * Utility class for helping determine whether hafez is running on a physical device or an emulator.
 * Note that this does not comprehensively identify all emulators, and therefore the app cannot
 * guarantee that it is not running on an emulator; rather it provides common checks against typical
 * emulator settings.
 *
 * TL;DR: While running hafez on an emulator cannot be prohibited, it is not encouraged.
 */
public final class DeviceUtil {
    private static final String TAG = "DeviceUtil";
    public static boolean hasEmulatorFlags() {

        try {
            if (Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.toLowerCase().contains("droid4x")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.HARDWARE.equals("goldfish")
                    || Build.HARDWARE.equals("vbox86")
                    || Build.PRODUCT.equals("google_sdk")
                    || Build.PRODUCT.equals("sdk_x86")
                    || Build.PRODUCT.equals("vbox86p")
                    || Build.BOARD.toLowerCase().contains("nox")
                    || Build.BOOTLOADER.toLowerCase().contains("nox")
                    || Build.HARDWARE.toLowerCase().contains("nox")
                    || Build.PRODUCT.toLowerCase().contains("nox")
                    || Build.SERIAL.toLowerCase().contains("nox")
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || Build.HARDWARE.contains("golfish")
                    || Build.HARDWARE.contains("ranchu")) {
                return true;
            }
        } catch (Exception e) {

            // It's helpful if we can discover emulators, but not mandatory and not worth crashing over.
            Log.e(TAG, "Encountered error while checking if running on Emulator: " + e.getMessage());
        }
        return false;
    }
}
