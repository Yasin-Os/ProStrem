package com.myname.prostream;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class InterfaceActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private int r, g, b;

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

        String currentColorHex = prefs.getString("APP_COLOR", "#0D9488");
        int parsed = Color.parseColor(currentColorHex);
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
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekRed.setOnSeekBarChangeListener(listener);
        seekGreen.setOnSeekBarChangeListener(listener);
        seekBlue.setOnSeekBarChangeListener(listener);

        btnApplyCustomColor.setOnClickListener(v -> {
            String hex = String.format("#%06X", (0xFFFFFF & Color.rgb(r, g, b)));
            prefs.edit().putString("APP_COLOR", hex).apply();
            applyThemeUI();
            Toast.makeText(this, "থিম কালার সফলভাবে সংরক্ষিত!", Toast.LENGTH_SHORT).show();
        });

        boolean isDark = prefs.getBoolean("DARK_MODE", true);
        switchDarkMode.setChecked(isDark);
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("DARK_MODE", checked).apply();
        });

        applyThemeUI();
    }

    private void updatePreviewColor() {
        GradientDrawable previewBox = new GradientDrawable();
        previewBox.setColor(Color.rgb(r, g, b));
        previewBox.setCornerRadius(24f);
        colorPreview.setBackground(previewBox);
    }

    private void applyThemeUI() {
        String hex = prefs.getString("APP_COLOR", "#0D9488");
        int color = Color.parseColor(hex);

        getWindow().setStatusBarColor(color);
        btnApplyCustomColor.setBackgroundTintList(ColorStateList.valueOf(color));
        toolbarInterface.setTitleTextColor(color);
        toolbarInterface.setNavigationIconTint(color);
        switchDarkMode.setThumbTintList(ColorStateList.valueOf(color));
    }
}