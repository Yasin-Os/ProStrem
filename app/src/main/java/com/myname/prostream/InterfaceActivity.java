package com.myname.prostream;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class InterfaceActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private int r, g, b;
    private EditText hexInput;
    private TextView tvHexReadout;

    private MaterialToolbar toolbarInterface;
    private View colorPreview;
    private SeekBar seekRed, seekGreen, seekBlue;
    private MaterialButton btnApplyCustomColor;
    private MaterialSwitch switchDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interface);

        prefs = getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);

        toolbarInterface = findViewById(R.id.toolbarInterface);
        colorPreview = findViewById(R.id.colorPreview);
        seekRed = findViewById(R.id.seekRed);
        seekGreen = findViewById(R.id.seekGreen);
        seekBlue = findViewById(R.id.seekBlue);
        btnApplyCustomColor = findViewById(R.id.btnApplyCustomColor);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        toolbarInterface.setNavigationOnClickListener(v -> finish());

        String currentColorHex = prefs.getString("APP_COLOR", "#00F5A0");
        int parsed = AppUtil.parseColorSafe(currentColorHex, Color.parseColor("#00F5A0"));
        r = Color.red(parsed);
        g = Color.green(parsed);
        b = Color.blue(parsed);

        updatePreviewColor();

        seekRed.setProgress(r);
        seekGreen.setProgress(g);
        seekBlue.setProgress(b);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                r = seekRed.getProgress();
                g = seekGreen.getProgress();
                b = seekBlue.getProgress();
                updatePreviewColor();
                // I1: স্লাইডার নাড়ালেই রং সেভ হয়ে যায় — Apply চাপতে হয় না
                if (fromUser) saveThemeColor();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekRed.setOnSeekBarChangeListener(listener);
        seekGreen.setOnSeekBarChangeListener(listener);
        seekBlue.setOnSeekBarChangeListener(listener);

        addHexAndPresets();

        // I1: রং এখনই সেভ হয়, তাই বোতামটা শুধু Done/বন্ধ করার কাজ করে
        btnApplyCustomColor.setText("সম্পন্ন");
        btnApplyCustomColor.setOnClickListener(v -> {
            saveThemeColor();
            finish();
        });

        // HomeActivity-র সঙ্গে ডিফল্ট মিল রাখা হয়েছে (false) — আগে এখানে true ছিল
        boolean isDark = prefs.getBoolean("DARK_MODE", false);
        switchDarkMode.setChecked(isDark);
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("DARK_MODE", checked).apply();
            // I2: টগল করলেই স্ক্রিনে প্রভাব দেখা যায়
            recreate();
        });

        applyThemeUI();
    }

    private void updatePreviewColor() {
        GradientDrawable previewBox = new GradientDrawable();
        previewBox.setColor(Color.rgb(r, g, b));
        previewBox.setCornerRadius(24f);
        colorPreview.setBackground(previewBox);
        if (tvHexReadout != null) {
            tvHexReadout.setText(String.format("#%06X", (0xFFFFFF & Color.rgb(r, g, b))));
        }
    }

    // I1: বর্তমান রং সঙ্গে সঙ্গে সেভ করা হয়
    private void saveThemeColor() {
        String hex = String.format("#%06X", (0xFFFFFF & Color.rgb(r, g, b)));
        prefs.edit().putString("APP_COLOR", hex).apply();
        applyThemeUI();
        if (tvHexReadout != null) tvHexReadout.setText(hex);
        if (hexInput != null && !hexInput.hasFocus()
                && !hex.equalsIgnoreCase(hexInput.getText().toString().trim())) {
            hexInput.setText(hex);
        }
    }

    // I3: hex ইনপুট + প্রিসেট রং (Apply বোতামের ঠিক আগে বসানো হয়)
    private void addHexAndPresets() {
        if (!(btnApplyCustomColor.getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) btnApplyCustomColor.getParent();
        int applyIdx = parent.indexOfChild(btnApplyCustomColor);

        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        secLp.bottomMargin = dpToPx(16);
        section.setLayoutParams(secLp);

        tvHexReadout = new TextView(this);
        tvHexReadout.setTextColor(Color.parseColor("#888888"));
        tvHexReadout.setTextSize(13);
        tvHexReadout.setGravity(android.view.Gravity.CENTER);
        tvHexReadout.setPadding(0, 0, 0, dpToPx(10));
        section.addView(tvHexReadout);

        LinearLayout hexRow = new LinearLayout(this);
        hexRow.setOrientation(LinearLayout.HORIZONTAL);
        hexRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        hexInput = new EditText(this);
        hexInput.setHint("#00F5A0");
        hexInput.setTextColor(Color.WHITE);
        hexInput.setTextSize(15);
        hexInput.setInputType(InputType.TYPE_CLASS_TEXT);
        hexInput.setSingleLine(true);
        LinearLayout.LayoutParams hexLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hexLp.rightMargin = dpToPx(8);
        hexInput.setLayoutParams(hexLp);
        hexRow.addView(hexInput);

        MaterialButton btnApplyHex = new MaterialButton(this);
        btnApplyHex.setText("লাগান");
        btnApplyHex.setTextColor(Color.WHITE);
        btnApplyHex.setOnClickListener(v -> applyHexInput());
        hexRow.addView(btnApplyHex);
        section.addView(hexRow);

        LinearLayout presetsRow = new LinearLayout(this);
        presetsRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsRow.setGravity(android.view.Gravity.CENTER);
        presetsRow.setPadding(0, dpToPx(12), 0, 0);
        String[] presets = {"#00F5A0", "#00D9FF", "#7C3AED", "#FF5252", "#FF9800", "#E91E63"};
        for (String preset : presets) {
            View swatch = new View(this);
            LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
            swLp.leftMargin = dpToPx(5);
            swLp.rightMargin = dpToPx(5);
            swatch.setLayoutParams(swLp);
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor(preset));
            gd.setCornerRadius(dpToPx(20));
            swatch.setBackground(gd);
            final String hex = preset;
            swatch.setOnClickListener(v -> applyPresetColor(hex));
            presetsRow.addView(swatch);
        }
        section.addView(presetsRow);

        parent.addView(section, applyIdx);
        updatePreviewColor();
    }

    private void applyHexInput() {
        if (hexInput == null) return;
        String raw = hexInput.getText().toString().trim();
        if (!raw.startsWith("#")) raw = "#" + raw;
        try {
            int c = Color.parseColor(raw);
            r = Color.red(c);
            g = Color.green(c);
            b = Color.blue(c);
            seekRed.setProgress(r);
            seekGreen.setProgress(g);
            seekBlue.setProgress(b);
            updatePreviewColor();
            saveThemeColor();
            Toast.makeText(this, "থিম কালার বদলে গেছে!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "সঠিক hex লিখুন (যেমন #00F5A0)", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyPresetColor(String hex) {
        try {
            int c = Color.parseColor(hex);
            r = Color.red(c);
            g = Color.green(c);
            b = Color.blue(c);
            seekRed.setProgress(r);
            seekGreen.setProgress(g);
            seekBlue.setProgress(b);
            updatePreviewColor();
            saveThemeColor();
        } catch (Exception ignored) {}
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void applyThemeUI() {
        String hex = prefs.getString("APP_COLOR", "#00F5A0");
        int color = AppUtil.parseColorSafe(hex, Color.parseColor("#00F5A0"));

        getWindow().setStatusBarColor(color);
        btnApplyCustomColor.setBackgroundTintList(ColorStateList.valueOf(color));
        toolbarInterface.setTitleTextColor(color);
        toolbarInterface.setNavigationIconTint(color);
        switchDarkMode.setThumbTintList(ColorStateList.valueOf(color));
    }
}