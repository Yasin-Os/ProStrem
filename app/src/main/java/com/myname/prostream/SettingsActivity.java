package com.myname.prostream;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private ActivityResultLauncher<String[]> restorePickerLauncher;

    private MaterialToolbar toolbarSettings;
    private MaterialCardView cardSiteLinks, cardInterface, cardAppLock, cardSmartDNS;
    private MaterialButton clearCacheButton;
    private ImageView iconLink, iconTheme, iconSecurity, iconDns;
    private View cardGroup1, cardGroup2;
    private TextView devCredit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);

        restorePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) handleRestoreUri(uri);
                });

        toolbarSettings = findViewById(R.id.toolbarSettings);
        cardSiteLinks = findViewById(R.id.cardSiteLinks);
        cardInterface = findViewById(R.id.cardInterface);
        cardAppLock = findViewById(R.id.cardAppLock);
        cardSmartDNS = findViewById(R.id.cardSmartDNS);
        clearCacheButton = findViewById(R.id.clearCacheButton);
        iconLink = findViewById(R.id.iconLink);
        iconTheme = findViewById(R.id.iconTheme);
        iconSecurity = findViewById(R.id.iconSecurity);
        iconDns = findViewById(R.id.iconDns);
        cardGroup1 = findViewById(R.id.cardGroup1);
        cardGroup2 = findViewById(R.id.cardGroup2);
        devCredit = findViewById(R.id.devCredit);

        toolbarSettings.setNavigationOnClickListener(v -> finish());

        cardSiteLinks.setOnClickListener(v ->
                startActivity(new Intent(this, SiteManagerActivity.class)));

        cardInterface.setOnClickListener(v ->
                startActivity(new Intent(this, InterfaceActivity.class)));

        cardAppLock.setOnClickListener(v ->
                startActivity(new Intent(this, AppLockActivity.class)));

        cardSmartDNS.setOnClickListener(v -> showDnsSetupDialog());

        clearCacheButton.setOnClickListener(v -> showClearCacheConfirmDialog());

        addUpdateRow();
        addBackupRestoreCard();

        applyThemeColor();
        refreshCardStates();
        runEntryAnimations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCardStates();
    }

    private void runEntryAnimations() {
        View[] views = {cardGroup1, cardGroup2, clearCacheButton, devCredit};
        for (int i = 0; i < views.length; i++) {
            if (views[i] == null) continue;
            views[i].setTranslationY(80f);
            views[i].setAlpha(0f);

            ObjectAnimator move = ObjectAnimator.ofFloat(views[i], "translationY", 0f);
            ObjectAnimator fade = ObjectAnimator.ofFloat(views[i], "alpha", 1f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(move, fade);
            set.setDuration(400);
            set.setStartDelay(60L * i);
            set.setInterpolator(new DecelerateInterpolator());
            set.start();
        }
    }

    private void showDnsSetupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Smart AdBlock (DNS) সেটআপ")
                .setMessage("বিজ্ঞাপনমুক্ত ব্রাউজিংয়ের জন্য ফোনের Settings → Network & internet → Private DNS-এ গিয়ে \"Private DNS provider hostname\" বেছে নিয়ে নিচের ঠিকানাটি পেস্ট করুন:\n\np2.freedns.controld.com")
                .setPositiveButton("কপি করে সেটিংসে যান", (dialog, which) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("DNS", "p2.freedns.controld.com"));
                    Toast.makeText(this, "DNS লিংক কপি হয়েছে!", Toast.LENGTH_SHORT).show();
                    try {
                        startActivity(new Intent("android.settings.PRIVATE_DNS_SETTINGS"));
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        } catch (Exception ignored) {}
                    }
                })
                .setNegativeButton("বন্ধ করুন", null)
                .show();
    }

    // S1: ক্যাশ মোছার আগে কনফার্মেশন — লগআউট ও সাইট ডেটা মুছে যাবে
    private void showClearCacheConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("ক্যাশ ক্লিয়ার করবেন?")
                .setMessage("ক্যাশ মোছার সঙ্গে কুকি ও সাইটের সংরক্ষিত ডেটাও মুছে যাবে — সব সাইট থেকে লগআউট হয়ে যাবেন।")
                .setPositiveButton("ক্লিয়ার করুন", (dialog, which) -> clearCacheNow())
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void clearCacheNow() {
        // S6: কাজ শেষে throwaway WebView ধ্বংস করা হয়
        WebView wv = new WebView(this);
        wv.clearCache(true);
        wv.destroy();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        Toast.makeText(this, "ক্যাশ ও কুকি ক্লিয়ার করা হয়েছে!", Toast.LENGTH_SHORT).show();
    }

    // S3: Settings থেকে আপডেট চেক — HomeActivity-তে OPEN_UPDATE_DIALOG পাঠানো হয়
    private void addUpdateRow() {
        if (cardGroup1 == null || !(cardGroup1.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup inner = (ViewGroup) cardGroup1.getChildAt(0);

        View divider = new View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        divLp.leftMargin = dpToPx(60);
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(Color.parseColor("#222222"));
        inner.addView(divider);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        setClickableBackground(row);
        row.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("OPEN_UPDATE_DIALOG", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("Check for updates");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView sub = new TextView(this);
        sub.setText("নতুন ভার্সন আছে কি না দেখুন");
        sub.setTextColor(Color.parseColor("#777777"));
        sub.setTextSize(13);

        textCol.addView(title);
        textCol.addView(sub);
        row.addView(textCol);
        inner.addView(row);
    }

    // S4: সাইটের ব্যাকআপ ও রিস্টোর কার্ড (devCredit-এর ঠিক আগে বসানো হয়)
    private void addBackupRestoreCard() {
        if (devCredit == null || !(devCredit.getParent() instanceof ViewGroup)) return;
        ViewGroup root = (ViewGroup) devCredit.getParent();

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(28);
        card.setLayoutParams(lp);
        card.setRadius(dpToPx(20));
        card.setCardElevation(0);
        card.setCardBackgroundColor(Color.parseColor("#161618"));
        card.setStrokeColor(Color.parseColor("#222222"));
        card.setStrokeWidth(dpToPx(1));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);

        inner.addView(makeBackupRow("ব্যাকআপ নিন", "সাইটের তালিকা ফাইলে সংরক্ষণ করুন", v -> backupSites()));
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        divider.setBackgroundColor(Color.parseColor("#222222"));
        inner.addView(divider);
        inner.addView(makeBackupRow("রিস্টোর করুন", "ব্যাকআপ ফাইল থেকে সাইট ফিরিয়ে আনুন", v -> {
            if (restorePickerLauncher != null) {
                restorePickerLauncher.launch(new String[]{"application/json"});
            }
        }));

        card.addView(inner);
        root.addView(card, root.indexOfChild(devCredit));
    }

    private LinearLayout makeBackupRow(String titleText, String subText, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        setClickableBackground(row);
        row.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        row.setOnClickListener(onClick);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView sub = new TextView(this);
        sub.setText(subText);
        sub.setTextColor(Color.parseColor("#777777"));
        sub.setTextSize(13);

        textCol.addView(title);
        textCol.addView(sub);
        row.addView(textCol);
        return row;
    }

    private void setClickableBackground(View v) {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            v.setBackgroundResource(tv.resourceId);
        }
    }

    private void backupSites() {
        String json = prefs.getString("SITES_JSON", "[]");
        if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) {
            Toast.makeText(this, "ব্যাকআপ করার মতো কোনো সাইট নেই", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = "ProStream-sites-backup.json";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ProStream");
                values.put(MediaStore.Downloads.IS_PENDING, 1);
                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri itemUri = getContentResolver().insert(collection, values);
                if (itemUri == null) throw new Exception("MediaStore insert failed");
                try (OutputStream os = getContentResolver().openOutputStream(itemUri)) {
                    if (os == null) throw new Exception("Cannot open output stream");
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                ContentValues done = new ContentValues();
                done.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(itemUri, done, null, null);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ProStream");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, fileName);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(json.getBytes(StandardCharsets.UTF_8));
                }
            }
            Toast.makeText(this, "ব্যাকআপ সংরক্ষিত হয়েছে (Downloads/ProStream)", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "ব্যাকআপ সংরক্ষণ করা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRestoreUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.optString("name", "").trim().isEmpty()
                        || o.optString("url", "").trim().isEmpty()) {
                    throw new Exception("Invalid site entry");
                }
            }
            prefs.edit().putString("SITES_JSON", arr.toString()).apply();
            refreshCardStates();
            Toast.makeText(this, "ব্যাকআপ রিস্টোর হয়েছে (" + arr.length() + "টি সাইট)", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "ব্যাকআপ ফাইলটি সঠিক নয়", Toast.LENGTH_SHORT).show();
        }
    }

    // S5: প্রত্যেক কার্ডে বর্তমান অবস্থা দেখানো হয়; Settings-এ ফিরলেই আপডেট হয়
    private void refreshCardStates() {
        setCardSubtitle(cardSiteLinks, siteCountText());
        String hex = prefs.getString("APP_COLOR", "#00F5A0");
        setCardSubtitle(cardInterface, "বর্তমান রং: " + hex.toUpperCase());
        int lockType = prefs.getInt("LOCK_TYPE", 1);
        String lockState = lockType == 0 ? "লক বন্ধ আছে"
                : (AppUtil.hasPin(prefs) ? "PIN লক চালু আছে" : "লক চালু আছে (PIN সেট হয়নি)");
        setCardSubtitle(cardAppLock, lockState);
        setCardSubtitle(cardSmartDNS, "হোস্ট: p2.freedns.controld.com");
    }

    private String siteCountText() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("SITES_JSON", "[]"));
            int n = arr.length();
            return n == 0 ? "কোনো সাইট নেই" : n + "টি সাইট সংরক্ষিত আছে";
        } catch (Exception ignored) {
            return "কোনো সাইট নেই";
        }
    }

    private void setCardSubtitle(View card, String text) {
        TextView sub = findSubtitleTextView(card);
        if (sub != null) sub.setText(text);
    }

    // কার্ডের সাবটাইটেল (#777777 রঙের TextView) খুঁজে বের করা হয়
    private TextView findSubtitleTextView(View v) {
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            if (tv.getCurrentTextColor() == Color.parseColor("#777777")) return tv;
            return null;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                TextView found = findSubtitleTextView(g.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void applyThemeColor() {
        String hex = prefs.getString("APP_COLOR", "#0D9488");
        int color = AppUtil.parseColorSafe(hex, Color.parseColor("#0D9488"));

        getWindow().setStatusBarColor(color);
        toolbarSettings.setTitleTextColor(color);
        toolbarSettings.setNavigationIconTint(color);

        ColorStateList tint = ColorStateList.valueOf(color);
        iconLink.setImageTintList(tint);
        iconTheme.setImageTintList(tint);
        iconSecurity.setImageTintList(tint);
        iconDns.setImageTintList(tint);
    }
}