package com.myname.prostream;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class AppLockActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private boolean resetMode;

    private MaterialToolbar toolbarAppLock;
    private RadioGroup lockTypeGroup;
    private RadioButton radioNone, radioPassword;
    private LinearLayout passwordSection;
    private EditText editNewPassword;
    private EditText editCurrentPassword, editConfirmPassword;
    private TextView tvPinError;
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

        // A2: Forgot PIN → বায়োমেট্রিক দিয়ে এসেছে, তাই বর্তমান PIN লাগবে না
        resetMode = getIntent().getBooleanExtra("RESET_PIN", false);
        addPinFields();

        int currentType = prefs.getInt("LOCK_TYPE", 1);
        // A4: ফিঙ্গারপ্রিন্ট ডিফল্ট বন্ধ — নতুন ইউজার নিজে চালু করবে
        boolean useBio = prefs.getBoolean("USE_BIOMETRIC", false);

        if (currentType == 0) {
            radioNone.setChecked(true);
            passwordSection.setVisibility(View.GONE);
        } else {
            radioPassword.setChecked(true);
            passwordSection.setVisibility(View.VISIBLE);
        }

        // PIN কখনো প্লেইনটেক্সটে দেখানো হয় না — নতুন PIN লিখে সেভ করলে হ্যাশ হিসেবে সংরক্ষিত হয়
        switchFingerprint.setChecked(useBio);
        updateFingerprintSwitch(currentType);

        lockTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioNone) {
                passwordSection.setVisibility(View.GONE);
                updateFingerprintSwitch(0);
            } else {
                passwordSection.setVisibility(View.VISIBLE);
                updateFingerprintSwitch(1);
            }
        });

        saveLockButton.setOnClickListener(v -> {
            int selectedType = radioNone.isChecked() ? 0 : 1;

            if (selectedType == 0) {
                // A6: লক বন্ধ করার আলাদা বার্তা
                prefs.edit()
                        .putInt("LOCK_TYPE", 0)
                        .putBoolean("USE_BIOMETRIC", false)
                        .apply();
                Toast.makeText(this, "অ্যাপ লক বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String pass = editNewPassword.getText().toString().trim();
            if (pass.isEmpty()) {
                showPinError("পাসওয়ার্ড খালি রাখা যাবে না!");
                return;
            }
            // A5: কমপক্ষে ৪ সংখ্যার PIN
            if (pass.length() < 4) {
                showPinError("PIN কমপক্ষে ৪ সংখ্যার হতে হবে");
                return;
            }
            // A1: দুটো ফিল্ড একই হতে হবে
            String confirm = editConfirmPassword.getText().toString().trim();
            if (!pass.equals(confirm)) {
                showPinError("দুটো PIN একই হয়নি — আবার লিখুন");
                return;
            }
            // A2: PIN আগে থেকে থাকলে বর্তমান PIN যাচাই করতে হবে
            if (!resetMode && AppUtil.hasPin(prefs)) {
                String current = editCurrentPassword.getText().toString().trim();
                if (current.isEmpty() || !AppUtil.verifyPin(prefs, current)) {
                    showPinError("বর্তমান PIN সঠিক নয়");
                    return;
                }
            }

            AppUtil.savePin(prefs, pass);
            prefs.edit()
                    .putInt("LOCK_TYPE", 1)
                    .putBoolean("USE_BIOMETRIC", switchFingerprint.isChecked())
                    .apply();

            Toast.makeText(this, "PIN সেট হয়েছে", Toast.LENGTH_SHORT).show();
            finish();
        });

        applyThemeColor();
    }

    // A1/A2: Confirm PIN + বর্তমান PIN ফিল্ড (কার্ডের নিচে, কোড থেকে যোগ করা হয়)
    private void addPinFields() {
        int numericPassword = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        editNewPassword.setInputType(numericPassword);

        editCurrentPassword = new EditText(this);
        editCurrentPassword.setHint("বর্তমান PIN");
        editCurrentPassword.setInputType(numericPassword);
        editCurrentPassword.setTextColor(Color.WHITE);
        editCurrentPassword.setTextSize(16);
        editCurrentPassword.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        // resetMode-এ (Forgot PIN → বায়োমেট্রিক) বর্তমান PIN লাগে না
        editCurrentPassword.setVisibility(AppUtil.hasPin(prefs) && !resetMode ? View.VISIBLE : View.GONE);
        passwordSection.addView(editCurrentPassword);

        editConfirmPassword = new EditText(this);
        editConfirmPassword.setHint("PIN নিশ্চিত করুন");
        editConfirmPassword.setInputType(numericPassword);
        editConfirmPassword.setTextColor(Color.WHITE);
        editConfirmPassword.setTextSize(16);
        editConfirmPassword.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        passwordSection.addView(editConfirmPassword);

        tvPinError = new TextView(this);
        tvPinError.setTextColor(Color.parseColor("#FF6B6B"));
        tvPinError.setTextSize(13);
        tvPinError.setPadding(dpToPx(16), dpToPx(6), dpToPx(16), 0);
        tvPinError.setVisibility(View.GONE);
        passwordSection.addView(tvPinError);

        // নতুন কিছু লিখলেই পুরনো এরর মুছে যায়
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvPinError != null) tvPinError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        editNewPassword.addTextChangedListener(clearErrorWatcher);
        editCurrentPassword.addTextChangedListener(clearErrorWatcher);
        editConfirmPassword.addTextChangedListener(clearErrorWatcher);
    }

    // A3: লক None হলে ফিঙ্গারপ্রিন্ট সুইচ নিষ্ক্রিয় (ধূসর) হয়ে যায়
    private void updateFingerprintSwitch(int lockType) {
        boolean enabled = lockType != 0;
        switchFingerprint.setEnabled(enabled);
        switchFingerprint.setAlpha(enabled ? 1f : 0.4f);
        if (!enabled) switchFingerprint.setChecked(false);
    }

    private void showPinError(String message) {
        if (tvPinError != null) {
            tvPinError.setText(message);
            tvPinError.setVisibility(View.VISIBLE);
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void applyThemeColor() {
        String hex = prefs.getString("APP_COLOR", "#0D9488");
        int color = AppUtil.parseColorSafe(hex, Color.parseColor("#0D9488"));

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