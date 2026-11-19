package com.myname.prostream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.biometrics.BiometricManager;
import android.os.Build;
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
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class AppLockActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private boolean resetMode;
    // ঢোকার সময় PIN ছিল কি না — PIN না বসিয়ে ফিরে গেলে ভূতুড়ে লক স্টেট
    // (LOCK_TYPE 1, কিন্তু PIN নেই) যেন না থাকে
    private boolean hadPinAtEntry;

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
        hadPinAtEntry = AppUtil.hasPin(this);

        // findViewById দিয়ে সব ভিউ ধরা হয়েছে
        toolbarAppLock = findViewById(R.id.toolbarAppLock);
        lockTypeGroup = findViewById(R.id.lockTypeGroup);
        radioNone = findViewById(R.id.radioNone);
        radioPassword = findViewById(R.id.radioPassword);
        passwordSection = findViewById(R.id.passwordSection);
        editNewPassword = findViewById(R.id.editNewPassword);
        switchFingerprint = findViewById(R.id.switchFingerprint);
        saveLockButton = findViewById(R.id.saveLockButton);

        toolbarAppLock.setNavigationOnClickListener(v -> finishWithPinCleanup());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithPinCleanup();
            }
        });

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
            if (!resetMode && AppUtil.hasPin(this)) {
                String current = editCurrentPassword.getText().toString().trim();
                if (current.isEmpty() || !AppUtil.verifyPin(this, current)) {
                    showPinError("বর্তমান PIN সঠিক নয়");
                    return;
                }
            }

            AppUtil.savePin(this, pass);
            prefs.edit()
                    .putInt("LOCK_TYPE", 1)
                    .putBoolean("USE_BIOMETRIC", switchFingerprint.isChecked())
                    .apply();

            Toast.makeText(this, "PIN সেট হয়েছে", Toast.LENGTH_SHORT).show();
            finish();
        });

        applyThemeColor();
    }

    // PIN না বসিয়ে ফিরে গেলে ভূতুড়ে লক স্টেট থাকবে না — PIN আগে থেকে না
    // থাকলে LOCK_TYPE 0 করে দেওয়া হয়
    private void finishWithPinCleanup() {
        if (!hadPinAtEntry && !AppUtil.hasPin(this)) {
            prefs.edit().putInt("LOCK_TYPE", 0).putBoolean("USE_BIOMETRIC", false).apply();
        }
        finish();
    }

    // A1/A2: Confirm PIN + বর্তমান PIN ফিল্ড — কার্ডের ভেতরে সঠিক ক্রমে
    // (বর্তমান → নতুন → নিশ্চিত করুন), New PIN ফিল্ডের মতো একই স্টাইলে
    private void addPinFields() {
        int numericPassword = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        editNewPassword.setInputType(numericPassword);

        LinearLayout cardInner = (LinearLayout) editNewPassword.getParent();

        editCurrentPassword = new EditText(this);
        editCurrentPassword.setHint("বর্তমান PIN");
        editCurrentPassword.setInputType(numericPassword);
        editCurrentPassword.setTextColor(Color.WHITE);
        editCurrentPassword.setHintTextColor(Color.parseColor("#666666"));
        editCurrentPassword.setTextSize(16);
        editCurrentPassword.setBackground(null);
        editCurrentPassword.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        // resetMode-এ (Forgot PIN → বায়োমেট্রিক) বর্তমান PIN লাগে না
        editCurrentPassword.setVisibility(AppUtil.hasPin(this) && !resetMode ? View.VISIBLE : View.GONE);
        cardInner.addView(editCurrentPassword, cardInner.indexOfChild(editNewPassword));

        editConfirmPassword = new EditText(this);
        editConfirmPassword.setHint("PIN নিশ্চিত করুন");
        editConfirmPassword.setInputType(numericPassword);
        editConfirmPassword.setTextColor(Color.WHITE);
        editConfirmPassword.setHintTextColor(Color.parseColor("#666666"));
        editConfirmPassword.setTextSize(16);
        editConfirmPassword.setBackground(null);
        editConfirmPassword.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        int confirmIdx = cardInner.indexOfChild(editNewPassword) + 2; // New PIN + তার ডিভাইডারের পরে
        cardInner.addView(editConfirmPassword, confirmIdx);

        View confirmDivider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        divParams.setMarginStart(dpToPx(20));
        confirmDivider.setLayoutParams(divParams);
        confirmDivider.setBackgroundColor(Color.parseColor("#222222"));
        cardInner.addView(confirmDivider, confirmIdx + 1);

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

    // A3: লক None হলে ফিঙ্গারপ্রিন্ট সুইচ নিষ্ক্রিয় (ধূসর) হয়ে যায়;
    // বায়োমেট্রিক এনরোল করা না থাকলেও সুইচ চালু করা যায় না
    private void updateFingerprintSwitch(int lockType) {
        boolean enabled = lockType != 0 && isBiometricEnrolled();
        switchFingerprint.setEnabled(enabled);
        switchFingerprint.setAlpha(enabled ? 1f : 0.4f);
        if (!enabled) switchFingerprint.setChecked(false);
    }

    @SuppressWarnings("deprecation")
    private boolean isBiometricEnrolled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false;
        try {
            BiometricManager bm = (BiometricManager) getSystemService(Context.BIOMETRIC_SERVICE);
            if (bm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        == BiometricManager.BIOMETRIC_SUCCESS;
            }
            return bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception ignored) {
            return false;
        }
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