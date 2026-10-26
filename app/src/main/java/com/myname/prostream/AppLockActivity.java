package com.myname.prostream;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class AppLockActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    private MaterialToolbar toolbarAppLock;
    private RadioGroup lockTypeGroup;
    private RadioButton radioNone, radioPassword;
    private LinearLayout passwordSection;
    private EditText editNewPassword;
    private MaterialSwitch switchFingerprint;
    private MaterialButton saveLockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        prefs = getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);

        // findViewById দিয়ে সব ভিউ ধরা হয়েছে
        toolbarAppLock = findViewById(R.id.toolbarAppLock);
        lockTypeGroup = findViewById(R.id.lockTypeGroup);
        radioNone = findViewById(R.id.radioNone);
        radioPassword = findViewById(R.id.radioPassword);
        passwordSection = findViewById(R.id.passwordSection);
        editNewPassword = findViewById(R.id.editNewPassword);
        switchFingerprint = findViewById(R.id.switchFingerprint);
        saveLockButton = findViewById(R.id.saveLockButton);

        toolbarAppLock.setNavigationOnClickListener(v -> finish());

        int currentType = prefs.getInt("LOCK_TYPE", 1);
        String savedPass = prefs.getString("APP_PASSWORD", "1234");
        boolean useBio = prefs.getBoolean("USE_BIOMETRIC", true);

        if (currentType == 0) {
            radioNone.setChecked(true);
            passwordSection.setVisibility(View.GONE);
        } else {
            radioPassword.setChecked(true);
            passwordSection.setVisibility(View.VISIBLE);
        }

        editNewPassword.setText(savedPass);
        switchFingerprint.setChecked(useBio);

        lockTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioNone) {
                passwordSection.setVisibility(View.GONE);
            } else {
                passwordSection.setVisibility(View.VISIBLE);
            }
        });

        saveLockButton.setOnClickListener(v -> {
            int selectedType = radioNone.isChecked() ? 0 : 1;
            String pass = editNewPassword.getText().toString().trim();

            if (selectedType == 1 && pass.isEmpty()) {
                Toast.makeText(this, "পাসওয়ার্ড খালি রাখা যাবে না!", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putInt("LOCK_TYPE", selectedType)
                    .putString("APP_PASSWORD", pass)
                    .putBoolean("USE_BIOMETRIC", switchFingerprint.isChecked())
                    .apply();

            Toast.makeText(this, "লক সেটিংস সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show();
            finish();
        });

        applyThemeColor();
    }

    private void applyThemeColor() {
        String hex = prefs.getString("APP_COLOR", "#0D9488");
        int color = Color.parseColor(hex);

        getWindow().setStatusBarColor(color);
        toolbarAppLock.setTitleTextColor(color);
        toolbarAppLock.setNavigationIconTint(color);

        ColorStateList stateList = ColorStateList.valueOf(color);
        saveLockButton.setBackgroundTintList(stateList);
        radioNone.setButtonTintList(stateList);
        radioPassword.setButtonTintList(stateList);
        switchFingerprint.setThumbTintList(stateList);
    }
}