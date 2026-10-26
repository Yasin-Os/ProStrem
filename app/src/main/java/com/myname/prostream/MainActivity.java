package com.myname.prostream;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPrefs;
    private View lockRootLayout, orb1, orb2, headerLayout, loginCard, fingerprintLayout;
    private MaterialCardView logoCard, devBadgeCard, fingerprintCard;
    private ImageView appLogo, fingerprintIcon;
    private TextView tvTypoAppName, tvTypoDeveloper, tvLockSubtitle;
    private EditText passwordInput;
    private MaterialButton loginButton;
    private final Handler typoHandler = new Handler(Looper.getMainLooper());

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
        if (lockType == 0 && HomeActivity.isSessionAlive()) {
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

        typoHandler.postDelayed(() -> {
            if (isFinishing()) return;
            if (lockType == 0) {
                goToHome();
            } else {
                revealLockScreenSmoothly();
            }
        }, 1250);

        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                String inputPassword = passwordInput != null ? passwordInput.getText().toString().trim() : "";
                String savedPassword = sharedPrefs.getString("APP_PASSWORD", "1234");
                if (inputPassword.equals(savedPassword)) {
                    goToHome();
                } else {
                    if (loginCard != null) {
                        loginCard.animate().translationX(20f).setDuration(55).withEndAction(() ->
                                loginCard.animate().translationX(-20f).setDuration(55).withEndAction(() ->
                                        loginCard.animate().translationX(0f).setDuration(55).start()).start()).start();
                    }
                    Toast.makeText(MainActivity.this, "Incorrect PIN / Password!", Toast.LENGTH_SHORT).show();
                }
            });
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

        boolean useBiometric = sharedPrefs.getBoolean("USE_BIOMETRIC", true);
        if (useBiometric) {
            if (fingerprintLayout != null) {
                fingerprintLayout.setVisibility(View.VISIBLE);
                fingerprintLayout.setAlpha(0f);
                fingerprintLayout.animate().alpha(1f).setDuration(300).start();
            }
            typoHandler.postDelayed(this::showNativeBiometricPrompt, 220);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                        .setTitle("ProStream")
                        .setSubtitle("Yasin Adnan • Security Unlock")
                        .setNegativeButton("Use PIN", getMainExecutor(), (dialog, which) -> {})
                        .build();

                prompt.authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        goToHome();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(MainActivity.this, "Biometric not recognized", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception ignored) {}
        }
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
        typoHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}