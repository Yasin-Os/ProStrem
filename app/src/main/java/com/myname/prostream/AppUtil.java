package com.myname.prostream;

import android.content.SharedPreferences;
import android.graphics.Color;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/** Shared small helpers: safe color parsing and PIN storage.
 *  The PIN is never stored in plaintext: SHA-256(salt + ":" + pin) with a
 *  per-install random salt. Legacy plaintext values are migrated to a hash
 *  on the next successful unlock, then deleted. */
public final class AppUtil {

    private AppUtil() {}

    public static int parseColorSafe(String colorStr, int fallback) {
        try {
            return Color.parseColor(colorStr);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ---- App lock PIN ----

    private static final String KEY_PIN_HASH = "APP_PASSWORD_HASH";
    private static final String KEY_PIN_SALT = "APP_PASSWORD_SALT";
    private static final String KEY_PIN_FAILS = "PIN_FAIL_COUNT";
    private static final String KEY_PIN_LOCKOUT_UNTIL = "PIN_LOCKOUT_UNTIL";
    private static final int MAX_PIN_ATTEMPTS = 5;

    /** True if a PIN has ever been set (hashed or legacy plaintext). */
    public static boolean hasPin(SharedPreferences prefs) {
        return prefs.contains(KEY_PIN_HASH) || prefs.contains("APP_PASSWORD");
    }

    /** Store a PIN as a salted hash and drop any legacy plaintext value. */
    public static void savePin(SharedPreferences prefs, String pin) {
        String salt = generateSalt();
        String hash = sha256Hex(salt + ":" + pin);
        prefs.edit()
                .putString(KEY_PIN_SALT, salt)
                .putString(KEY_PIN_HASH, hash)
                .remove("APP_PASSWORD")
                .apply();
        resetPinFailures(prefs);
    }

    /** Verify a PIN. Returns false while locked out. Migrates legacy plaintext on success. */
    public static boolean verifyPin(SharedPreferences prefs, String input) {
        if (isPinLockedOut(prefs)) return false;
        String hash = prefs.getString(KEY_PIN_HASH, null);
        if (hash != null) {
            String salt = prefs.getString(KEY_PIN_SALT, "");
            if (hash.equals(sha256Hex(salt + ":" + input))) {
                resetPinFailures(prefs);
                return true;
            }
            recordPinFailure(prefs);
            return false;
        }
        String legacy = prefs.getString("APP_PASSWORD", null);
        if (legacy != null && legacy.equals(input)) {
            savePin(prefs, input);
            return true;
        }
        if (legacy != null) recordPinFailure(prefs);
        return false;
    }

    public static boolean isPinLockedOut(SharedPreferences prefs) {
        return System.currentTimeMillis() < prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0);
    }

    public static long pinLockoutRemainingMs(SharedPreferences prefs) {
        return Math.max(0, prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0) - System.currentTimeMillis());
    }

    public static int pinFailuresLeft(SharedPreferences prefs) {
        return Math.max(0, MAX_PIN_ATTEMPTS - prefs.getInt(KEY_PIN_FAILS, 0));
    }

    private static void recordPinFailure(SharedPreferences prefs) {
        int fails = prefs.getInt(KEY_PIN_FAILS, 0) + 1;
        SharedPreferences.Editor ed = prefs.edit().putInt(KEY_PIN_FAILS, fails);
        if (fails >= MAX_PIN_ATTEMPTS) {
            int steps = fails - MAX_PIN_ATTEMPTS;
            long lockMs = Math.min(300_000L, 30_000L << steps); // 30s, 60s, 120s... max 5 min
            ed.putLong(KEY_PIN_LOCKOUT_UNTIL, System.currentTimeMillis() + lockMs);
        }
        ed.apply();
    }

    private static void resetPinFailures(SharedPreferences prefs) {
        prefs.edit().remove(KEY_PIN_FAILS).remove(KEY_PIN_LOCKOUT_UNTIL).apply();
    }

    private static String generateSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder(32);
        for (byte x : b) sb.append(String.format(Locale.US, "%02x", x));
        return sb.toString();
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(64);
            for (byte x : d) sb.append(String.format(Locale.US, "%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
