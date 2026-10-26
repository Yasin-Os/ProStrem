package com.myname.prostream;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

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

        clearCacheButton.setOnClickListener(v -> {
            new WebView(this).clearCache(true);
            Toast.makeText(this, "ক্যাশ মেমোরি ক্লিয়ার করা হয়েছে!", Toast.LENGTH_SHORT).show();
        });

        applyThemeColor();
        runEntryAnimations();
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
                .setMessage("বিজ্ঞাপনমুক্ত ব্রাউজিংয়ের জন্য আপনার ফোনের Private DNS সেটিংসে নিচের ঠিকানাটি পেস্ট করুন:\n\np2.freedns.controld.com")
                .setPositiveButton("কপি করে সেটিংসে যান", (dialog, which) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("DNS", "p2.freedns.controld.com"));
                    Toast.makeText(this, "DNS লিংক কপি হয়েছে!", Toast.LENGTH_SHORT).show();
                    try {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }
                })
                .setNegativeButton("বন্ধ করুন", null)
                .show();
    }

    private void applyThemeColor() {
        String hex = prefs.getString("APP_COLOR", "#0D9488");
        int color = Color.parseColor(hex);

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