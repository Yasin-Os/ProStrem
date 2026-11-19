package com.myname.prostream;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/** Shared small helpers: safe color parsing and PIN storage.
 *  The PIN is never stored in plaintext: SHA-256(salt + ":" + pin) with a
 *  per-install random salt. Legacy plaintext values are migrated to a hash
 *  on the next successful unlock, then deleted.
 *
 *  PIN সংক্রান্ত সবকিছু (হ্যাশ, সল্ট, ব্যর্থ চেষ্টা, লকআউট) আলাদা prefs
 *  ফাইলে (ProStreamLockPrefs) থাকে — ব্যাকআপ থেকে সেটা বাদ দেওয়া হয়
 *  (backup_rules.xml / data_extraction_rules.xml), তাই আনইনস্টল করে
 *  আবার বসালে ভুলে যাওয়া PIN ফিরে আসে না। */
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

    private static final String LOCK_PREFS_NAME = "ProStreamLockPrefs";
    private static final String MAIN_PREFS_NAME = "ProStreamPrefs";

    private static final String KEY_PIN_HASH = "APP_PASSWORD_HASH";
    private static final String KEY_PIN_SALT = "APP_PASSWORD_SALT";
    private static final String KEY_PIN_FAILS = "PIN_FAIL_COUNT";
    private static final String KEY_PIN_LOCKOUT_UNTIL = "PIN_LOCKOUT_UNTIL";
    private static final int MAX_PIN_ATTEMPTS = 5;

    /** PIN-এর prefs — প্রথমবার পুরোনো জায়গা (ProStreamPrefs) থেকে PIN-এর
     *  চাবিগুলো এখানে সরিয়ে আনা হয়, তারপর আর সেখানে থাকে না। */
    public static SharedPreferences lockPrefs(Context ctx) {
        SharedPreferences lock = ctx.getSharedPreferences(LOCK_PREFS_NAME, Context.MODE_PRIVATE);
        migratePinFromMainPrefs(ctx, lock);
        return lock;
    }

    private static void migratePinFromMainPrefs(Context ctx, SharedPreferences lock) {
        SharedPreferences main = ctx.getSharedPreferences(MAIN_PREFS_NAME, Context.MODE_PRIVATE);
        if (!main.contains(KEY_PIN_HASH) && !main.contains(KEY_PIN_SALT)
                && !main.contains("APP_PASSWORD") && !main.contains(KEY_PIN_FAILS)
                && !main.contains(KEY_PIN_LOCKOUT_UNTIL)) {
            return;
        }
        SharedPreferences.Editor lockEd = lock.edit();
        if (main.contains(KEY_PIN_HASH)) lockEd.putString(KEY_PIN_HASH, main.getString(KEY_PIN_HASH, ""));
        if (main.contains(KEY_PIN_SALT)) lockEd.putString(KEY_PIN_SALT, main.getString(KEY_PIN_SALT, ""));
        if (main.contains("APP_PASSWORD")) lockEd.putString("APP_PASSWORD", main.getString("APP_PASSWORD", ""));
        if (main.contains(KEY_PIN_FAILS)) lockEd.putInt(KEY_PIN_FAILS, main.getInt(KEY_PIN_FAILS, 0));
        if (main.contains(KEY_PIN_LOCKOUT_UNTIL)) lockEd.putLong(KEY_PIN_LOCKOUT_UNTIL, main.getLong(KEY_PIN_LOCKOUT_UNTIL, 0));
        lockEd.apply();
        main.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).remove("APP_PASSWORD")
                .remove(KEY_PIN_FAILS).remove(KEY_PIN_LOCKOUT_UNTIL).apply();
    }

    /** True if a PIN has ever been set (hashed or legacy plaintext). */
    public static boolean hasPin(Context ctx) {
        SharedPreferences prefs = lockPrefs(ctx);
        return prefs.contains(KEY_PIN_HASH) || prefs.contains("APP_PASSWORD");
    }

    /** Store a PIN as a salted hash and drop any legacy plaintext value. */
    public static void savePin(Context ctx, String pin) {
        SharedPreferences prefs = lockPrefs(ctx);
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
    public static boolean verifyPin(Context ctx, String input) {
        SharedPreferences prefs = lockPrefs(ctx);
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
            savePin(ctx, input);
            return true;
        }
        if (legacy != null) recordPinFailure(prefs);
        return false;
    }

    public static boolean isPinLockedOut(Context ctx) {
        return System.currentTimeMillis() < lockPrefs(ctx).getLong(KEY_PIN_LOCKOUT_UNTIL, 0);
    }

    public static long pinLockoutRemainingMs(Context ctx) {
        return Math.max(0, lockPrefs(ctx).getLong(KEY_PIN_LOCKOUT_UNTIL, 0) - System.currentTimeMillis());
    }

    public static int pinFailuresLeft(Context ctx) {
        return Math.max(0, MAX_PIN_ATTEMPTS - lockPrefs(ctx).getInt(KEY_PIN_FAILS, 0));
    }

    private static boolean isPinLockedOut(SharedPreferences prefs) {
        return System.currentTimeMillis() < prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0);
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
