package com.myname.prostream;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;

public class SiteManagerActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private JSONArray sitesArray;

    private MaterialToolbar toolbarSites;
    private LinearLayout sitesContainer;
    private FloatingActionButton btnAddSite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_manager);

        prefs = getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);

        // findViewById দিয়ে সরাসরি ভিউ ধরা হয়েছে, কোনো binding লাগবে না
        toolbarSites = findViewById(R.id.toolbarSites);
        sitesContainer = findViewById(R.id.sitesContainer);
        btnAddSite = findViewById(R.id.btnAddSite);

        toolbarSites.setNavigationOnClickListener(v -> finish());
        btnAddSite.setOnClickListener(v -> showEditDialog(-1));

        loadSites();
        applyDynamicTheme();
    }

    private void loadSites() {
        sitesContainer.removeAllViews();
        try {
            String jsonStr = prefs.getString("SITES_JSON", "[]");
            sitesArray = new JSONArray(jsonStr);

            for (int i = 0; i < sitesArray.length(); i++) {
                final int index = i;
                JSONObject site = sitesArray.getJSONObject(i);
                String name = site.optString("name", "Site");
                String url = site.optString("url", "");
                String colorHex = site.optString("color", "#0D9488");

                MaterialCardView card = new MaterialCardView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.bottomMargin = dpToPx(10);
                card.setLayoutParams(lp);
                card.setRadius(dpToPx(16));
                card.setCardElevation(dpToPx(2));
                card.setCardBackgroundColor(Color.parseColor("#161618"));

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                MaterialCardView iconCard = new MaterialCardView(this);
                iconCard.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)));
                iconCard.setRadius(dpToPx(12));
                iconCard.setCardElevation(0);
                iconCard.setCardBackgroundColor(AppUtil.parseColorSafe(colorHex, Color.parseColor("#0D9488")));

                TextView tvLetter = new TextView(this);
                tvLetter.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                ));
                tvLetter.setGravity(android.view.Gravity.CENTER);
                tvLetter.setTextColor(Color.WHITE);
                tvLetter.setTextSize(18);
                tvLetter.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLetter.setText(name.isEmpty() ? "S" : name.substring(0, 1).toUpperCase());
                iconCard.addView(tvLetter);

                LinearLayout textCol = new LinearLayout(this);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                textLp.leftMargin = dpToPx(14);
                textCol.setLayoutParams(textLp);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextColor(Color.WHITE);
                tvName.setTextSize(16);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                tvName.setMaxLines(1);
                tvName.setEllipsize(TextUtils.TruncateAt.END);

                TextView tvUrl = new TextView(this);
                tvUrl.setText(url);
                tvUrl.setTextColor(Color.parseColor("#888888"));
                tvUrl.setTextSize(12);
                tvUrl.setMaxLines(1);
                tvUrl.setEllipsize(TextUtils.TruncateAt.END);

                textCol.addView(tvName);
                textCol.addView(tvUrl);

                ImageView editIcon = new ImageView(this);
                editIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
                editIcon.setImageResource(R.drawable.ic_edit);
                editIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#666666")));

                row.addView(iconCard);
                row.addView(textCol);
                row.addView(editIcon);

                card.addView(row);
                card.setOnClickListener(v -> showEditDialog(index));

                sitesContainer.addView(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // SM9: সেভ ডেটা নষ্ট থাকলে খালি তালিকা ধরা হয় — ক্র্যাশ নয়
            sitesArray = new JSONArray();
        }
        // SM6: সাইট না থাকলে নির্দেশনা দেখানো হয়
        if (sitesArray == null) sitesArray = new JSONArray();
        if (sitesArray.length() == 0) {
            TextView hint = new TextView(this);
            hint.setText("এখনো কোনো সাইট নেই — + চেপে একটি যোগ করুন");
            hint.setTextColor(Color.parseColor("#888888"));
            hint.setTextSize(14);
            hint.setGravity(android.view.Gravity.CENTER);
            hint.setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32));
            sitesContainer.addView(hint);
        }
    }

    private void showEditDialog(final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(index == -1 ? "নতুন সাইট যোগ করুন" : "সাইট এডিট করুন");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10));

        final EditText nameInput = new EditText(this);
        nameInput.setHint("সাইটের নাম (যেমন: Prmovies)");
        layout.addView(nameInput);

        final EditText urlInput = new EditText(this);
        urlInput.setHint("URL (https://...)");
        layout.addView(urlInput);

        // SM2/SM5: ভ্যালিডেশন এরর ডায়ালগের ভেতরেই দেখানো হয়
        final TextView tvDialogError = new TextView(this);
        tvDialogError.setTextColor(Color.parseColor("#FF6B6B"));
        tvDialogError.setTextSize(13);
        tvDialogError.setPadding(0, dpToPx(6), 0, 0);
        tvDialogError.setVisibility(View.GONE);
        layout.addView(tvDialogError);

        final View colorPreview = new View(this);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(36)
        );
        previewLp.topMargin = dpToPx(12);
        colorPreview.setLayoutParams(previewLp);
        layout.addView(colorPreview);

        // SM4: hex রিডআউট
        final TextView tvHexReadout = new TextView(this);
        tvHexReadout.setTextColor(Color.parseColor("#888888"));
        tvHexReadout.setTextSize(12);
        tvHexReadout.setGravity(android.view.Gravity.CENTER);
        tvHexReadout.setPadding(0, dpToPx(6), 0, dpToPx(4));
        layout.addView(tvHexReadout);

        final SeekBar seekRed = createSeekBar(255);
        final SeekBar seekGreen = createSeekBar(255);
        final SeekBar seekBlue = createSeekBar(255);

        // SM4: প্রত্যেক স্লাইডারের লেবেল
        layout.addView(makeSliderLabel("লাল (R)"));
        layout.addView(seekRed);
        layout.addView(makeSliderLabel("সবুজ (G)"));
        layout.addView(seekGreen);
        layout.addView(makeSliderLabel("নীল (B)"));
        layout.addView(seekBlue);

        int initialColor = Color.parseColor("#0D9488");
        if (index != -1) {
            try {
                JSONObject s = sitesArray.getJSONObject(index);
                nameInput.setText(s.getString("name"));
                urlInput.setText(s.getString("url"));
                initialColor = Color.parseColor(s.optString("color", "#0D9488"));
            } catch (Exception ignored) {}
        }

        seekRed.setProgress(Color.red(initialColor));
        seekGreen.setProgress(Color.green(initialColor));
        seekBlue.setProgress(Color.blue(initialColor));
        updatePreviewBox(colorPreview, initialColor);
        tvHexReadout.setText(colorHex(initialColor));

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int c = Color.rgb(seekRed.getProgress(), seekGreen.getProgress(), seekBlue.getProgress());
                updatePreviewBox(colorPreview, c);
                tvHexReadout.setText(colorHex(c));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekRed.setOnSeekBarChangeListener(listener);
        seekGreen.setOnSeekBarChangeListener(listener);
        seekBlue.setOnSeekBarChangeListener(listener);

        builder.setView(layout);

        // SM2: Positive বোতামের লিসেনার show()-এর পরে বসানো হয়, যাতে
        // ভ্যালিডেশন ব্যর্থ হলে ডায়ালগ খোলা থাকে আর লেখা মুছে না যায়
        builder.setPositiveButton("সংরক্ষণ", null);

        if (index != -1) {
            builder.setNeutralButton("ডিলিট", null);
        }

        builder.setNegativeButton("বাতিল", null);
        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (sitesArray == null) sitesArray = new JSONArray();
            String name = nameInput.getText().toString().trim();
            String url = urlInput.getText().toString().trim();
            if (name.isEmpty() || url.isEmpty()) {
                showDialogError(tvDialogError, "সব তথ্য পূরণ করুন!");
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            // SM5: হোস্টে ডট থাকতে হবে (সঠিক ডোমেইন)
            Uri parsed = Uri.parse(url);
            String host = parsed != null ? parsed.getHost() : null;
            if (host == null || !host.contains(".")) {
                showDialogError(tvDialogError, "সঠিক URL লিখুন (যেমন https://example.com)");
                return;
            }
            // SM3: ডুপ্লিকেট চেক — স্কিম/www/শেষের স্ল্যাশ উপেক্ষা করে (নিজের এন্ট্রি বাদে)
            String normUrl = normalizeSiteUrl(url);
            try {
                for (int i = 0; i < sitesArray.length(); i++) {
                    if (i == index) continue;
                    if (normUrl.equals(normalizeSiteUrl(sitesArray.getJSONObject(i).optString("url", "")))) {
                        showDialogError(tvDialogError, "এই URL already আছে!");
                        return;
                    }
                }
            } catch (Exception ignored) {}

            int selectedColor = Color.rgb(seekRed.getProgress(), seekGreen.getProgress(), seekBlue.getProgress());
            String hexColor = colorHex(selectedColor);

            try {
                JSONObject obj = new JSONObject();
                obj.put("name", name);
                obj.put("url", url);
                obj.put("color", hexColor);

                if (index == -1) {
                    sitesArray.put(obj);
                } else {
                    sitesArray.put(index, obj);
                }

                prefs.edit().putString("SITES_JSON", sitesArray.toString()).apply();
                loadSites();
                dialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
                showDialogError(tvDialogError, "সংরক্ষণ করা যায়নি — আবার চেষ্টা করুন");
            }
        });

        if (index != -1) {
            // SM1: ডিলিটের আগে কনফার্মেশন
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("সাইট ডিলিট করবেন?")
                        .setMessage("এই সাইটটি তালিকা থেকে মুছে যাবে।")
                        .setPositiveButton("ডিলিট", (d2, which) -> {
                            if (sitesArray != null) {
                                sitesArray.remove(index);
                                prefs.edit().putString("SITES_JSON", sitesArray.toString()).apply();
                                loadSites();
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton("বাতিল", null)
                        .show();
            });
        }
    }

    private void showDialogError(TextView tvError, String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private TextView makeSliderLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.parseColor("#888888"));
        label.setTextSize(12);
        label.setPadding(0, dpToPx(8), 0, 0);
        return label;
    }

    // SM3: তুলনার জন্য URL স্বাভাবিক করা হয় (স্কিম/www/শেষের স্ল্যাশ বাদ, ছোট হাতের)
    private static String normalizeSiteUrl(String url) {
        if (url == null) return "";
        String s = url.trim().toLowerCase();
        if (s.startsWith("http://")) s = s.substring(7);
        else if (s.startsWith("https://")) s = s.substring(8);
        if (s.startsWith("www.")) s = s.substring(4);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String colorHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    private SeekBar createSeekBar(int max) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setPadding(0, dpToPx(8), 0, dpToPx(8));
        return sb;
    }

    private void updatePreviewBox(View view, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dpToPx(8));
        view.setBackground(gd);
    }

    private void applyDynamicTheme() {
        String hex = prefs.getString("APP_COLOR", "#0D9488");
        int color = AppUtil.parseColorSafe(hex, Color.parseColor("#0D9488"));

        getWindow().setStatusBarColor(color);
        toolbarSites.setTitleTextColor(color);
        toolbarSites.setNavigationIconTint(color);
        btnAddSite.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}