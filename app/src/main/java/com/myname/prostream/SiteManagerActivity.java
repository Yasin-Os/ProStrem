package com.myname.prostream;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
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
                iconCard.setCardBackgroundColor(Color.parseColor(colorHex));

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

                TextView tvUrl = new TextView(this);
                tvUrl.setText(url);
                tvUrl.setTextColor(Color.parseColor("#888888"));
                tvUrl.setTextSize(12);

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
        }
    }

    private void showEditDialog(final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(index == -1 ? "নতুন সাইট যোগ করুন" : "সাইট এডিট করুন");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10));

        final EditText nameInput = new EditText(this);
        nameInput.setHint("সাইটের নাম (যেমন: Medicare)");
        layout.addView(nameInput);

        final EditText urlInput = new EditText(this);
        urlInput.setHint("URL (https://...)");
        layout.addView(urlInput);

        final View colorPreview = new View(this);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(36)
        );
        previewLp.topMargin = dpToPx(12);
        colorPreview.setLayoutParams(previewLp);
        layout.addView(colorPreview);

        final SeekBar seekRed = createSeekBar(255);
        final SeekBar seekGreen = createSeekBar(255);
        final SeekBar seekBlue = createSeekBar(255);

        layout.addView(seekRed);
        layout.addView(seekGreen);
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

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int c = Color.rgb(seekRed.getProgress(), seekGreen.getProgress(), seekBlue.getProgress());
                updatePreviewBox(colorPreview, c);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekRed.setOnSeekBarChangeListener(listener);
        seekGreen.setOnSeekBarChangeListener(listener);
        seekBlue.setOnSeekBarChangeListener(listener);

        builder.setView(layout);

        builder.setPositiveButton("সংরক্ষণ", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String url = urlInput.getText().toString().trim();
            if (name.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "সব তথ্য পূরণ করুন!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            int selectedColor = Color.rgb(seekRed.getProgress(), seekGreen.getProgress(), seekBlue.getProgress());
            String hexColor = String.format("#%06X", (0xFFFFFF & selectedColor));

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        if (index != -1 && sitesArray.length() > 1) {
            builder.setNeutralButton("ডিলিট", (dialog, which) -> {
                sitesArray.remove(index);
                prefs.edit().putString("SITES_JSON", sitesArray.toString()).apply();
                loadSites();
            });
        }

        builder.setNegativeButton("বাতিল", null);
        builder.show();
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
        int color = Color.parseColor(hex);

        getWindow().setStatusBarColor(color);
        toolbarSites.setTitleTextColor(color);
        toolbarSites.setNavigationIconTint(color);
        btnAddSite.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}