package com.myname.prostream;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPrefs;
    private View lockRootLayout, orb1, orb2, headerLayout, loginCard, fingerprintLayout;
    private MaterialCardView logoCard, devBadgeCard, fingerprintCard;
    private ImageView appLogo, fingerprintIcon;
    private TextView tvTypoAppName, tvTypoDeveloper, tvLockSubtitle;
    private EditText passwordInput;
    private MaterialButton loginButton;
    private ObjectAnimator logoPulseAnimator;
    private final Handler typoHandler = new Handler(Looper.getMainLooper());
    // Forgot-PIN ফ্লোতে বায়োমেট্রিক ব্যবহার হচ্ছে কি না (সাধারণ আনলক থেকে আলাদা)
    private boolean biometricResetMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
        } catch (Exception ignored) {}

        sharedPrefs = getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);
        final int lockType = sharedPrefs.getInt("LOCK_TYPE", 1);

        // যদি লক বন্ধ থাকে এবং HomeActivity ইতিমধ্যে সচল থাকে, তবে রিলোড ছাড়াই সরাসরি আগের পেজে নিয়ে যাবে
        // PIN কখনো সেট করা না হয়ে থাকলে (নতুন ইনস্টল) লক স্ক্রিন দেখিয়ে আটকে রাখা যাবে না
        if ((lockType == 0 || !AppUtil.hasPin(sharedPrefs)) && HomeActivity.isSessionAlive()) {
            goToHome();
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        lockRootLayout = findViewById(R.id.lockRootLayout);
        orb1 = findViewById(R.id.orb1);
        orb2 = findViewById(R.id.orb2);
        headerLayout = findViewById(R.id.headerLayout);
        loginCard = findViewById(R.id.loginCard);
        fingerprintLayout = findViewById(R.id.fingerprintLayout);
        logoCard = findViewById(R.id.logoCard);
        devBadgeCard = findViewById(R.id.devBadgeCard);
        fingerprintCard = findViewById(R.id.fingerprintCard);
        appLogo = findViewById(R.id.appLogo);
        fingerprintIcon = findViewById(R.id.fingerprintIcon);
        tvTypoAppName = findViewById(R.id.tvTypoAppName);
        tvTypoDeveloper = findViewById(R.id.tvTypoDeveloper);
        tvLockSubtitle = findViewById(R.id.tvLockSubtitle);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);

        setupPinField();
        addForgotPinView();

        if (lockRootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(lockRootLayout, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        if (loginCard != null) loginCard.setVisibility(View.GONE);
        if (fingerprintLayout != null) fingerprintLayout.setVisibility(View.GONE);
        if (tvLockSubtitle != null) tvLockSubtitle.setVisibility(View.GONE);

        applyDynamicTheme();
        startLiveBackgroundAnimation();
        startTypoBrandAnimation();

        // M1: লক না থাকলে ১২৫০ms স্প্ল্যাশ অপেক্ষা নেই — সরাসরি হোমে যাওয়া হয়।
        // লক থাকলে শুধু ~৩৫০ms রিভিল অপেক্ষা। প্রথমবার PIN সেট না থাকলে
        // একবার PIN সেট করার প্রস্তাব দেখানো হয় (M8)।
        final boolean needsLock = lockType != 0 && AppUtil.hasPin(sharedPrefs);
        if (!needsLock && !AppUtil.hasPin(sharedPrefs) && lockType != 0
                && !sharedPrefs.getBoolean("PIN_OFFER_SHOWN", false)) {
            typoHandler.postDelayed(this::showPinSetupOfferDialog, 350);
        } else if (!needsLock) {
            goToHome();
        } else {
            typoHandler.postDelayed(this::revealLockScreenSmoothly, 350);
        }

        if (loginButton != null) {
            loginButton.setOnClickListener(v -> attemptLogin());
        }

        if (fingerprintCard != null) {
            fingerprintCard.setOnClickListener(v -> showNativeBiometricPrompt());
        }
        if (fingerprintIcon != null) {
            fingerprintIcon.setOnClickListener(v -> showNativeBiometricPrompt());
        }
    }

    private void revealLockScreenSmoothly() {
        if (loginCard != null) {
            loginCard.setVisibility(View.VISIBLE);
            loginCard.setAlpha(0f);
            loginCard.setScaleX(0.90f);
            loginCard.setScaleY(0.90f);
            loginCard.setTranslationY(40f);
            loginCard.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(360)
                    .setInterpolator(new OvershootInterpolator(1.05f))
                    .start();
        }

        // M6: লক সাবটাইটেল দেখানো হয়
        if (tvLockSubtitle != null) {
            tvLockSubtitle.setText("আপনার PIN লিখুন");
            tvLockSubtitle.setVisibility(View.VISIBLE);
        }

        // M2: বায়োমেট্রিক প্রম্পট নিজে থেকে খোলা হয় না — ব্যবহারকারী
        // ফিঙ্গারপ্রিন্ট বোতাম চাপলেই খুলবে। M3: SDK < 28-এ বোতাম লুকানো থাকে।
        // A4: USE_BIOMETRIC ডিফল্ট false (নতুন ইনস্টলে অপ্ট-ইন)।
        boolean useBiometric = sharedPrefs.getBoolean("USE_BIOMETRIC", false);
        if (fingerprintLayout != null) {
            if (useBiometric && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                fingerprintLayout.setVisibility(View.VISIBLE);
                fingerprintLayout.setAlpha(0f);
                fingerprintLayout.animate().alpha(1f).setDuration(300).start();
            } else {
                fingerprintLayout.setVisibility(View.GONE);
            }
        }
    }

    private void startTypoBrandAnimation() {
        if (logoCard != null) {
            logoCard.setScaleX(0.4f);
            logoCard.setScaleY(0.4f);
            logoCard.setAlpha(0f);
            logoCard.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(new OvershootInterpolator(1.25f))
                    .start();

            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(
                    logoCard,
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.04f, 1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.04f, 1f)
            );
            pulse.setDuration(2200);
            pulse.setStartDelay(550);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();
            logoPulseAnimator = pulse;
        }

        final String brandTitle = "ProStream";
        final String devTitle = "Yasin Adnan";

        if (tvTypoAppName != null) {
            tvTypoAppName.setText("");
            for (int i = 0; i <= brandTitle.length(); i++) {
                final int idx = i;
                typoHandler.postDelayed(() -> {
                    if (tvTypoAppName != null) {
                        String sub = brandTitle.substring(0, idx);
                        tvTypoAppName.setText(idx < brandTitle.length() ? sub + "▌" : sub);
                    }
                }, 50L * i);
            }
        }

        if (devBadgeCard != null) {
            devBadgeCard.setAlpha(0f);
            devBadgeCard.setScaleX(0.88f);
            devBadgeCard.setScaleY(0.88f);
            devBadgeCard.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(220)
                    .setDuration(380)
                    .setInterpolator(new OvershootInterpolator(1.15f))
                    .start();
        }

        if (tvTypoDeveloper != null) {
            tvTypoDeveloper.setText("");
            for (int i = 0; i <= devTitle.length(); i++) {
                final int idx = i;
                typoHandler.postDelayed(() -> {
                    if (tvTypoDeveloper != null) {
                        tvTypoDeveloper.setText(devTitle.substring(0, idx));
                    }
                }, 220L + (32L * i));
            }
        }
    }

    private void applyDynamicTheme() {
        int color = parseColorSafe(sharedPrefs.getString("APP_COLOR", "#00A884"));
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#06080F"));
        window.setNavigationBarColor(Color.parseColor("#06080F"));

        if (loginButton != null) {
            loginButton.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        if (fingerprintIcon != null) {
            fingerprintIcon.setColorFilter(color);
        }
        if (logoCard != null) {
            logoCard.setStrokeColor(color);
        }
        if (devBadgeCard != null) {
            devBadgeCard.setStrokeColor(color);
        }
    }

    private void startLiveBackgroundAnimation() {
        int color = parseColorSafe(sharedPrefs.getString("APP_COLOR", "#00A884"));

        GradientDrawable glow1 = new GradientDrawable();
        glow1.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow1.setColors(new int[]{color, Color.TRANSPARENT});
        glow1.setGradientRadius(420f);

        GradientDrawable glow2 = new GradientDrawable();
        glow2.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow2.setColors(new int[]{Color.parseColor("#7C3AED"), Color.TRANSPARENT});
        glow2.setGradientRadius(450f);

        if (orb1 != null) {
            orb1.setBackground(glow1);
            ObjectAnimator animX1 = ObjectAnimator.ofFloat(orb1, "translationX", 0f, 200f);
            animX1.setDuration(9000);
            animX1.setRepeatCount(ValueAnimator.INFINITE);
            animX1.setRepeatMode(ValueAnimator.REVERSE);
            animX1.setInterpolator(new AccelerateDecelerateInterpolator());
            animX1.start();

            ObjectAnimator animY1 = ObjectAnimator.ofFloat(orb1, "translationY", 0f, 260f);
            animY1.setDuration(11000);
            animY1.setRepeatCount(ValueAnimator.INFINITE);
            animY1.setRepeatMode(ValueAnimator.REVERSE);
            animY1.setInterpolator(new AccelerateDecelerateInterpolator());
            animY1.start();
        }

        if (orb2 != null) {
            orb2.setBackground(glow2);
            ObjectAnimator animX2 = ObjectAnimator.ofFloat(orb2, "translationX", 0f, -200f);
            animX2.setDuration(10000);
            animX2.setRepeatCount(ValueAnimator.INFINITE);
            animX2.setRepeatMode(ValueAnimator.REVERSE);
            animX2.setInterpolator(new AccelerateDecelerateInterpolator());
            animX2.start();

            ObjectAnimator animY2 = ObjectAnimator.ofFloat(orb2, "translationY", 0f, -260f);
            animY2.setDuration(8500);
            animY2.setRepeatCount(ValueAnimator.INFINITE);
            animY2.setRepeatMode(ValueAnimator.REVERSE);
            animY2.setInterpolator(new AccelerateDecelerateInterpolator());
            animY2.start();
        }
    }

    private void showNativeBiometricPrompt() {
        showNativeBiometricPrompt(false);
    }

    private void showNativeBiometricPrompt(final boolean forReset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                biometricResetMode = forReset;
                BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                        .setTitle("ProStream")
                        .setSubtitle("Yasin Adnan • Security Unlock")
                        .setNegativeButton("Use PIN", getMainExecutor(), (dialog, which) -> {
                            biometricResetMode = false;
                        })
                        .build();

                prompt.authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        if (biometricResetMode) {
                            biometricResetMode = false;
                            Intent intent = new Intent(MainActivity.this, AppLockActivity.class);
                            intent.putExtra("RESET_PIN", true);
                            startActivity(intent);
                            finish();
                        } else {
                            goToHome();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(MainActivity.this, "বায়োমেট্রিক চেনা যায়নি", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // M3: সেন্সরে সমস্যা/এনরোল না থাকলে চুপচাপ কিছু না করে PIN-এর কথা জানানো হয়
                        Toast.makeText(MainActivity.this, "বায়োমেট্রিক কাজ করেনি — PIN ব্যবহার করুন।", Toast.LENGTH_SHORT).show();
                        if (biometricResetMode) {
                            biometricResetMode = false;
                            showClearDataDialog();
                        }
                    }
                });
            } catch (Exception ignored) {
                biometricResetMode = false;
                if (forReset) showClearDataDialog();
            }
        }
    }

    // M9: PIN ফিল্ডে সংখ্যাসূচক মাস্কড কিবোর্ড + Done চাপলে লগইন
    private void setupPinField() {
        if (passwordInput == null) return;
        passwordInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        passwordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        // নতুন কিছু লিখলেই আগের এরর মুছে যায় (M4)
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearPinError();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void clearPinError() {
        if (passwordInput != null && passwordInput.getParent() instanceof TextInputLayout) {
            ((TextInputLayout) passwordInput.getParent()).setError(null);
        }
    }

    private void showPinError(String message) {
        if (passwordInput != null && passwordInput.getParent() instanceof TextInputLayout) {
            ((TextInputLayout) passwordInput.getParent()).setError(message);
        }
    }

    // M5: লক কার্ডের নিচে "PIN ভুলে গেছেন?" লিংক
    private void addForgotPinView() {
        if (loginCard == null || !(loginCard instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup cardInner = (android.view.ViewGroup) ((android.view.ViewGroup) loginCard).getChildAt(0);
        if (cardInner == null) return;
        TextView forgot = new TextView(this);
        forgot.setText("PIN ভুলে গেছেন?");
        forgot.setTextColor(Color.parseColor("#94A3B8"));
        forgot.setTextSize(12.5f);
        forgot.setGravity(android.view.Gravity.CENTER);
        forgot.setPadding(0, dpToPx(12), 0, 0);
        forgot.setOnClickListener(v -> showForgotPinOptions());
        int loginIdx = loginButton != null ? cardInner.indexOfChild(loginButton) : -1;
        if (loginIdx >= 0) {
            cardInner.addView(forgot, loginIdx + 1);
        } else {
            cardInner.addView(forgot);
        }
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void attemptLogin() {
        String inputPassword = passwordInput != null ? passwordInput.getText().toString().trim() : "";
        if (AppUtil.isPinLockedOut(sharedPrefs)) {
            long secs = AppUtil.pinLockoutRemainingMs(sharedPrefs) / 1000 + 1;
            Toast.makeText(MainActivity.this, "অনেক ভুল চেষ্টা! " + secs + " সেকেন্ড পরে আবার চেষ্টা করুন।", Toast.LENGTH_SHORT).show();
            return;
        }
        if (AppUtil.verifyPin(sharedPrefs, inputPassword)) {
            clearPinError();
            goToHome();
        } else {
            if (loginCard != null) {
                loginCard.animate().translationX(20f).setDuration(55).withEndAction(() ->
                        loginCard.animate().translationX(-20f).setDuration(55).withEndAction(() ->
                                loginCard.animate().translationX(0f).setDuration(55).start()).start()).start();
            }
            // M4: ভুল PIN মুছে ফেলা হয় + কার্ডে স্থায়ী এরর দেখানো হয়; M7: বাংলায়
            if (passwordInput != null) passwordInput.setText("");
            int left = AppUtil.pinFailuresLeft(sharedPrefs);
            String msg = left > 0
                    ? "ভুল PIN! আর " + left + " বার চেষ্টা করা যাবে।"
                    : "ভুল PIN! কিছুক্ষণ পরে আবার চেষ্টা করুন।";
            showPinError(msg);
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    // M8: প্রথমবার PIN না থাকলে একবারই PIN সেট করার প্রস্তাব
    private void showPinSetupOfferDialog() {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setTitle("PIN লক সেট করবেন?")
                .setMessage("অ্যাপে PIN লক চালু করলে অন্য কেউ আপনার অ্যাপ খুলতে পারবে না। এখনই সেট করবেন?")
                .setPositiveButton("এখনই সেট করুন", (dialog, which) -> {
                    sharedPrefs.edit().putBoolean("PIN_OFFER_SHOWN", true).apply();
                    startActivity(new Intent(MainActivity.this, AppLockActivity.class));
                    finish();
                })
                .setNegativeButton("এখন নয়", (dialog, which) -> {
                    sharedPrefs.edit().putBoolean("PIN_OFFER_SHOWN", true).apply();
                    goToHome();
                })
                .setCancelable(false)
                .show();
    }

    // M5: PIN ভুলে গেলে — বায়োমেট্রিক থাকলে তা দিয়ে নতুন PIN, না থাকলে ডেটা মোছার পথ
    private void showForgotPinOptions() {
        if (biometricAvailableForReset()) {
            showNativeBiometricPrompt(true);
        } else {
            showClearDataDialog();
        }
    }

    private boolean biometricAvailableForReset() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false;
        if (!sharedPrefs.getBoolean("USE_BIOMETRIC", false)) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return isBiometricEnrolledApi29();
        }
        return true; // API 28: এনরোল যাচাই করা যায় না — প্রম্পট খুলে দেখা হবে, ব্যর্থ হলে এরর পথে যাবে
    }

    @SuppressWarnings("deprecation")
    private boolean isBiometricEnrolledApi29() {
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

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("PIN ভুলে গেছেন?")
                .setMessage("PIN ছাড়া অ্যাপ খোলা যাবে না। একমাত্র উপায় অ্যাপের ডেটা মোছা — তাতে PIN মুছে যাবে, তবে সব সাইট ও সেটিংসও মুছে যাবে।\n\nSettings → Apps → ProStream → Storage → Clear data থেকে মুছতে পারবেন।")
                .setPositiveButton("সেটিংস খুলুন", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private int parseColorSafe(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.parseColor("#00A884");
        }
    }

    private void goToHome() {
        typoHandler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (logoPulseAnimator != null) logoPulseAnimator.cancel();
        typoHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}