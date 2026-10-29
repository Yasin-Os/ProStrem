package com.myname.prostream;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class HomeActivity extends AppCompatActivity {

    private static final String TELEGRAM_BOT_URL = "https://t.me/contactyasinbot";
    private static final String DEFAULT_PRMOVIES_URL = "https://prmovies.technology/";
    private static final String DL_CHANNEL_PROGRESS = "prostream_media_dl_progress";
    private static final String DL_CHANNEL_DONE = "prostream_media_dl_done";
    private static final String ACTION_CANCEL_DL = "com.myname.prostream.ACTION_CANCEL_DL";
    private static final String PREF_DL_IDS_KEY = "PROSTREAM_ONLY_DL_IDS_V3";
    private static final String PREF_DL_HISTORY_KEY = "PROSTREAM_ONLY_HISTORY_V3";
    private static final int FILE_CHOOSER_REQUEST_CODE = 802;

    private static final Handler bgDownloadHandler = new Handler(Looper.getMainLooper());
    private static final Set<String> trackedDownloadIds = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Long, Long> lastBytesMap = new ConcurrentHashMap<>();
    private static final Map<Long, Long> lastTimeMap = new ConcurrentHashMap<>();
    private static final Map<Long, Long> currentSpeedMap = new ConcurrentHashMap<>();
    private static final Map<Long, CustomDownloadTask> customTasksMap = new ConcurrentHashMap<>();
    private static final ExecutorService customDlExecutor = Executors.newFixedThreadPool(4);
    private static final ExecutorService parallelChunkPool = Executors.newFixedThreadPool(8);

    private static final LinkedHashMap<String, String> liveDetectedStreamsMap = new LinkedHashMap<String, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 12;
        }
    };

    private static Context appContextRef;
    private static WifiManager.WifiLock turboWifiLock;
    private static PowerManager.WakeLock turboWakeLock;
    private static boolean isNotificationTrackerRunning = false;
    private static boolean isReceiverRegisteredOnApp = false;
    private static boolean sessionAlive = false;
    private static boolean sessionUnlocked = false;

    public static boolean isSessionAlive() {
        return sessionAlive;
    }

    public static boolean isSessionUnlocked() {
        return sessionUnlocked;
    }

    public static void setSessionUnlocked(boolean unlocked) {
        sessionUnlocked = unlocked;
    }

    public static void forceEnableAnimations() {
        try {
            Method setDurationScale = ValueAnimator.class.getDeclaredMethod("setDurationScale", float.class);
            setDurationScale.setAccessible(true);
            setDurationScale.invoke(null, 1.0f);
        } catch (Throwable ignored) {}
    }

    private static final String[] BLOCKED_AD_DOMAINS = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "popads.net", "popcash.net", "onclickalgo.com", "onclickperformance.com",
            "adsterra.com", "exoclick.com", "realsrv.com", "tsyndicate.com",
            "juicyads.com", "propellerads.com", "hilltopads.net", "clickadu.com",
            "trafficjunky.net", "acscdn.com", "histats.com", "magsrv.com",
            "twinrds.com", "bidgear.com", "galaksion.com", "yllix.com",
            "adcash.com", "evadav.com", "push.house", "richads.com",
            "bet365.com", "1xbet.com", "melbet.com", "mostbet.com"
    };

    private static boolean isBlockedAdUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("cloudflare") || lower.contains("turnstile") || lower.contains("cdn-cgi")) {
            return false;
        }
        for (String domain : BLOCKED_AD_DOMAINS) {
            if (lower.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    private View rootLayout;
    private LinearProgressIndicator progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private FrameLayout webViewHolder;
    private WebView webView;
    private MaterialCardView btnFloatingHub;
    private ImageView ivHubIcon;
    private TextView tvHubBadge;

    private LinearLayout modernOfflineOverlay;
    private TextView tvOfflineUrlInfo;

    private SharedPreferences sharedPrefs;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private ValueCallback<Uri[]> filePathCallback;
    private final ArrayList<SiteItem> siteList = new ArrayList<>();
    private final Stack<SiteItem> siteHistoryStack = new Stack<>();

    private final Map<String, WebView> webViewPool = new HashMap<>();
    private final Map<WebView, String> lastFailedUrlMap = new HashMap<>();
    private final Set<WebView> clearHistoryOnLoadSet = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> fetchingOgUrls = Collections.synchronizedSet(new HashSet<>());

    private SiteItem currentSite;
    private final int appColor = Color.parseColor("#00A884");
    private boolean darkModeEnabled = false;
    private boolean batterySaverEnabled = true;
    private boolean launchingInternalActivity = false;
    private boolean leftAppToBackground = false;
    private File pendingUpdateApkFile = null;
    private static boolean hasAutoCheckedUpdateThisSession = false;
    private Typeface modernFont;
    private String browserUserAgent;

    private static final ExecutorService bgExecutor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable mediaLiveRefreshRunnable;
    private final ArrayList<ValueAnimator> activePopupAnimators = new ArrayList<>();
    private final ArrayList<ValueAnimator> hubIdleAnimators = new ArrayList<>();
    private LruCache<String, Bitmap> ogMemoryCache;

    static class CustomDownloadTask {
        long id;
        String title;
        String url;
        File outputFile;
        String publicUriString = "";
        final AtomicLong downloadedBytes = new AtomicLong(0);
        volatile long totalBytes = -1;
        volatile long speedBps = 0;
        volatile int status = DownloadManager.STATUS_RUNNING;
        volatile boolean cancelled = false;
        boolean isHls = false;
        volatile int totalSegments = 0;
        volatile int completedSegments = 0;

        CustomDownloadTask(long id, String title, String url, File outputFile, boolean isHls) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.outputFile = outputFile;
            this.isHls = isHls;
        }

        int getPercent() {
            if (status == DownloadManager.STATUS_SUCCESSFUL) return 100;
            if (isHls && totalSegments > 0) {
                return Math.min(99, (completedSegments * 100) / totalSegments);
            }
            long dl = downloadedBytes.get();
            if (totalBytes > 0) {
                return (int) Math.min(99L, (dl * 100L) / totalBytes);
            }
            return 0;
        }
    }

    private static final BroadcastReceiver globalDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null || intent.getAction() == null) return;
            appContextRef = context.getApplicationContext();
            String action = intent.getAction();

            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (trackedDownloadIds.contains(String.valueOf(id))) {
                    updateSingleCompletedNotificationStatic(appContextRef, id);
                    Toast.makeText(appContextRef, "Download complete", Toast.LENGTH_SHORT).show();
                }
            } else if (ACTION_CANCEL_DL.equals(action)) {
                long cancelId = intent.getLongExtra("DL_ID", -1L);
                if (cancelId != -1L) {
                    cancelOrRemoveDownloadStatic(appContextRef, cancelId);
                }
            }
        }
    };

    private static void cancelOrRemoveDownloadStatic(Context ctx, long cancelId) {
        if (ctx == null) return;
        try {
            CustomDownloadTask customTask = customTasksMap.get(cancelId);
            if (customTask != null) {
                customTask.cancelled = true;
                customTask.status = DownloadManager.STATUS_FAILED;
                customTasksMap.remove(cancelId);
                if (customTask.outputFile != null && customTask.outputFile.exists()) {
                    customTask.outputFile.delete();
                }
            } else {
                DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) dm.remove(cancelId);
            }
            trackedDownloadIds.remove(String.valueOf(cancelId));
            SharedPreferences prefs = ctx.getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);
            prefs.edit().putStringSet(PREF_DL_IDS_KEY, new HashSet<>(trackedDownloadIds)).apply();
            removeHistoryRecordStatic(ctx, cancelId);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel((int) (Math.abs(cancelId) % 100000L) + 4000);
            }
        } catch (Exception ignored) {}
    }

    private static void saveCompletedHistoryRecordStatic(Context ctx, long id, String title, long sizeBytes, String filePath, String publicUri) {
        if (ctx == null) return;
        try {
            SharedPreferences prefs = ctx.getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);
            String raw = prefs.getString(PREF_DL_HISTORY_KEY, "[]");
            JSONArray arr = new JSONArray(raw);
            JSONArray updated = new JSONArray();

            JSONObject entry = new JSONObject();
            entry.put("id", id);
            entry.put("title", title);
            entry.put("size", sizeBytes);
            entry.put("path", filePath != null ? filePath : "");
            entry.put("uri", publicUri != null ? publicUri : "");
            updated.put(entry);

            for (int i = 0; i < arr.length() && updated.length() < 30; i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null && o.optLong("id") != id) {
                    updated.put(o);
                }
            }
            prefs.edit().putString(PREF_DL_HISTORY_KEY, updated.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static void removeHistoryRecordStatic(Context ctx, long id) {
        if (ctx == null) return;
        try {
            SharedPreferences prefs = ctx.getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);
            String raw = prefs.getString(PREF_DL_HISTORY_KEY, "[]");
            JSONArray arr = new JSONArray(raw);
            JSONArray updated = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null && o.optLong("id") != id) {
                    updated.put(o);
                }
            }
            prefs.edit().putString(PREF_DL_HISTORY_KEY, updated.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static final Runnable staticLiveDownloadPoller = new Runnable() {
        @Override
        public void run() {
            if (appContextRef == null) {
                isNotificationTrackerRunning = false;
                return;
            }
            boolean anyRunning = updateLiveDownloadNotificationsAndSpeedStatic(appContextRef);
            if (anyRunning) {
                acquireTurboDownloadBoosterStatic(appContextRef);
                bgDownloadHandler.postDelayed(this, 1000);
            } else {
                isNotificationTrackerRunning = false;
                releaseTurboDownloadBoosterStatic();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        forceEnableAnimations();
        sessionAlive = true;
        appContextRef = getApplicationContext();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_home);

        rootLayout = findViewById(R.id.rootLayout);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        webView = findViewById(R.id.webView);
        btnFloatingHub = findViewById(R.id.btnFloatingHub);
        ivHubIcon = findViewById(R.id.ivHubIcon);
        tvHubBadge = findViewById(R.id.tvHubBadge);

        ogMemoryCache = new LruCache<>(36);
        browserUserAgent = buildAuthenticBrowserUserAgent(this);

        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        sharedPrefs = getSharedPreferences("ProStreamPrefs", MODE_PRIVATE);
        darkModeEnabled = sharedPrefs.getBoolean("DARK_MODE", false);
        batterySaverEnabled = sharedPrefs.getBoolean("BATTERY_SAVER", true);

        trackedDownloadIds.clear();
        Set<String> savedIds = sharedPrefs.getStringSet(PREF_DL_IDS_KEY, null);
        if (savedIds != null) {
            trackedDownloadIds.addAll(savedIds);
        }
        modernFont = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (!sharedPrefs.getBoolean("CF_NATIVE_COOKIE_CLEAN_V6", false)) {
            sharedPrefs.edit()
                    .remove("COOKIE_VAULT_V4")
                    .putBoolean("CF_NATIVE_COOKIE_CLEAN_V6", true)
                    .apply();
            try {
                cookieManager.removeAllCookies(value -> {
                    try {
                        CookieManager.getInstance().flush();
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }

        setupNotificationChannelsStatic(appContextRef);
        registerGlobalReceiverIfNeeded(appContextRef);
        requestAllMobileAndChromePermissions(false);
        setupMultiWebViewAndOfflineContainer();

        if (swipeRefresh != null) {
            swipeRefresh.setEnabled(true);
            swipeRefresh.setColorSchemeColors(
                    Color.parseColor("#00F5A0"),
                    Color.parseColor("#00E5FF"),
                    Color.parseColor("#7C3AED"));
            swipeRefresh.setProgressBackgroundColorSchemeColor(Color.parseColor("#141722"));
            swipeRefresh.setProgressViewOffset(false, dpToPx(12), dpToPx(64));
            swipeRefresh.setDistanceToTriggerSync(dpToPx(60));
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
                if (modernOfflineOverlay != null && modernOfflineOverlay.getVisibility() == View.VISIBLE) {
                    return false;
                }
                if (webView == null) return false;
                return webView.getScrollY() > dpToPx(6);
            });
            swipeRefresh.setOnRefreshListener(this::reloadActivePageCleanly);
        }

        loadCustomLinksAndPreloadAll();
        applyDynamicTheme();
        setupDraggableFloatingHub();
        startFloatingHubBreathingAnimation();
        startNotificationProgressTrackerStatic(appContextRef);

        if (getIntent() != null && getIntent().getBooleanExtra("OPEN_MEDIA_DIALOG", false)) {
            mainHandler.postDelayed(this::showMediaDownloadsDialog, 350);
        }

        if (!hasAutoCheckedUpdateThisSession) {
            hasAutoCheckedUpdateThisSession = true;
            mainHandler.postDelayed(() -> checkForAppUpdateInBackground(false), 3500);
        }
    }

    private static String buildAuthenticBrowserUserAgent(Context ctx) {
        try {
            String raw = WebSettings.getDefaultUserAgent(ctx);
            if (raw != null && !raw.isEmpty()) {
                return raw
                        .replace("; wv)", ")")
                        .replace("; wv;", ";")
                        .replace("; wv", "")
                        .replaceAll("Version/\\d+\\.\\d+\\s*", "");
            }
        } catch (Exception ignored) {}
        return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36";
    }

    private static void registerGlobalReceiverIfNeeded(Context appCtx) {
        if (isReceiverRegisteredOnApp || appCtx == null) return;
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            filter.addAction(ACTION_CANCEL_DL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appCtx.registerReceiver(globalDownloadReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                appCtx.registerReceiver(globalDownloadReceiver, filter);
            }
            isReceiverRegisteredOnApp = true;
        } catch (Exception ignored) {}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("OPEN_MEDIA_DIALOG", false)) {
            showMediaDownloadsDialog();
        }
    }

    private void requestAllMobileAndChromePermissions(boolean requestedByUser) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> needed = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= 33) {
                addPermIfNeeded(needed, "android.permission.POST_NOTIFICATIONS");
                addPermIfNeeded(needed, "android.permission.READ_MEDIA_VIDEO");
                addPermIfNeeded(needed, "android.permission.READ_MEDIA_AUDIO");
                addPermIfNeeded(needed, "android.permission.READ_MEDIA_IMAGES");
            } else {
                addPermIfNeeded(needed, android.Manifest.permission.READ_EXTERNAL_STORAGE);
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    addPermIfNeeded(needed, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }

            if (requestedByUser) {
                addPermIfNeeded(needed, android.Manifest.permission.CAMERA);
                addPermIfNeeded(needed, android.Manifest.permission.RECORD_AUDIO);
                addPermIfNeeded(needed, android.Manifest.permission.ACCESS_FINE_LOCATION);
                addPermIfNeeded(needed, android.Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (!needed.isEmpty()) {
                try {
                    launchingInternalActivity = true;
                    requestPermissions(needed.toArray(new String[0]), 701);
                } catch (Exception ignored) {}
            } else if (requestedByUser) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            }

            if (requestedByUser) {
                try {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        launchingInternalActivity = true;
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void addPermIfNeeded(ArrayList<String> list, String perm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                    list.add(perm);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void setupNotificationChannelsStatic(Context ctx) {
        if (ctx == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm == null) return;

                NotificationChannel progressChannel = new NotificationChannel(
                        DL_CHANNEL_PROGRESS,
                        "ProStream Downloads",
                        NotificationManager.IMPORTANCE_LOW);
                progressChannel.setShowBadge(false);
                nm.createNotificationChannel(progressChannel);

                NotificationChannel doneChannel = new NotificationChannel(
                        DL_CHANNEL_DONE,
                        "Completed Downloads",
                        NotificationManager.IMPORTANCE_HIGH);
                doneChannel.enableVibration(true);
                nm.createNotificationChannel(doneChannel);
            } catch (Exception ignored) {}
        }
    }

    private static void startNotificationProgressTrackerStatic(Context ctx) {
        if (ctx != null) {
            appContextRef = ctx.getApplicationContext();
        }
        if (!isNotificationTrackerRunning) {
            isNotificationTrackerRunning = true;
            bgDownloadHandler.removeCallbacks(staticLiveDownloadPoller);
            bgDownloadHandler.post(staticLiveDownloadPoller);
        }
    }

    private static void startBackgroundDownloadKeeperService(Context ctx) {
        if (ctx == null) return;
        acquireTurboDownloadBoosterStatic(ctx);
    }

    private static void acquireTurboDownloadBoosterStatic(Context ctx) {
        if (ctx == null) return;
        try {
            Context appCtx = ctx.getApplicationContext();
            if (turboWifiLock == null) {
                WifiManager wm = (WifiManager) appCtx.getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    int lockMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            ? WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                            : WifiManager.WIFI_MODE_FULL_HIGH_PERF;
                    turboWifiLock = wm.createWifiLock(lockMode, "ProStream:TurboWifiLock");
                    turboWifiLock.setReferenceCounted(false);
                }
            }
            if (turboWifiLock != null && !turboWifiLock.isHeld()) {
                turboWifiLock.acquire();
            }

            if (turboWakeLock == null) {
                PowerManager pm = (PowerManager) appCtx.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    turboWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProStream:TurboDownloadWakeLock");
                    turboWakeLock.setReferenceCounted(false);
                }
            }
            if (turboWakeLock != null && !turboWakeLock.isHeld()) {
                turboWakeLock.acquire(60 * 60 * 1000L);
            }
        } catch (Throwable ignored) {}
    }

    private static void releaseTurboDownloadBoosterStatic() {
        try {
            if (turboWifiLock != null && turboWifiLock.isHeld()) {
                turboWifiLock.release();
            }
        } catch (Throwable ignored) {}
        try {
            if (turboWakeLock != null && turboWakeLock.isHeld()) {
                turboWakeLock.release();
            }
        } catch (Throwable ignored) {}
    }

    private static boolean updateLiveDownloadNotificationsAndSpeedStatic(Context ctx) {
        boolean anyRunning = false;
        if (ctx == null) return false;
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return false;

            for (CustomDownloadTask task : customTasksMap.values()) {
                if (task.cancelled || task.status == DownloadManager.STATUS_FAILED) {
                    continue;
                }
                if (task.status == DownloadManager.STATUS_RUNNING || task.status == DownloadManager.STATUS_PENDING) {
                    anyRunning = true;
                    int pct = task.getPercent();
                    long dl = task.downloadedBytes.get();
                    String spd = formatSpeedStatic(task.speedBps);
                    String sizeStr = task.totalBytes > 0
                            ? (formatBytesStatic(dl) + " / " + formatBytesStatic(task.totalBytes))
                            : formatBytesStatic(dl);
                    String statusLine = pct + "%  •  " + sizeStr + "  •  " + spd;
                    postMediaStyleNotificationCardStatic(ctx, nm, task.id, task.title, statusLine, pct, spd, true);
                }
            }

            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null && !trackedDownloadIds.isEmpty()) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterByStatus(
                        DownloadManager.STATUS_RUNNING
                                | DownloadManager.STATUS_PENDING
                                | DownloadManager.STATUS_PAUSED);

                Cursor c = dm.query(query);
                if (c != null) {
                    int idIdx = c.getColumnIndex(DownloadManager.COLUMN_ID);
                    int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
                    int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int soFarIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                    long nowMs = System.currentTimeMillis();

                    while (c.moveToNext()) {
                        long id = idIdx >= 0 ? c.getLong(idIdx) : -1L;
                        if (!trackedDownloadIds.contains(String.valueOf(id))) {
                            continue;
                        }

                        String fileTitle = (titleIdx >= 0 && c.getString(titleIdx) != null)
                                ? c.getString(titleIdx) : "Video.mp4";
                        int status = statusIdx >= 0 ? c.getInt(statusIdx) : 0;
                        long soFar = soFarIdx >= 0 ? c.getLong(soFarIdx) : 0L;
                        long total = totalIdx >= 0 ? c.getLong(totalIdx) : -1L;

                        if (status == DownloadManager.STATUS_FAILED) {
                            continue;
                        }

                        anyRunning = true;

                        long prevBytes = lastBytesMap.containsKey(id) ? lastBytesMap.get(id) : soFar;
                        long prevTime = lastTimeMap.containsKey(id) ? lastTimeMap.get(id) : (nowMs - 1000L);
                        long dt = Math.max(250L, nowMs - prevTime);
                        long dBytes = Math.max(0L, soFar - prevBytes);
                        long bytesPerSec = (dBytes * 1000L) / dt;

                        if (lastBytesMap.containsKey(id)) {
                            long oldSpeed = currentSpeedMap.containsKey(id) ? currentSpeedMap.get(id) : bytesPerSec;
                            bytesPerSec = (oldSpeed + bytesPerSec) / 2L;
                        }
                        lastBytesMap.put(id, soFar);
                        lastTimeMap.put(id, nowMs);
                        currentSpeedMap.put(id, bytesPerSec);

                        int percent = (total > 0) ? (int) ((soFar * 100L) / total) : 0;
                        String speedStr = formatSpeedStatic(bytesPerSec);
                        String sizeProgressStr = (total > 0)
                                ? (formatBytesStatic(soFar) + " / " + formatBytesStatic(total))
                                : formatBytesStatic(soFar);

                        String statusLine = percent + "%  •  " + sizeProgressStr + "  •  " + speedStr;
                        postMediaStyleNotificationCardStatic(ctx, nm, id, fileTitle, statusLine, percent, speedStr, true);
                    }
                    c.close();
                }
            }
        } catch (Exception ignored) {}
        return anyRunning;
    }

    private static void updateSingleCompletedNotificationStatic(Context ctx, long downloadId) {
        if (ctx == null) return;
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            CustomDownloadTask customTask = customTasksMap.get(downloadId);
            if (customTask != null && customTask.status == DownloadManager.STATUS_SUCCESSFUL) {
                String doneLine = formatBytesStatic(customTask.downloadedBytes.get()) + "  •  Completed";
                postMediaStyleNotificationCardStatic(ctx, nm, downloadId, customTask.title, doneLine, 100, "Done", false);
                return;
            }

            if (!trackedDownloadIds.contains(String.valueOf(downloadId))) return;

            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) return;

            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloadId);
            Cursor c = dm.query(q);
            if (c != null) {
                if (c.moveToFirst()) {
                    int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
                    int totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

                    String title = (titleIdx >= 0 && c.getString(titleIdx) != null) ? c.getString(titleIdx) : "Video.mp4";
                    long total = totalIdx >= 0 ? c.getLong(totalIdx) : 0L;
                    int status = statusIdx >= 0 ? c.getInt(statusIdx) : 0;

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        String doneLine = formatBytesStatic(total) + "  •  Completed";
                        postMediaStyleNotificationCardStatic(ctx, nm, downloadId, title, doneLine, 100, "Done", false);
                    }
                }
                c.close();
            }
        } catch (Exception ignored) {}
    }

    private static void postMediaStyleNotificationCardStatic(Context ctx, NotificationManager nm, long downloadId, String fileTitle, String contentText, int percent, String subPillText, boolean isOngoing) {
        if (ctx == null || nm == null) return;
        try {
            int notifId = (int) (Math.abs(downloadId) % 100000L) + 4000;

            Intent openMediaIntent = new Intent(ctx, HomeActivity.class);
            openMediaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openMediaIntent.putExtra("OPEN_MEDIA_DIALOG", true);
            int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                    : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent contentPi = PendingIntent.getActivity(ctx, notifId, openMediaIntent, piFlags);

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(ctx, isOngoing ? DL_CHANNEL_PROGRESS : DL_CHANNEL_DONE);
            } else {
                builder = new Notification.Builder(ctx);
            }

            builder.setSmallIcon(isOngoing ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_download_done)
                    .setLargeIcon(createMediaNotificationBadgeBitmapStatic(percent, !isOngoing))
                    .setContentTitle(fileTitle)
                    .setContentText(contentText)
                    .setSubText(subPillText)
                    .setContentIntent(contentPi)
                    .setOngoing(isOngoing)
                    .setAutoCancel(!isOngoing)
                    .setOnlyAlertOnce(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setColor(Color.parseColor(isOngoing ? "#00E5FF" : "#00F5A0"));
                builder.setCategory(Notification.CATEGORY_PROGRESS);
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            if (isOngoing) {
                builder.setProgress(100, Math.max(2, percent), false);

                Intent cancelIntent = new Intent(ACTION_CANCEL_DL);
                cancelIntent.setPackage(ctx.getPackageName());
                cancelIntent.putExtra("DL_ID", downloadId);
                PendingIntent cancelPi = PendingIntent.getBroadcast(ctx, notifId + 1, cancelIntent, piFlags);

                builder.addAction(android.R.drawable.ic_menu_view, "Downloads", contentPi);
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPi);
            } else {
                builder.setProgress(0, 0, false);
                builder.addAction(android.R.drawable.ic_media_play, "Open", contentPi);
            }

            nm.notify(notifId, builder.build());
        } catch (Exception ignored) {}
    }

    private static Bitmap createMediaNotificationBadgeBitmapStatic(int percent, boolean isDone) {
        int size = 128;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#121726"));
        RectF rect = new RectF(4, 4, size - 4, size - 4);
        canvas.drawRoundRect(rect, 34f, 34f, bgPaint);

        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(8f);
        trackPaint.setColor(Color.parseColor("#26FFFFFF"));
        RectF arcRect = new RectF(14, 14, size - 14, size - 14);
        canvas.drawArc(arcRect, 0, 360, false, trackPaint);

        Paint progPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progPaint.setStyle(Paint.Style.STROKE);
        progPaint.setStrokeWidth(8f);
        progPaint.setStrokeCap(Paint.Cap.ROUND);
        progPaint.setColor(Color.parseColor(isDone ? "#00F5A0" : "#00E5FF"));
        float sweep = (Math.max(5, Math.min(100, percent)) * 360f) / 100f;
        canvas.drawArc(arcRect, -90f, sweep, false, progPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(isDone ? 34f : 30f);
        String label = isDone ? "✓" : (percent + "%");
        int yPos = (int) ((size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f));
        canvas.drawText(label, size / 2f, yPos, textPaint);

        return bmp;
    }

    private static String formatSpeedStatic(long bytesPerSec) {
        if (bytesPerSec <= 0) return "Turbo...";
        double mbps = bytesPerSec / (1024.0 * 1024.0);
        if (mbps >= 1.0) {
            return String.format(Locale.US, "%.2f MB/s", mbps);
        }
        double kbps = bytesPerSec / 1024.0;
        return String.format(Locale.US, "%.0f KB/s", Math.max(1.0, kbps));
    }

    private static String formatBytesStatic(long bytes) {
        if (bytes <= 0) return "0 MB";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1024.0) {
            return String.format(Locale.US, "%.2f GB", mb / 1024.0);
        }
        return String.format(Locale.US, "%.1f MB", mb);
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        } catch (Exception ignored) {}
        return true;
    }

    private void reloadActivePageCleanly() {
        hideModernOfflineScreen();
        if (webView != null) {
            String retryUrl = lastFailedUrlMap.get(webView);
            String curUrl = webView.getUrl();
            if (retryUrl != null && !retryUrl.isEmpty()) {
                lastFailedUrlMap.remove(webView);
                webView.loadUrl(retryUrl);
            } else if (curUrl == null || curUrl.isEmpty() || curUrl.startsWith("about:blank")) {
                if (currentSite != null) {
                    webView.loadUrl(currentSite.getEffectiveUrl());
                }
            } else {
                webView.reload();
            }
        } else if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void goBackWithSmoothAnimation() {
        if (webView == null || !webView.canGoBack()) return;
        hideModernOfflineScreen();

        final WebView target = webView;
        target.stopLoading();

        int stepsBack = -1;
        try {
            WebBackForwardList history = target.copyBackForwardList();
            if (history != null) {
                int curIdx = history.getCurrentIndex();
                if (curIdx - 1 >= 0) {
                    WebHistoryItem prevItem = history.getItemAtIndex(curIdx - 1);
                    if (prevItem != null && "about:blank".equalsIgnoreCase(prevItem.getUrl()) && curIdx - 2 >= 0) {
                        stepsBack = -2;
                    }
                }
            }
        } catch (Exception ignored) {}

        if (stepsBack < -1 && target.canGoBackOrForward(stepsBack)) {
            target.goBackOrForward(stepsBack);
        } else {
            target.goBack();
        }

        target.setAlpha(0.55f);
        target.setTranslationX(-dpToPx(24));
        target.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void setupMultiWebViewAndOfflineContainer() {
        webViewHolder = new FrameLayout(this) {
            private float downX, downY;
            private boolean pullingDownFromTop = false;

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = ev.getX();
                        downY = ev.getY();
                        pullingDownFromTop = false;
                        if (webView != null && webView.getScrollY() <= dpToPx(8) && getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(ev.getX() - downX);
                        float dy = ev.getY() - downY;
                        if (webView != null && webView.getScrollY() <= dpToPx(8) && dy > dpToPx(8) && dy > dx * 1.15f) {
                            pullingDownFromTop = true;
                            if (getParent() != null) {
                                getParent().requestDisallowInterceptTouchEvent(false);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        pullingDownFromTop = false;
                        break;
                }
                return super.dispatchTouchEvent(ev);
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                if (disallowIntercept && (pullingDownFromTop || (webView != null && webView.getScrollY() <= dpToPx(8)))) {
                    super.requestDisallowInterceptTouchEvent(false);
                    return;
                }
                super.requestDisallowInterceptTouchEvent(disallowIntercept);
            }

            @Override
            public boolean canScrollVertically(int direction) {
                if (direction < 0) {
                    if (modernOfflineOverlay != null && modernOfflineOverlay.getVisibility() == View.VISIBLE) {
                        return false;
                    }
                    return webView != null && webView.getScrollY() > dpToPx(6);
                }
                return super.canScrollVertically(direction);
            }
        };
        webViewHolder.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webViewHolder.setBackgroundColor(Color.parseColor("#080B12"));

        modernOfflineOverlay = buildModernOfflineOverlay();
        modernOfflineOverlay.setVisibility(View.GONE);
        webViewHolder.addView(modernOfflineOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (swipeRefresh != null) {
            if (webView != null && webView.getParent() == swipeRefresh) {
                swipeRefresh.removeView(webView);
            }
            swipeRefresh.addView(webViewHolder);
        }
    }

    private LinearLayout buildModernOfflineOverlay() {
        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setPadding(dpToPx(28), dpToPx(28), dpToPx(28), dpToPx(28));
        overlay.setBackgroundColor(Color.parseColor("#080B12"));
        overlay.setClickable(true);
        overlay.setFocusable(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dpToPx(24));
        cardBg.setColor(Color.parseColor("#121726"));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#3300E5FF"));
        card.setBackground(cardBg);

        TextView iconTv = new TextView(this);
        iconTv.setText("📡");
        iconTv.setTextSize(38f);
        iconTv.setGravity(Gravity.CENTER);
        card.addView(iconTv);

        TextView titleTv = new TextView(this);
        titleTv.setText("No Internet Connection");
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(17f);
        titleTv.setTypeface(modernFont, Typeface.BOLD);
        titleTv.setGravity(Gravity.CENTER);
        titleTv.setPadding(0, dpToPx(10), 0, dpToPx(6));
        card.addView(titleTv);

        tvOfflineUrlInfo = new TextView(this);
        tvOfflineUrlInfo.setTextColor(Color.parseColor("#64748B"));
        tvOfflineUrlInfo.setTextSize(11f);
        tvOfflineUrlInfo.setSingleLine(true);
        tvOfflineUrlInfo.setGravity(Gravity.CENTER);
        tvOfflineUrlInfo.setPadding(0, 0, 0, dpToPx(16));
        card.addView(tvOfflineUrlInfo);

        TextView btnRetry = createDialogButton("Retry", "#06101E", "#00F5A0");
        btnRetry.setTextSize(13f);
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retryParams.setMargins(0, 0, 0, dpToPx(8));
        btnRetry.setLayoutParams(retryParams);
        attachTouchBounceAnimation(btnRetry);
        btnRetry.setOnClickListener(v -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
            reloadActivePageCleanly();
        });
        card.addView(btnRetry);

        TextView btnSwitch = createDialogButton("Switch Site", "#7DD3FC", "#17253B");
        btnSwitch.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        attachTouchBounceAnimation(btnSwitch);
        btnSwitch.setOnClickListener(v -> showInstantAppSwitcherDialog());
        card.addView(btnSwitch);

        overlay.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return overlay;
    }

    private void showModernOfflineScreen(String failingUrl) {
        if (modernOfflineOverlay == null) return;
        if (tvOfflineUrlInfo != null) {
            tvOfflineUrlInfo.setText(failingUrl != null ? failingUrl : "");
        }
        modernOfflineOverlay.bringToFront();
        if (modernOfflineOverlay.getVisibility() != View.VISIBLE) {
            modernOfflineOverlay.setVisibility(View.VISIBLE);
            modernOfflineOverlay.setAlpha(0f);
            modernOfflineOverlay.setScaleX(0.92f);
            modernOfflineOverlay.setScaleY(0.92f);
            modernOfflineOverlay.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(240)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }
    }

    private void hideModernOfflineScreen() {
        if (modernOfflineOverlay != null && modernOfflineOverlay.getVisibility() == View.VISIBLE) {
            modernOfflineOverlay.setVisibility(View.GONE);
        }
    }

    private void switchToSiteInstant(SiteItem site, boolean forceReload) {
        switchToSiteInstantInternal(site, forceReload, true);
    }

    private void switchToSiteInstantInternal(SiteItem site, boolean forceReload, boolean recordHistory) {
        if (site == null || webViewHolder == null) return;
        if (recordHistory && currentSite != null && !currentSite.url.equalsIgnoreCase(site.url)) {
            siteHistoryStack.push(currentSite);
        }
        currentSite = site;
        updateUIForCurrentSite();
        hideModernOfflineScreen();

        for (int i = 0; i < webViewHolder.getChildCount(); i++) {
            View child = webViewHolder.getChildAt(i);
            if (child != modernOfflineOverlay) {
                child.setVisibility(View.GONE);
                if (child instanceof WebView) {
                    try {
                        WebView bgWv = (WebView) child;
                        bgWv.evaluateJavascript("(function(){var m=document.querySelectorAll('video,audio');for(var i=0;i<m.length;i++){try{m[i].pause();}catch(e){}}})();", null);
                        if (batterySaverEnabled) {
                            bgWv.onPause();
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        WebView targetWv = webViewPool.get(site.url);
        if (targetWv == null) {
            targetWv = createConfiguredWebView(site);
            webViewPool.put(site.url, targetWv);
            webViewHolder.addView(targetWv, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            targetWv.loadUrl(site.getEffectiveUrl());
        } else if (forceReload || targetWv.getUrl() == null || targetWv.getUrl().startsWith("about:blank")) {
            targetWv.loadUrl(site.getEffectiveUrl());
        }

        targetWv.setVisibility(View.VISIBLE);
        targetWv.setAlpha(0.55f);
        targetWv.setTranslationX(dpToPx(22));
        targetWv.setScaleX(0.97f);
        targetWv.setScaleY(0.97f);
        targetWv.animate()
                .alpha(1f)
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        try {
            targetWv.onResume();
            targetWv.resumeTimers();
        } catch (Exception ignored) {}
        webView = targetWv;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
            } catch (Exception ignored) {}
        }
        if (darkModeEnabled) {
            applyWebViewDarkMode(webView, true);
        }
    }

    private void startFloatingHubBreathingAnimation() {
        for (ValueAnimator a : hubIdleAnimators) {
            try { a.cancel(); } catch (Exception ignored) {}
        }
        hubIdleAnimators.clear();
        if (btnFloatingHub == null) return;

        ObjectAnimator hubPulse = ObjectAnimator.ofPropertyValuesHolder(
                btnFloatingHub,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f, 1f)
        );
        hubPulse.setDuration(2400);
        hubPulse.setRepeatCount(ValueAnimator.INFINITE);
        hubPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        hubPulse.start();
        hubIdleAnimators.add(hubPulse);

        ValueAnimator hubStrokeGlow = ValueAnimator.ofArgb(
                Color.parseColor("#00F5A0"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#7C3AED"),
                Color.parseColor("#00F5A0")
        );
        hubStrokeGlow.setDuration(3600);
        hubStrokeGlow.setRepeatCount(ValueAnimator.INFINITE);
        hubStrokeGlow.addUpdateListener(anim -> {
            if (btnFloatingHub != null) {
                btnFloatingHub.setStrokeColor((int) anim.getAnimatedValue());
            }
        });
        hubStrokeGlow.start();
        hubIdleAnimators.add(hubStrokeGlow);
    }

    private void setupDraggableFloatingHub() {
        if (btnFloatingHub == null) return;

        btnFloatingHub.post(() -> {
            float savedX = sharedPrefs.getFloat("HUB_POS_X", -1f);
            float savedY = sharedPrefs.getFloat("HUB_POS_Y", -1f);
            if (savedX >= 0f && savedY >= 0f && rootLayout != null) {
                float maxX = Math.max(0, rootLayout.getWidth() - btnFloatingHub.getWidth());
                float maxY = Math.max(0, rootLayout.getHeight() - btnFloatingHub.getHeight());
                btnFloatingHub.setX(Math.min(savedX, maxX));
                btnFloatingHub.setY(Math.min(savedY, maxY));
            }
        });

        btnFloatingHub.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY, downRawX, downRawY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        isDragging = false;
                        v.animate().scaleX(0.84f).scaleY(0.84f).setDuration(90).start();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float moveX = Math.abs(event.getRawX() - downRawX);
                        float moveY = Math.abs(event.getRawY() - downRawY);
                        if (moveX > dpToPx(8) || moveY > dpToPx(8)) {
                            isDragging = true;
                            float newX = event.getRawX() + dX;
                            float newY = event.getRawY() + dY;
                            int parentW = rootLayout != null ? rootLayout.getWidth() : getResources().getDisplayMetrics().widthPixels;
                            int parentH = rootLayout != null ? rootLayout.getHeight() : getResources().getDisplayMetrics().heightPixels;
                            newX = Math.max(dpToPx(8), Math.min(newX, parentW - v.getWidth() - dpToPx(8)));
                            newY = Math.max(dpToPx(16), Math.min(newY, parentH - v.getHeight() - dpToPx(16)));
                            v.setX(newX);
                            v.setY(newY);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(new OvershootInterpolator(1.75f)).start();
                        if (!isDragging) {
                            if (ivHubIcon != null) {
                                ivHubIcon.animate().rotationBy(360f).setDuration(380).setInterpolator(new OvershootInterpolator(1.2f)).start();
                            }
                            showInstantAppSwitcherDialog();
                        } else {
                            sharedPrefs.edit()
                                    .putFloat("HUB_POS_X", v.getX())
                                    .putFloat("HUB_POS_Y", v.getY())
                                    .apply();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void stopPopupAnimators() {
        for (ValueAnimator anim : activePopupAnimators) {
            try {
                anim.cancel();
            } catch (Exception ignored) {}
        }
        activePopupAnimators.clear();
    }

    private void showAnimatedDialog(final AlertDialog dialog, final View rootCard) {
        if (dialog == null) return;
        forceEnableAnimations();
        if (rootCard != null) {
            rootCard.setAlpha(0f);
            rootCard.setScaleX(0.76f);
            rootCard.setScaleY(0.76f);
            rootCard.setTranslationY(dpToPx(42));
            dialog.setOnShowListener(d -> rootCard.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(320)
                    .setInterpolator(new OvershootInterpolator(1.25f))
                    .start());
        }
        dialog.show();
    }

    private void dismissPopupWithAnimation(final AlertDialog dialog, final View animTarget, final Runnable afterDismiss) {
        if (dialog == null || !dialog.isShowing()) {
            if (afterDismiss != null) afterDismiss.run();
            return;
        }
        if (animTarget == null) {
            dialog.dismiss();
            if (afterDismiss != null) afterDismiss.run();
            return;
        }
        animTarget.animate()
                .alpha(0f)
                .scaleX(0.82f)
                .scaleY(0.82f)
                .translationY(dpToPx(30))
                .setDuration(160)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {}
                    if (afterDismiss != null) afterDismiss.run();
                })
                .start();
    }

    private void attachTouchBounceAnimation(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new OvershootInterpolator(1.6f)).start();
                    break;
            }
            return false;
        });
    }

    private void showInstantAppSwitcherDialog() {
        try {
            forceEnableAnimations();
            stopPopupAnimators();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_switcher, null);
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            final MaterialCardView rootCard = dialogView.findViewById(R.id.dialogRootCard);
            final View animTarget = rootCard != null ? rootCard : dialogView;
            animTarget.setAlpha(0f);
            animTarget.setScaleX(0.70f);
            animTarget.setScaleY(0.70f);
            animTarget.setTranslationY(dpToPx(60));

            final List<View> cascadeViews = new ArrayList<>();

            View headerRow = dialogView.findViewById(R.id.dialogHeaderRow);
            if (headerRow != null) {
                prepareCascadeView(headerRow);
                cascadeViews.add(headerRow);
            }

            TextView tvCount = dialogView.findViewById(R.id.tvReadyCount);
            if (tvCount != null) {
                tvCount.setText(siteList.size() + " SITE" + (siteList.size() > 1 ? "S" : ""));
            }

            TextView tvDialogTelegram = dialogView.findViewById(R.id.tvDialogTelegram);
            if (tvDialogTelegram != null) {
                tvDialogTelegram.setText("t.me/contactyasinbot");
                tvDialogTelegram.setTextColor(Color.parseColor("#00F5A0"));
                tvDialogTelegram.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, () -> openUrlSafely(TELEGRAM_BOT_URL));
                });
            }

            View btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
            if (btnCloseDialog != null) {
                attachTouchBounceAnimation(btnCloseDialog);
                btnCloseDialog.setOnClickListener(v -> dismissPopupWithAnimation(dialog, animTarget, null));
            }

            TextView tvAllRestored = dialogView.findViewById(R.id.tvAllRestored);
            if (tvAllRestored != null) {
                tvAllRestored.setText("↻ Reload");
                tvAllRestored.setTextColor(Color.parseColor("#00F5A0"));
                GradientDrawable refBg = new GradientDrawable();
                refBg.setCornerRadius(dpToPx(8));
                refBg.setColor(Color.parseColor("#12312B"));
                refBg.setStroke(dpToPx(1), Color.parseColor("#4D00F5A0"));
                tvAllRestored.setBackground(refBg);
                attachTouchBounceAnimation(tvAllRestored);

                tvAllRestored.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, () -> {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
                        reloadActivePageCleanly();
                    });
                });
            }

            final ScrollView sitesScrollView = dialogView.findViewById(R.id.dialogSitesScrollView);
            if (sitesScrollView != null) {
                ViewGroup.LayoutParams svParams = sitesScrollView.getLayoutParams();
                if (siteList.size() > 3) {
                    svParams.height = dpToPx(204);
                    sitesScrollView.setLayoutParams(svParams);
                    sitesScrollView.setVerticalScrollBarEnabled(true);
                    sitesScrollView.setScrollbarFadingEnabled(false);
                    sitesScrollView.setOnTouchListener((v, event) -> {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return false;
                    });
                } else {
                    svParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    sitesScrollView.setLayoutParams(svParams);
                }
            }

            LinearLayout container = dialogView.findViewById(R.id.dialogSitesContainer);
            if (container != null) {
                container.removeAllViews();
                for (int i = 0; i < siteList.size(); i++) {
                    final SiteItem site = siteList.get(i);
                    final boolean isActive = (currentSite != null && currentSite.url.equalsIgnoreCase(site.url));
                    int siteColorInt = parseColorSafe(site.color, appColor);

                    LinearLayout itemLayout = new LinearLayout(this);
                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(60));
                    itemParams.setMargins(0, 0, 0, i == siteList.size() - 1 ? 0 : dpToPx(8));
                    itemLayout.setLayoutParams(itemParams);
                    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                    itemLayout.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
                    itemLayout.setGravity(Gravity.CENTER_VERTICAL);

                    final GradientDrawable cardBg = new GradientDrawable();
                    cardBg.setCornerRadius(dpToPx(15));
                    if (isActive) {
                        cardBg.setColor(Color.parseColor("#1E263D"));
                        cardBg.setStroke(dpToPx(2), Color.parseColor("#00F5A0"));
                    } else {
                        cardBg.setColor(Color.parseColor("#181B26"));
                        cardBg.setStroke(dpToPx(1), Color.parseColor("#22FFFFFF"));
                    }
                    itemLayout.setBackground(cardBg);

                    final ImageView icon = new ImageView(this);
                    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    icon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)));
                    loadSiteOgPhotoAsync(site, icon, siteColorInt);

                    LinearLayout textColumn = new LinearLayout(this);
                    textColumn.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    colParams.setMargins(dpToPx(12), 0, dpToPx(8), 0);
                    textColumn.setLayoutParams(colParams);

                    TextView tvName = new TextView(this);
                    tvName.setText(site.name);
                    tvName.setTextColor(Color.WHITE);
                    tvName.setTextSize(14f);
                    tvName.setTypeface(modernFont, Typeface.BOLD);
                    tvName.setSingleLine(true);

                    TextView tvUrl = new TextView(this);
                    tvUrl.setText(site.url.replaceFirst("^https?://", "").replaceAll("/$", ""));
                    tvUrl.setTextColor(Color.parseColor("#94A3B8"));
                    tvUrl.setTextSize(11f);
                    tvUrl.setSingleLine(true);

                    textColumn.addView(tvName);
                    textColumn.addView(tvUrl);

                    itemLayout.addView(icon);
                    itemLayout.addView(textColumn);

                    if (isActive) {
                        TextView tag = new TextView(this);
                        tag.setText("ACTIVE");
                        tag.setTextColor(Color.parseColor("#00F5A0"));
                        tag.setTextSize(10f);
                        tag.setTypeface(modernFont, Typeface.BOLD);
                        tag.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));

                        GradientDrawable badgeBg = new GradientDrawable();
                        badgeBg.setCornerRadius(dpToPx(6));
                        badgeBg.setColor(Color.parseColor("#12312B"));
                        tag.setBackground(badgeBg);
                        itemLayout.addView(tag);
                    } else {
                        TextView switchTv = new TextView(this);
                        switchTv.setText("Open ›");
                        switchTv.setTextColor(Color.parseColor("#00E5FF"));
                        switchTv.setTextSize(11.5f);
                        switchTv.setTypeface(modernFont, Typeface.BOLD);
                        itemLayout.addView(switchTv);
                    }

                    attachTouchBounceAnimation(itemLayout);
                    itemLayout.setOnClickListener(v -> {
                        dismissPopupWithAnimation(dialog, animTarget, () -> switchToSiteInstant(site, false));
                    });

                    prepareCascadeView(itemLayout);
                    cascadeViews.add(itemLayout);
                    container.addView(itemLayout);
                }
            }

            View btnMedia = dialogView.findViewById(R.id.btnDialogTheme);
            if (btnMedia != null) {
                prepareCascadeView(btnMedia);
                cascadeViews.add(btnMedia);
                attachTouchBounceAnimation(btnMedia);
                btnMedia.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showMediaDownloadsDialog);
                });
            }

            View btnDownloader = dialogView.findViewById(R.id.btnDialogDownloader);
            if (btnDownloader != null) {
                prepareCascadeView(btnDownloader);
                cascadeViews.add(btnDownloader);
                attachTouchBounceAnimation(btnDownloader);
                btnDownloader.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, () -> showUniversalLinkDownloaderDialog(""));
                });
            }

            View btnAddSite = dialogView.findViewById(R.id.btnDialogAddSite);
            if (btnAddSite != null) {
                prepareCascadeView(btnAddSite);
                cascadeViews.add(btnAddSite);
                attachTouchBounceAnimation(btnAddSite);
                btnAddSite.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showSiteManagerDialog);
                });
            }

            View btnSettings = dialogView.findViewById(R.id.btnDialogSettings);
            if (btnSettings != null) {
                prepareCascadeView(btnSettings);
                cascadeViews.add(btnSettings);
                attachTouchBounceAnimation(btnSettings);
                btnSettings.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showAllSettingsDialog);
                });
            }

            TextView tvQuickDark = dialogView.findViewById(R.id.tvQuickDark);
            if (tvQuickDark != null) {
                tvQuickDark.setText(darkModeEnabled ? "🌙 Dark ON" : "🌙 Dark");
                tvQuickDark.setTextColor(Color.parseColor(darkModeEnabled ? "#FDE047" : "#E2E8F0"));
            }

            View btnQuickDark = dialogView.findViewById(R.id.btnQuickDark);
            if (btnQuickDark != null) {
                prepareCascadeView(btnQuickDark);
                cascadeViews.add(btnQuickDark);
                attachTouchBounceAnimation(btnQuickDark);
                btnQuickDark.setOnClickListener(v -> {
                    darkModeEnabled = !darkModeEnabled;
                    sharedPrefs.edit().putBoolean("DARK_MODE", darkModeEnabled).apply();
                    for (WebView wv : webViewPool.values()) {
                        applyWebViewDarkMode(wv, darkModeEnabled);
                    }
                    Toast.makeText(this, darkModeEnabled ? "Dark Mode ON" : "Dark Mode OFF", Toast.LENGTH_SHORT).show();
                    dismissPopupWithAnimation(dialog, animTarget, null);
                });
            }

            View btnQuickAppLock = dialogView.findViewById(R.id.btnQuickAppLock);
            if (btnQuickAppLock != null) {
                prepareCascadeView(btnQuickAppLock);
                cascadeViews.add(btnQuickAppLock);
                attachTouchBounceAnimation(btnQuickAppLock);
                btnQuickAppLock.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showAppLockDialog);
                });
            }

            TextView tvQuickEco = dialogView.findViewById(R.id.tvQuickEco);
            if (tvQuickEco != null) {
                tvQuickEco.setText(batterySaverEnabled ? "🔋 Eco ON" : "🔋 Eco OFF");
                tvQuickEco.setTextColor(Color.parseColor(batterySaverEnabled ? "#6EE7B7" : "#94A3B8"));
            }

            View btnQuickEco = dialogView.findViewById(R.id.btnQuickEco);
            if (btnQuickEco != null) {
                prepareCascadeView(btnQuickEco);
                cascadeViews.add(btnQuickEco);
                attachTouchBounceAnimation(btnQuickEco);
                btnQuickEco.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showBatterySaverDialog);
                });
            }

            View btnQuickAdBlock = dialogView.findViewById(R.id.btnQuickAdBlock);
            if (btnQuickAdBlock != null) {
                prepareCascadeView(btnQuickAdBlock);
                cascadeViews.add(btnQuickAdBlock);
                attachTouchBounceAnimation(btnQuickAdBlock);
                btnQuickAdBlock.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showPrivateDnsSetupDialog);
                });
            }

            View btnQuickCache = dialogView.findViewById(R.id.btnQuickCache);
            if (btnQuickCache != null) {
                prepareCascadeView(btnQuickCache);
                cascadeViews.add(btnQuickCache);
                attachTouchBounceAnimation(btnQuickCache);
                btnQuickCache.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::clearAppCacheNow);
                });
            }

            final View cardFlashBadge = dialogView.findViewById(R.id.cardFlashBadge);

            dialog.setOnShowListener(d -> {
                forceEnableAnimations();
                animTarget.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setDuration(350)
                        .setInterpolator(new OvershootInterpolator(1.30f))
                        .start();

                for (int idx = 0; idx < cascadeViews.size(); idx++) {
                    View cv = cascadeViews.get(idx);
                    animateCascadeItemNow(cv, 25 + (idx * 24));
                }

                if (rootCard != null) {
                    ValueAnimator borderGlow = ValueAnimator.ofArgb(
                            Color.parseColor("#00F5A0"),
                            Color.parseColor("#00E5FF"),
                            Color.parseColor("#7C3AED"),
                            Color.parseColor("#00F5A0"));
                    borderGlow.setDuration(3000);
                    borderGlow.setRepeatCount(ValueAnimator.INFINITE);
                    borderGlow.addUpdateListener(a -> rootCard.setStrokeColor((int) a.getAnimatedValue()));
                    borderGlow.start();
                    activePopupAnimators.add(borderGlow);
                }

                if (cardFlashBadge != null) {
                    ObjectAnimator logoPulse = ObjectAnimator.ofPropertyValuesHolder(
                            cardFlashBadge,
                            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.12f, 1f),
                            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.12f, 1f)
                    );
                    logoPulse.setDuration(1800);
                    logoPulse.setRepeatCount(ValueAnimator.INFINITE);
                    logoPulse.start();
                    activePopupAnimators.add(logoPulse);
                }
            });

            dialog.setOnDismissListener(d -> stopPopupAnimators());
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareCascadeView(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(dpToPx(22));
        view.setScaleX(0.90f);
        view.setScaleY(0.90f);
    }

    private void animateCascadeItemNow(final View view, int delayMs) {
        if (view == null) return;
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delayMs)
                .setDuration(280)
                .setInterpolator(new OvershootInterpolator(1.18f))
                .start();
    }

    private void showUniversalLinkDownloaderDialog(String prefillUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("New Download");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(12));
        root.addView(title);

        final EditText inputLink = createStyledEditText("https://...", prefillUrl, false);
        final EditText inputName = createStyledEditText("File name (optional)", "", false);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (!liveDetectedStreamsMap.isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(liveDetectedStreamsMap.entrySet());
            final Map.Entry<String, String> latest = entries.get(entries.size() - 1);
            final String detectedStreamUrl = latest.getKey();
            final String detectedTitle = latest.getValue();

            View instantBtn = createCompactSettingRow("⚡  " + detectedTitle, "Download Now", "#00F5A0", v -> {
                dismissPopupWithAnimation(dialog, root, () -> {
                    String referer = (webView != null && webView.getUrl() != null) ? webView.getUrl() : detectedStreamUrl;
                    boolean isHls = detectedStreamUrl.toLowerCase(Locale.ROOT).contains(".m3u8");
                    launchBestDownloadEngine(detectedStreamUrl, referer, detectedTitle, isHls);
                });
            });
            root.addView(instantBtn);
        }

        root.addView(inputLink);
        root.addView(inputName);

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setWeightSum(2f);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qrParams.setMargins(0, 0, 0, dpToPx(12));
        quickRow.setLayoutParams(qrParams);

        TextView btnPasteClip = createDialogButton("📋 Paste", "#7DD3FC", "#13263B");
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p1.setMargins(0, 0, dpToPx(5), 0);
        btnPasteClip.setLayoutParams(p1);
        attachTouchBounceAnimation(btnPasteClip);
        btnPasteClip.setOnClickListener(v -> {
            try {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                    if (text != null) {
                        inputLink.setText(text.toString().trim());
                    }
                }
            } catch (Exception ignored) {}
        });

        TextView btnUseCurrentPage = createDialogButton("🌐 Current Page", "#6EE7B7", "#122B27");
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p2.setMargins(dpToPx(5), 0, 0, 0);
        btnUseCurrentPage.setLayoutParams(p2);
        attachTouchBounceAnimation(btnUseCurrentPage);
        btnUseCurrentPage.setOnClickListener(v -> {
            if (webView != null && webView.getUrl() != null && webView.getUrl().startsWith("http")) {
                inputLink.setText(webView.getUrl());
                if (webView.getTitle() != null && !webView.getTitle().isEmpty()) {
                    inputName.setText(webView.getTitle().replaceAll("[^a-zA-Z0-9._\\- ()]", "_").trim() + ".mp4");
                }
            }
        });

        quickRow.addView(btnPasteClip);
        quickRow.addView(btnUseCurrentPage);
        root.addView(quickRow);

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.END);

        TextView btnCancel = createDialogButton("Cancel", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnCancel);
        btnCancel.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));

        final TextView btnStartDl = createDialogButton("Download", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams dlParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlParams.setMargins(dpToPx(8), 0, 0, 0);
        btnStartDl.setLayoutParams(dlParams);
        attachTouchBounceAnimation(btnStartDl);

        btnStartDl.setOnClickListener(v -> {
            String rawUrl = inputLink.getText().toString().trim();
            String customTitle = inputName.getText().toString().trim();
            if (rawUrl.isEmpty()) {
                Toast.makeText(this, "Enter a link first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                rawUrl = "https://" + rawUrl;
            }

            btnStartDl.setText("Fetching...");
            btnStartDl.setEnabled(false);
            btnStartDl.setAlpha(0.65f);

            processUniversalDownloadLink(rawUrl, customTitle, dialog, btnStartDl);
        });

        bottomRow.addView(btnCancel);
        bottomRow.addView(btnStartDl);
        root.addView(bottomRow);

        showAnimatedDialog(dialog, root);
    }

    static class ExtractedMediaCandidate {
        String label;
        String url;
        boolean isHls;

        ExtractedMediaCandidate(String label, String url, boolean isHls) {
            this.label = label;
            this.url = url;
            this.isHls = isHls;
        }
    }

    private void processUniversalDownloadLink(final String inputUrl, final String userCustomTitle, final AlertDialog parentDialog, final TextView btnStart) {
        if (webView != null && webView.getUrl() != null && webView.getUrl().equalsIgnoreCase(inputUrl)) {
            extractLiveDomVideoFromActiveWebView(webView, inputUrl, userCustomTitle, parentDialog, btnStart);
            return;
        }
        analyzeAndExtractInBackground(inputUrl, userCustomTitle, parentDialog, btnStart);
    }

    private String getDeepVideoExtractionJs() {
        return "(function(){" +
                "try{" +
                "var out=[];" +
                "if(window.html5player){" +
                "if(html5player.url_high)out.push('HD MP4|'+html5player.url_high);" +
                "if(html5player.url_low)out.push('SD MP4|'+html5player.url_low);" +
                "if(html5player.url_hls)out.push('HLS Stream|'+html5player.url_hls);" +
                "}" +
                "if(window.initials && window.initials.xplayerSettings && window.initials.xplayerSettings.sources){" +
                "var sObj=window.initials.xplayerSettings.sources;" +
                "var std=sObj.standard||{};" +
                "var keys=['h264','mp4','av1'];" +
                "for(var k=0;k<keys.length;k++){" +
                "var arr=std[keys[k]];" +
                "if(Array.isArray(arr)){" +
                "for(var m=0;m<arr.length;m++){" +
                "var u=arr[m].url||arr[m].fallback||'';" +
                "var q=arr[m].quality||arr[m].label||'MP4';" +
                "if(u && u.indexOf('http')===0)out.push(q+' MP4|'+u);" +
                "}" +
                "}" +
                "}" +
                "if(sObj.hls){" +
                "for(var hk in sObj.hls){" +
                "if(sObj.hls[hk] && sObj.hls[hk].url)out.push('HLS ('+hk+')|'+sObj.hls[hk].url);" +
                "}" +
                "}" +
                "}" +
                "if(window.stream_data){" +
                "var sq=['1080p','720p','480p','360p','240p','main','m3u8'];" +
                "for(var si=0;si<sq.length;si++){" +
                "var sv=window.stream_data[sq[si]];" +
                "if(Array.isArray(sv) && sv.length>0 && typeof sv[0]==='string' && sv[0].indexOf('http')===0){" +
                "out.push(sq[si].toUpperCase()+'|'+sv[0]);" +
                "}" +
                "}" +
                "}" +
                "for(var wk in window){" +
                "if(wk && wk.indexOf('flashvars_')===0 && window[wk] && Array.isArray(window[wk].mediaDefinitions)){" +
                "var defs=window[wk].mediaDefinitions;" +
                "for(var di=0;di<defs.length;di++){" +
                "var du=defs[di].videoUrl||'';" +
                "var dq=defs[di].quality||defs[di].format||'Video';" +
                "if(du && du.indexOf('http')===0)out.push(dq+'|'+du);" +
                "}" +
                "}" +
                "}" +
                "var vids=document.querySelectorAll('video, source');" +
                "for(var i=0;i<vids.length;i++){" +
                "var s=vids[i].currentSrc||vids[i].src||vids[i].getAttribute('data-src')||'';" +
                "if(s && s.indexOf('http')===0 && s.indexOf('blob:')!==0){" +
                "var lbl=vids[i].getAttribute('label')||vids[i].getAttribute('res')||'Video Stream';" +
                "out.push(lbl+'|'+s);" +
                "}" +
                "}" +
                "if(window.performance && performance.getEntriesByType){" +
                "var res=performance.getEntriesByType('resource');" +
                "for(var r=0;r<res.length;r++){" +
                "var rn=res[r].name||'';" +
                "var rl=rn.toLowerCase();" +
                "if((rl.indexOf('.mp4')!==-1||rl.indexOf('.m3u8')!==-1||rl.indexOf('/get_file/')!==-1||rl.indexOf('videoplayback')!==-1)&&rl.indexOf('preview')===-1&&rl.indexOf('thumb')===-1&&rl.indexOf('.jpg')===-1&&rl.indexOf('.png')===-1){" +
                "out.push((rl.indexOf('.m3u8')!==-1?'HLS Stream':'Direct MP4')+'|'+rn);" +
                "}" +
                "}" +
                "}" +
                "var dlLinks=document.querySelectorAll('a[href*=\".mp4\"], a[href*=\".mkv\"], a[download]');" +
                "for(var j=0;j<dlLinks.length;j++){" +
                "var h=dlLinks[j].href||'';" +
                "if(h && h.indexOf('http')===0){out.push((dlLinks[j].innerText.trim()||'Download')+'|'+h);}" +
                "}" +
                "return out.join('###');" +
                "}catch(e){return '';}" +
                "})();";
    }

    private void extractLiveDomVideoFromActiveWebView(final WebView activeWv, final String pageUrl, final String userCustomTitle, final AlertDialog parentDialog, final TextView btnStart) {
        activeWv.evaluateJavascript(getDeepVideoExtractionJs(), result -> {
            List<ExtractedMediaCandidate> list = parsePipeSeparatedCandidates(result);
            if (list.isEmpty() && !liveDetectedStreamsMap.isEmpty()) {
                List<Map.Entry<String, String>> entries = new ArrayList<>(liveDetectedStreamsMap.entrySet());
                for (int i = entries.size() - 1; i >= 0; i--) {
                    String u = entries.get(i).getKey();
                    boolean hls = u.toLowerCase(Locale.ROOT).contains(".m3u8");
                    list.add(new ExtractedMediaCandidate(guessQualityLabelFromUrl(u, hls ? "HLS Stream" : "MP4 Video"), u, hls));
                }
            }
            if (!list.isEmpty()) {
                String titleToUse = !userCustomTitle.isEmpty() ? userCustomTitle : (activeWv.getTitle() != null ? activeWv.getTitle() : "Video");
                handleExtractedCandidatesOnUi(list, pageUrl, titleToUse, parentDialog);
                return;
            }
            analyzeAndExtractInBackground(pageUrl, userCustomTitle, parentDialog, btnStart);
        });
    }

    private List<ExtractedMediaCandidate> parsePipeSeparatedCandidates(String jsResult) {
        List<ExtractedMediaCandidate> mp4List = new ArrayList<>();
        List<ExtractedMediaCandidate> hlsList = new ArrayList<>();
        if (jsResult == null) return mp4List;

        String clean = jsResult;
        if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() >= 2) {
            clean = clean.substring(1, clean.length() - 1);
        }
        clean = clean.replace("\\\"", "\"").replace("\\/", "/").trim();
        if (clean.isEmpty() || clean.equals("null")) return mp4List;

        Set<String> seen = new HashSet<>();
        String[] parts = clean.split("###");
        for (String p : parts) {
            int bar = p.indexOf('|');
            if (bar > 0) {
                String lbl = p.substring(0, bar).trim();
                String u = p.substring(bar + 1).trim();
                if (u.startsWith("http") && !isThumbnailOrPreviewVideo(u) && !seen.contains(u)) {
                    seen.add(u);
                    boolean hls = u.toLowerCase(Locale.ROOT).contains(".m3u8");
                    ExtractedMediaCandidate c = new ExtractedMediaCandidate(lbl, u, hls);
                    if (hls) hlsList.add(c);
                    else mp4List.add(c);
                }
            }
        }
        mp4List.addAll(hlsList);
        return mp4List;
    }

    private void analyzeAndExtractInBackground(final String targetUrl, final String userCustomTitle, final AlertDialog parentDialog, final TextView btnStart) {
        bgExecutor.execute(() -> {
            try {
                if (isDirectMediaFileExtension(targetUrl)) {
                    boolean isHls = targetUrl.toLowerCase(Locale.ROOT).contains(".m3u8");
                    mainHandler.post(() -> {
                        if (parentDialog != null && parentDialog.isShowing()) parentDialog.dismiss();
                        launchBestDownloadEngine(targetUrl, targetUrl, userCustomTitle, isHls);
                    });
                    return;
                }

                HttpURLConnection conn = openBrowserLikeConnectionWithRedirects(targetUrl, targetUrl, null);
                int code = conn.getResponseCode();
                String finalResolvedUrl = conn.getURL() != null ? conn.getURL().toString() : targetUrl;
                String contentType = conn.getContentType() != null ? conn.getContentType().toLowerCase(Locale.ROOT) : "";
                long contentLen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? conn.getContentLengthLong() : conn.getContentLength();
                String contentDisp = conn.getHeaderField("Content-Disposition");

                if (!contentType.contains("text/html") && (contentType.contains("video/")
                        || contentType.contains("audio/")
                        || contentType.contains("application/octet-stream")
                        || contentType.contains("application/mp4")
                        || contentType.contains("application/x-mpegurl")
                        || contentType.contains("application/vnd.apple.mpegurl")
                        || (contentDisp != null && contentDisp.toLowerCase(Locale.ROOT).contains("attachment"))
                        || contentLen > (2L * 1024L * 1024L))) {
                    conn.disconnect();
                    final boolean isHls = contentType.contains("mpegurl") || finalResolvedUrl.toLowerCase(Locale.ROOT).contains(".m3u8");
                    mainHandler.post(() -> {
                        if (parentDialog != null && parentDialog.isShowing()) parentDialog.dismiss();
                        launchBestDownloadEngine(finalResolvedUrl, targetUrl, userCustomTitle, isHls);
                    });
                    return;
                }

                InputStream in = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder htmlSb = new StringBuilder();
                if (in != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null && htmlSb.length() < 800000) {
                        htmlSb.append(line).append("\n");
                    }
                    reader.close();
                }
                conn.disconnect();

                String html = htmlSb.toString();
                String extractedPageTitle = extractHtmlTitle(html);
                final String finalTitle = !userCustomTitle.isEmpty() ? userCustomTitle : (!extractedPageTitle.isEmpty() ? extractedPageTitle : "Video");

                final List<ExtractedMediaCandidate> candidates = parseAllVideoStreamsFromHtml(html, finalResolvedUrl);

                if (!candidates.isEmpty()) {
                    mainHandler.post(() -> handleExtractedCandidatesOnUi(candidates, finalResolvedUrl, finalTitle, parentDialog));
                } else {
                    mainHandler.post(() -> runOnDemandHeadlessVideoSniffer(finalResolvedUrl, finalTitle, parentDialog, btnStart));
                }
            } catch (Exception e) {
                mainHandler.post(() -> runOnDemandHeadlessVideoSniffer(targetUrl, userCustomTitle.isEmpty() ? "Video" : userCustomTitle, parentDialog, btnStart));
            }
        });
    }

    private void runOnDemandHeadlessVideoSniffer(final String pageUrl, final String videoTitle, final AlertDialog parentDialog, final TextView btnStart) {
        final WebView snifferWv = new WebView(this);
        WebSettings s = snifferWv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(browserUserAgent);

        CookieManager.getInstance().setAcceptThirdPartyCookies(snifferWv, true);

        final List<ExtractedMediaCandidate> foundList = Collections.synchronizedList(new ArrayList<>());
        final Set<String> seenUrls = Collections.synchronizedSet(new HashSet<>());
        final boolean[] finished = {false};

        final Runnable finishSniffing = () -> {
            if (finished[0]) return;
            finished[0] = true;

            mainHandler.post(() -> {
                try {
                    snifferWv.setWebViewClient(null);
                    snifferWv.setWebChromeClient(null);
                    snifferWv.stopLoading();
                    snifferWv.destroy();
                } catch (Throwable ignored) {}

                if (!foundList.isEmpty()) {
                    handleExtractedCandidatesOnUi(foundList, pageUrl, videoTitle, parentDialog);
                } else {
                    if (btnStart != null) {
                        btnStart.setText("Download");
                        btnStart.setEnabled(true);
                        btnStart.setAlpha(1f);
                    }
                    Toast.makeText(HomeActivity.this, "Play the video first, then tap Download", Toast.LENGTH_LONG).show();
                }
            });
        };

        snifferWv.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url != null && url.startsWith("http") && !seenUrls.contains(url)) {
                seenUrls.add(url);
                foundList.add(0, new ExtractedMediaCandidate("Direct Stream", url, url.contains(".m3u8")));
                mainHandler.post(finishSniffing);
            }
        });

        snifferWv.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (url == null || finished[0]) return;
                if (isLikelyRealVideoStreamUrl(url) && !seenUrls.contains(url)) {
                    seenUrls.add(url);
                    boolean isHls = url.toLowerCase(Locale.ROOT).contains(".m3u8");
                    if (isHls) {
                        foundList.add(new ExtractedMediaCandidate("HLS Stream", url, true));
                    } else {
                        foundList.add(0, new ExtractedMediaCandidate(guessQualityLabelFromUrl(url, "MP4 Video"), url, false));
                        mainHandler.postDelayed(finishSniffing, 250);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (finished[0]) return;
                view.evaluateJavascript(getDeepVideoExtractionJs(), val -> {
                    List<ExtractedMediaCandidate> domExtracted = parsePipeSeparatedCandidates(val);
                    for (ExtractedMediaCandidate c : domExtracted) {
                        if (!seenUrls.contains(c.url)) {
                            seenUrls.add(c.url);
                            if (c.isHls) foundList.add(c);
                            else foundList.add(0, c);
                        }
                    }
                    if (!foundList.isEmpty()) {
                        mainHandler.post(finishSniffing);
                    }
                });
            }
        });

        snifferWv.loadUrl(pageUrl);
        mainHandler.postDelayed(finishSniffing, 6000);
    }

    private List<ExtractedMediaCandidate> parseAllVideoStreamsFromHtml(String html, String baseUrl) {
        List<ExtractedMediaCandidate> mp4List = new ArrayList<>();
        List<ExtractedMediaCandidate> hlsList = new ArrayList<>();
        if (html == null || html.isEmpty()) return mp4List;

        String unescaped = html
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("\\u003d", "=")
                .replace("&amp;", "&");

        Map<String, String> labeledPatterns = new LinkedHashMap<>();
        labeledPatterns.put("1080p MP4", "(?:quality_1080p|\"1080p\"|\"1080\")\\s*[:=]\\s*\\[?\\s*['\"](https?://[^'\"]+\\.mp4[^'\"]*)['\"]|setVideoUrlHigh\\s*\\(\\s*['\"](https?://[^'\"]+)['\"]\\s*\\)");
        labeledPatterns.put("720p MP4", "(?:quality_720p|\"720p\"|\"720\"|mp4_720p)\\s*[:=]\\s*\\[?\\s*['\"](https?://[^'\"]+\\.mp4[^'\"]*)['\"]");
        labeledPatterns.put("480p MP4", "(?:quality_480p|\"480p\"|\"480\"|mp4_480p)\\s*[:=]\\s*\\[?\\s*['\"](https?://[^'\"]+\\.mp4[^'\"]*)['\"]");
        labeledPatterns.put("360p MP4", "(?:quality_360p|\"360p\"|\"360\"|mp4_360p|setVideoUrlLow)\\s*(?:[:=]|\\()\\s*\\[?\\s*['\"](https?://[^'\"]+)['\"]");
        labeledPatterns.put("Direct MP4", "(?:\"contentUrl\"|\"mp4File\"|\"fallback\"|\"videoUrl\")\\s*:\\s*\"(https?://[^\"]+)\"");
        labeledPatterns.put("HLS Stream", "setVideoHLS\\s*\\(\\s*['\"](https?://[^'\"]+)['\"]\\s*\\)");

        Set<String> seen = new HashSet<>();

        for (Map.Entry<String, String> entry : labeledPatterns.entrySet()) {
            Matcher m = Pattern.compile(entry.getValue(), Pattern.CASE_INSENSITIVE).matcher(unescaped);
            while (m.find()) {
                for (int g = 1; g <= m.groupCount(); g++) {
                    String found = m.group(g);
                    if (found != null && found.startsWith("http") && !isThumbnailOrPreviewVideo(found) && !seen.contains(found)) {
                        seen.add(found);
                        boolean isHls = found.toLowerCase(Locale.ROOT).contains(".m3u8");
                        ExtractedMediaCandidate item = new ExtractedMediaCandidate(entry.getKey(), found, isHls);
                        if (isHls) hlsList.add(item);
                        else mp4List.add(item);
                    }
                }
            }
        }

        Matcher mp4Matcher = Pattern.compile("(https?://[^\"'\\s<>\\\\]+\\.mp4(?:\\?[^\"'\\s<>\\\\]*)?)", Pattern.CASE_INSENSITIVE).matcher(unescaped);
        while (mp4Matcher.find()) {
            String u = mp4Matcher.group(1);
            if (u != null && !isThumbnailOrPreviewVideo(u) && !seen.contains(u)) {
                seen.add(u);
                mp4List.add(new ExtractedMediaCandidate(guessQualityLabelFromUrl(u, "MP4 Video"), u, false));
            }
        }

        Matcher getFileMatcher = Pattern.compile("(https?://[^\"'\\s<>\\\\]+/get_file/[^\"'\\s<>\\\\]+)", Pattern.CASE_INSENSITIVE).matcher(unescaped);
        while (getFileMatcher.find()) {
            String u = getFileMatcher.group(1);
            if (u != null && !isThumbnailOrPreviewVideo(u) && !seen.contains(u)) {
                seen.add(u);
                mp4List.add(new ExtractedMediaCandidate(guessQualityLabelFromUrl(u, "Direct MP4"), u, false));
            }
        }

        Matcher m3u8Matcher = Pattern.compile("(https?://[^\"'\\s<>\\\\]+\\.m3u8(?:\\?[^\"'\\s<>\\\\]*)?)", Pattern.CASE_INSENSITIVE).matcher(unescaped);
        while (m3u8Matcher.find()) {
            String u = m3u8Matcher.group(1);
            if (u != null && !isThumbnailOrPreviewVideo(u) && !seen.contains(u)) {
                seen.add(u);
                hlsList.add(new ExtractedMediaCandidate(guessQualityLabelFromUrl(u, "HLS Stream"), u, true));
            }
        }

        mp4List.addAll(hlsList);
        return mp4List;
    }

    private boolean isLikelyRealVideoStreamUrl(String url) {
        if (url == null || !url.startsWith("http") || isThumbnailOrPreviewVideo(url)) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".m4s") || lower.contains("seg-") || lower.contains("/ping.mp4")
                || lower.endsWith(".ts") || lower.contains("doubleclick") || lower.contains("googlesyndication")
                || lower.contains("exoclick") || lower.contains("adsterra")) {
            return false;
        }
        return lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mkv")
                || lower.contains("/get_file/") || lower.contains("videoplayback") || lower.contains("mime=video");
    }

    private boolean isThumbnailOrPreviewVideo(String url) {
        if (url == null) return true;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("thumb") || lower.contains("preview") || lower.contains("trailer")
                || lower.contains("sprite") || lower.contains("teaser") || lower.contains("/banners/")
                || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.endsWith(".webp") || lower.endsWith(".vtt") || lower.endsWith(".srt")
                || lower.endsWith(".js") || lower.endsWith(".css");
    }

    private String guessQualityLabelFromUrl(String url, String fallback) {
        String lower = url.toLowerCase(Locale.ROOT);
        String type = lower.contains(".m3u8") ? "HLS" : "MP4";
        if (lower.contains("2160") || lower.contains("4k")) return "4K (" + type + ")";
        if (lower.contains("1080")) return "1080p (" + type + ")";
        if (lower.contains("720")) return "720p (" + type + ")";
        if (lower.contains("480")) return "480p (" + type + ")";
        if (lower.contains("360")) return "360p (" + type + ")";
        if (lower.contains("240")) return "240p (" + type + ")";
        return fallback;
    }

    private String extractHtmlTitle(String html) {
        if (html == null) return "";
        Matcher m = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE).matcher(html);
        if (m.find() && m.group(1) != null) {
            return m.group(1).trim().replaceAll("\\s+", " ");
        }
        return "";
    }

    private void handleExtractedCandidatesOnUi(final List<ExtractedMediaCandidate> candidates, final String refererPageUrl, final String baseTitle, final AlertDialog parentDialog) {
        if (parentDialog != null && parentDialog.isShowing()) {
            parentDialog.dismiss();
        }
        if (candidates.size() == 1) {
            ExtractedMediaCandidate only = candidates.get(0);
            launchBestDownloadEngine(only.url, refererPageUrl, baseTitle, only.isHls);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("Select Quality");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(10));
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                candidates.size() > 5 ? dpToPx(250) : ViewGroup.LayoutParams.WRAP_CONTENT));

        builder.setView(root);
        final AlertDialog qDialog = builder.create();
        if (qDialog.getWindow() != null) {
            qDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        for (int i = 0; i < Math.min(candidates.size(), 8); i++) {
            final ExtractedMediaCandidate item = candidates.get(i);
            View row = createCompactSettingRow("⬇  " + item.label, item.isHls ? "Stream" : "Direct", "#00F5A0", v -> {
                dismissPopupWithAnimation(qDialog, root, () -> launchBestDownloadEngine(item.url, refererPageUrl, baseTitle, item.isHls));
            });
            list.addView(row);
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(8), 0, 0);
        TextView btnClose = createDialogButton("Cancel", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnClose);
        btnClose.setOnClickListener(v -> dismissPopupWithAnimation(qDialog, root, null));
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        showAnimatedDialog(qDialog, root);
    }

    private void launchBestDownloadEngine(String mediaUrl, String refererUrl, String customTitle, boolean isHlsStream) {
        String safeName = buildCleanVideoFileName(mediaUrl, customTitle);
        boolean hls = isHlsStream || mediaUrl.toLowerCase(Locale.ROOT).contains(".m3u8");
        startCustomStreamOrDirectDownload(mediaUrl, refererUrl, safeName, hls);
    }

    private String buildCleanVideoFileName(String mediaUrl, String customTitle) {
        String base = (customTitle != null && !customTitle.trim().isEmpty())
                ? customTitle.trim()
                : URLUtil.guessFileName(mediaUrl, null, "video/mp4");
        try {
            base = URLDecoder.decode(base, "UTF-8");
        } catch (Exception ignored) {}
        base = base.replaceAll("[^a-zA-Z0-9._\\- ()]", "_").replaceAll("_+", "_").trim();
        if (base.length() > 65) {
            base = base.substring(0, 65).trim();
        }
        if (base.isEmpty() || base.equals("downloadfile.bin") || base.endsWith(".html") || base.endsWith(".m3u8") || base.equalsIgnoreCase("video")) {
            base = "Video_" + (System.currentTimeMillis() % 100000);
        }
        String lower = base.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".mp4") && !lower.endsWith(".mkv")
                && !lower.endsWith(".webm") && !lower.endsWith(".mp3")
                && !lower.endsWith(".zip") && !lower.endsWith(".apk")) {
            base = base + ".mp4";
        }
        return base;
    }

    private File getSafeWritableDownloadFile(String fileName) {
        File appDlDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (appDlDir == null) {
            appDlDir = getFilesDir();
        }
        if (!appDlDir.exists()) {
            appDlDir.mkdirs();
        }
        return new File(appDlDir, fileName);
    }

    private String exportCompletedFileToPublicDownloads(File sourceFile, String fileName) {
        if (sourceFile == null || !sourceFile.exists() || sourceFile.length() <= 0) return "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getApplicationContext().getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ProStream");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri itemUri = resolver.insert(collection, values);
                if (itemUri != null) {
                    OutputStream os = resolver.openOutputStream(itemUri);
                    FileInputStream fis = new FileInputStream(sourceFile);
                    if (os != null) {
                        byte[] buf = new byte[128 * 1024];
                        int r;
                        while ((r = fis.read(buf)) != -1) {
                            os.write(buf, 0, r);
                        }
                        os.flush();
                        os.close();
                    }
                    fis.close();

                    ContentValues doneValues = new ContentValues();
                    doneValues.put(MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(itemUri, doneValues, null, null);
                    return itemUri.toString();
                }
            } else {
                File pubDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ProStream");
                if (!pubDir.exists()) pubDir.mkdirs();
                File dest = new File(pubDir, fileName);
                FileInputStream fis = new FileInputStream(sourceFile);
                FileOutputStream fos = new FileOutputStream(dest);
                byte[] buf = new byte[128 * 1024];
                int r;
                while ((r = fis.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
                fos.flush();
                fos.close();
                fis.close();
                MediaScannerConnection.scanFile(getApplicationContext(), new String[]{dest.getAbsolutePath()}, new String[]{"video/mp4"}, null);
                return Uri.fromFile(dest).toString();
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private void startCustomStreamOrDirectDownload(final String mediaUrl, final String refererUrl, final String fileName, final boolean isHls) {
        final File outFile = getSafeWritableDownloadFile(fileName);
        final long taskId = System.currentTimeMillis();

        final CustomDownloadTask task = new CustomDownloadTask(taskId, fileName, mediaUrl, outFile, isHls);
        customTasksMap.put(taskId, task);
        trackedDownloadIds.add(String.valueOf(taskId));
        sharedPrefs.edit().putStringSet(PREF_DL_IDS_KEY, new HashSet<>(trackedDownloadIds)).apply();

        startBackgroundDownloadKeeperService(appContextRef);
        acquireTurboDownloadBoosterStatic(appContextRef);
        startNotificationProgressTrackerStatic(appContextRef);

        Toast.makeText(this, "Downloading: " + fileName, Toast.LENGTH_SHORT).show();

        customDlExecutor.execute(() -> {
            if (isHls) {
                executeHlsM3u8Download(task, refererUrl);
            } else {
                executeDirectHttpTurboDownload(task, refererUrl);
            }
        });
    }

    private void executeDirectHttpTurboDownload(final CustomDownloadTask task, final String refererUrl) {
        HttpURLConnection conn = null;
        try {
            conn = openBrowserLikeConnectionWithRedirects(task.url, refererUrl, "bytes=0-");
            int respCode = conn.getResponseCode();
            String cType = conn.getContentType() != null ? conn.getContentType().toLowerCase(Locale.ROOT) : "";
            String finalUrl = conn.getURL() != null ? conn.getURL().toString() : task.url;

            if (cType.contains("text/html")) {
                InputStream errIn = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                if (errIn != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(errIn));
                    String ln;
                    while ((ln = br.readLine()) != null && sb.length() < 600000) {
                        sb.append(ln).append("\n");
                    }
                    br.close();
                }
                conn.disconnect();

                List<ExtractedMediaCandidate> extracted = parseAllVideoStreamsFromHtml(sb.toString(), finalUrl);
                if (!extracted.isEmpty()) {
                    ExtractedMediaCandidate best = extracted.get(0);
                    task.url = best.url;
                    task.isHls = best.isHls;
                    if (best.isHls) {
                        executeHlsM3u8Download(task, finalUrl);
                        return;
                    }
                    conn = openBrowserLikeConnectionWithRedirects(task.url, finalUrl, "bytes=0-");
                    respCode = conn.getResponseCode();
                    finalUrl = conn.getURL() != null ? conn.getURL().toString() : task.url;
                } else {
                    cancelOrRemoveDownloadStatic(appContextRef, task.id);
                    mainHandler.post(() -> Toast.makeText(appContextRef, "Play the video first, then download", Toast.LENGTH_LONG).show());
                    return;
                }
            }

            long contentLen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? conn.getContentLengthLong() : conn.getContentLength();
            String acceptRanges = conn.getHeaderField("Accept-Ranges");
            String contentRange = conn.getHeaderField("Content-Range");
            boolean supportsRange = (respCode == HttpURLConnection.HTTP_PARTIAL)
                    || (acceptRanges != null && acceptRanges.toLowerCase(Locale.ROOT).contains("bytes"))
                    || (contentRange != null && contentRange.toLowerCase(Locale.ROOT).contains("bytes"));

            if (contentRange != null && contentRange.contains("/")) {
                try {
                    String totalPart = contentRange.substring(contentRange.lastIndexOf('/') + 1).trim();
                    long parsedTotal = Long.parseLong(totalPart);
                    if (parsedTotal > 0) contentLen = parsedTotal;
                } catch (Exception ignored) {}
            }
            task.totalBytes = contentLen;

            if (supportsRange && contentLen > (2L * 1024L * 1024L)) {
                conn.disconnect();
                conn = null;
                boolean parallelSuccess = executeParallelMultiThreadDownload(task, finalUrl, refererUrl, contentLen, 4);
                if (parallelSuccess && !task.cancelled && task.downloadedBytes.get() > 1024) {
                    finalizeSuccessfulTask(task);
                    return;
                }
                if (task.cancelled) {
                    cancelOrRemoveDownloadStatic(appContextRef, task.id);
                    return;
                }
                task.downloadedBytes.set(0);
                conn = openBrowserLikeConnectionWithRedirects(finalUrl, refererUrl, null);
            }

            streamSingleConnectionWithResume(task, conn, finalUrl, refererUrl);

            if (!task.cancelled && task.downloadedBytes.get() > 1024) {
                finalizeSuccessfulTask(task);
            } else {
                cancelOrRemoveDownloadStatic(appContextRef, task.id);
            }
        } catch (Exception e) {
            if (!task.cancelled) {
                cancelOrRemoveDownloadStatic(appContextRef, task.id);
                mainHandler.post(() -> Toast.makeText(appContextRef, "Download failed", Toast.LENGTH_SHORT).show());
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean executeParallelMultiThreadDownload(final CustomDownloadTask task, final String resolvedUrl, final String refererUrl, final long totalSize, final int numThreads) {
        try {
            RandomAccessFile rafInit = new RandomAccessFile(task.outputFile, "rw");
            rafInit.setLength(totalSize);
            rafInit.close();

            final long partSize = totalSize / numThreads;
            List<Callable<Boolean>> workers = new ArrayList<>();
            final long[] lastSpeedCheckTime = {System.currentTimeMillis()};
            final long[] lastSpeedCheckBytes = {0L};

            for (int i = 0; i < numThreads; i++) {
                final long startByte = i * partSize;
                final long endByte = (i == numThreads - 1) ? (totalSize - 1) : ((i + 1) * partSize - 1);

                workers.add(() -> {
                    for (int attempt = 0; attempt < 3; attempt++) {
                        if (task.cancelled) return false;
                        HttpURLConnection partConn = null;
                        InputStream partIn = null;
                        RandomAccessFile partRaf = null;
                        long currentOffset = startByte;
                        try {
                            partConn = openBrowserLikeConnectionWithRedirects(resolvedUrl, refererUrl, "bytes=" + currentOffset + "-" + endByte);
                            int code = partConn.getResponseCode();
                            if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
                                return false;
                            }
                            partIn = new BufferedInputStream(partConn.getInputStream(), 128 * 1024);
                            partRaf = new RandomAccessFile(task.outputFile, "rw");
                            partRaf.seek(currentOffset);

                            byte[] buf = new byte[64 * 1024];
                            int r;
                            while ((r = partIn.read(buf)) != -1) {
                                if (task.cancelled) return false;
                                partRaf.write(buf, 0, r);
                                currentOffset += r;
                                long totalNow = task.downloadedBytes.addAndGet(r);

                                long now = System.currentTimeMillis();
                                synchronized (lastSpeedCheckTime) {
                                    long elapsed = now - lastSpeedCheckTime[0];
                                    if (elapsed >= 500) {
                                        task.speedBps = ((totalNow - lastSpeedCheckBytes[0]) * 1000L) / elapsed;
                                        lastSpeedCheckTime[0] = now;
                                        lastSpeedCheckBytes[0] = totalNow;
                                    }
                                }
                            }
                            return true;
                        } catch (Exception e) {
                            if (attempt == 2) return false;
                        } finally {
                            try { if (partRaf != null) partRaf.close(); } catch (Exception ignored) {}
                            try { if (partIn != null) partIn.close(); } catch (Exception ignored) {}
                            if (partConn != null) partConn.disconnect();
                        }
                    }
                    return false;
                });
            }

            List<Future<Boolean>> results = parallelChunkPool.invokeAll(workers);
            for (Future<Boolean> f : results) {
                if (!f.get()) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void streamSingleConnectionWithResume(final CustomDownloadTask task, HttpURLConnection initialConn, final String resolvedUrl, final String refererUrl) throws Exception {
        HttpURLConnection activeConn = initialConn;
        long lastTick = System.currentTimeMillis();
        long bytesAtLastTick = 0;

        for (int attempt = 0; attempt < 3; attempt++) {
            if (task.cancelled) return;
            InputStream in = null;
            FileOutputStream fos = null;
            try {
                long alreadyDownloaded = task.downloadedBytes.get();
                if (activeConn == null) {
                    String rangeHeader = alreadyDownloaded > 0 ? ("bytes=" + alreadyDownloaded + "-") : null;
                    activeConn = openBrowserLikeConnectionWithRedirects(resolvedUrl, refererUrl, rangeHeader);
                }

                in = new BufferedInputStream(activeConn.getInputStream(), 256 * 1024);
                fos = new FileOutputStream(task.outputFile, alreadyDownloaded > 0);

                byte[] buffer = new byte[128 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (task.cancelled) return;
                    fos.write(buffer, 0, read);
                    long nowDl = task.downloadedBytes.addAndGet(read);

                    long now = System.currentTimeMillis();
                    long elapsed = now - lastTick;
                    if (elapsed >= 500) {
                        task.speedBps = ((nowDl - bytesAtLastTick) * 1000L) / elapsed;
                        lastTick = now;
                        bytesAtLastTick = nowDl;
                    }
                }
                fos.flush();
                return;
            } catch (Exception e) {
                if (attempt == 2) throw e;
            } finally {
                try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                if (activeConn != null) {
                    activeConn.disconnect();
                    activeConn = null;
                }
            }
        }
    }

    private void finalizeSuccessfulTask(final CustomDownloadTask task) {
        long finalBytes = task.downloadedBytes.get();
        task.totalBytes = finalBytes;
        task.publicUriString = exportCompletedFileToPublicDownloads(task.outputFile, task.title);
        task.status = DownloadManager.STATUS_SUCCESSFUL;
        saveCompletedHistoryRecordStatic(appContextRef, task.id, task.title, finalBytes, task.outputFile.getAbsolutePath(), task.publicUriString);
        updateSingleCompletedNotificationStatic(appContextRef, task.id);
        mainHandler.post(() -> Toast.makeText(appContextRef, "Completed: " + task.title, Toast.LENGTH_SHORT).show());
    }

    private void executeHlsM3u8Download(final CustomDownloadTask task, final String refererUrl) {
        FileOutputStream fos = null;
        try {
            String[] playlistResult = fetchTextAndFinalUrlWithCookies(task.url, refererUrl);
            String playlistText = playlistResult[0];
            String activePlaylistUrl = playlistResult[1];

            if (playlistText.contains("#EXT-X-STREAM-INF")) {
                String bestSubUrl = null;
                long bestBw = -1;
                String[] lines = playlistText.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.startsWith("#EXT-X-STREAM-INF")) {
                        long bw = 0;
                        Matcher m = Pattern.compile("BANDWIDTH=(\\d+)").matcher(line);
                        if (m.find() && m.group(1) != null) {
                            bw = Long.parseLong(m.group(1));
                        }
                        for (int k = i + 1; k < lines.length; k++) {
                            String next = lines[k].trim();
                            if (next.isEmpty()) continue;
                            if (!next.startsWith("#")) {
                                if (bw >= bestBw) {
                                    bestBw = bw;
                                    bestSubUrl = resolveRelativeUrl(activePlaylistUrl, next);
                                }
                                break;
                            }
                        }
                    }
                }
                if (bestSubUrl != null) {
                    String[] subRes = fetchTextAndFinalUrlWithCookies(bestSubUrl, refererUrl);
                    playlistText = subRes[0];
                    activePlaylistUrl = subRes[1];
                }
            }

            final List<String> segmentUrls = new ArrayList<>();
            byte[] aesKeyBytes = null;
            byte[] aesCustomIv = null;
            int mediaSequenceBase = 0;

            for (String line : playlistText.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                    try {
                        mediaSequenceBase = Integer.parseInt(trimmed.substring("#EXT-X-MEDIA-SEQUENCE:".length()).trim());
                    } catch (Exception ignored) {}
                } else if (trimmed.startsWith("#EXT-X-KEY:") && trimmed.contains("METHOD=AES-128")) {
                    Matcher keyUriMatcher = Pattern.compile("URI=\"([^\"]+)\"").matcher(trimmed);
                    if (keyUriMatcher.find() && keyUriMatcher.group(1) != null) {
                        String keyUrl = resolveRelativeUrl(activePlaylistUrl, keyUriMatcher.group(1));
                        byte[] fetchedKey = downloadSegmentBytesWithRetry(keyUrl, refererUrl, task);
                        if (fetchedKey != null && fetchedKey.length == 16) {
                            aesKeyBytes = fetchedKey;
                        }
                    }
                    Matcher ivMatcher = Pattern.compile("IV=0[xX]([0-9a-fA-F]{32})").matcher(trimmed);
                    if (ivMatcher.find() && ivMatcher.group(1) != null) {
                        String hexIv = ivMatcher.group(1);
                        aesCustomIv = new byte[16];
                        for (int b = 0; b < 16; b++) {
                            aesCustomIv[b] = (byte) Integer.parseInt(hexIv.substring(b * 2, b * 2 + 2), 16);
                        }
                    }
                } else if (trimmed.startsWith("#EXT-X-MAP:")) {
                    Matcher mapMatcher = Pattern.compile("URI=\"([^\"]+)\"").matcher(trimmed);
                    if (mapMatcher.find() && mapMatcher.group(1) != null) {
                        segmentUrls.add(resolveRelativeUrl(activePlaylistUrl, mapMatcher.group(1)));
                    }
                } else if (!trimmed.startsWith("#")) {
                    segmentUrls.add(resolveRelativeUrl(activePlaylistUrl, trimmed));
                }
            }

            if (segmentUrls.isEmpty()) {
                executeDirectHttpTurboDownload(task, refererUrl);
                return;
            }

            final byte[] finalAesKey = aesKeyBytes;
            final byte[] finalAesIv = aesCustomIv;
            final int finalSeqBase = mediaSequenceBase;

            task.totalSegments = segmentUrls.size();
            fos = new FileOutputStream(task.outputFile);
            long lastTick = System.currentTimeMillis();
            long bytesAtLastTick = 0;

            final int batchSize = 4;
            for (int baseIdx = 0; baseIdx < segmentUrls.size(); baseIdx += batchSize) {
                if (task.cancelled) break;
                int endIdx = Math.min(segmentUrls.size(), baseIdx + batchSize);
                List<Callable<byte[]>> batchCallables = new ArrayList<>();

                for (int j = baseIdx; j < endIdx; j++) {
                    final int segIndex = j;
                    final String segUrl = segmentUrls.get(j);
                    batchCallables.add(() -> {
                        byte[] rawSeg = downloadSegmentBytesWithRetry(segUrl, refererUrl, task);
                        if (rawSeg != null && finalAesKey != null) {
                            return decryptAes128Segment(rawSeg, finalAesKey, finalAesIv, finalSeqBase + segIndex);
                        }
                        return rawSeg;
                    });
                }

                List<Future<byte[]>> batchFutures = parallelChunkPool.invokeAll(batchCallables);
                for (Future<byte[]> fut : batchFutures) {
                    if (task.cancelled) break;
                    byte[] segBytes = fut.get();
                    if (segBytes != null && segBytes.length > 0) {
                        fos.write(segBytes);
                        long totalNow = task.downloadedBytes.addAndGet(segBytes.length);

                        long now = System.currentTimeMillis();
                        if (now - lastTick >= 500) {
                            task.speedBps = ((totalNow - bytesAtLastTick) * 1000L) / (now - lastTick);
                            lastTick = now;
                            bytesAtLastTick = totalNow;
                        }
                    }
                    task.completedSegments++;
                    if (task.completedSegments > 0) {
                        task.totalBytes = (task.downloadedBytes.get() * task.totalSegments) / task.completedSegments;
                    }
                }
            }
            fos.flush();
            fos.close();
            fos = null;

            if (!task.cancelled && task.downloadedBytes.get() > 1024) {
                finalizeSuccessfulTask(task);
            } else {
                cancelOrRemoveDownloadStatic(appContextRef, task.id);
            }
        } catch (Exception e) {
            if (!task.cancelled) {
                cancelOrRemoveDownloadStatic(appContextRef, task.id);
                mainHandler.post(() -> Toast.makeText(appContextRef, "Stream download failed", Toast.LENGTH_SHORT).show());
            }
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception ignored) {}
        }
    }

    private byte[] decryptAes128Segment(byte[] encryptedData, byte[] keyBytes, byte[] customIv, int sequenceNumber) {
        try {
            byte[] ivBytes = new byte[16];
            if (customIv != null && customIv.length == 16) {
                System.arraycopy(customIv, 0, ivBytes, 0, 16);
            } else {
                ivBytes[12] = (byte) ((sequenceNumber >> 24) & 0xFF);
                ivBytes[13] = (byte) ((sequenceNumber >> 16) & 0xFF);
                ivBytes[14] = (byte) ((sequenceNumber >> 8) & 0xFF);
                ivBytes[15] = (byte) (sequenceNumber & 0xFF);
            }
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            return encryptedData;
        }
    }

    private byte[] downloadSegmentBytesWithRetry(String segUrl, String refererUrl, CustomDownloadTask task) {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (task.cancelled) return null;
            HttpURLConnection segConn = null;
            InputStream segIn = null;
            try {
                segConn = openBrowserLikeConnectionWithRedirects(segUrl, refererUrl, null);
                segIn = new BufferedInputStream(segConn.getInputStream(), 64 * 1024);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(128 * 1024);
                byte[] buf = new byte[32 * 1024];
                int r;
                while ((r = segIn.read(buf)) != -1) {
                    if (task.cancelled) return null;
                    baos.write(buf, 0, r);
                }
                return baos.toByteArray();
            } catch (Exception e) {
                if (attempt == 2) return null;
            } finally {
                try { if (segIn != null) segIn.close(); } catch (Exception ignored) {}
                if (segConn != null) segConn.disconnect();
            }
        }
        return null;
    }

    private String[] fetchTextAndFinalUrlWithCookies(String urlStr, String referer) throws Exception {
        HttpURLConnection conn = openBrowserLikeConnectionWithRedirects(urlStr, referer, null);
        String finalUrl = conn.getURL() != null ? conn.getURL().toString() : urlStr;
        InputStream in = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        conn.disconnect();
        return new String[]{sb.toString(), finalUrl};
    }

    private HttpURLConnection openBrowserLikeConnectionWithRedirects(String targetUrl, String refererUrl, String rangeHeader) throws Exception {
        String currentUrl = targetUrl;
        for (int hop = 0; hop < 8; hop++) {
            URL u = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", browserUserAgent);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Connection", "keep-alive");

            if (rangeHeader != null && !rangeHeader.isEmpty()) {
                conn.setRequestProperty("Range", rangeHeader);
            }

            if (refererUrl != null && !refererUrl.isEmpty()) {
                conn.setRequestProperty("Referer", refererUrl);
                try {
                    Uri refUri = Uri.parse(refererUrl);
                    if (refUri.getScheme() != null && refUri.getHost() != null) {
                        conn.setRequestProperty("Origin", refUri.getScheme() + "://" + refUri.getHost());
                    }
                } catch (Exception ignored) {}
            }

            CookieManager cm = CookieManager.getInstance();
            String cookies = cm.getCookie(currentUrl);
            if ((cookies == null || cookies.isEmpty()) && refererUrl != null) {
                cookies = cm.getCookie(refererUrl);
            }
            if (cookies != null && !cookies.isEmpty()) {
                conn.setRequestProperty("Cookie", cookies);
            }

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER
                    || code == 307
                    || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc != null && !loc.isEmpty()) {
                    currentUrl = resolveRelativeUrl(currentUrl, loc);
                    continue;
                }
            }
            return conn;
        }
        URL finalU = new URL(currentUrl);
        return (HttpURLConnection) finalU.openConnection();
    }

    private boolean isDirectMediaFileExtension(String url) {
        if (url == null) return false;
        String clean = url.split("\\?")[0].toLowerCase(Locale.ROOT);
        return clean.endsWith(".mp4") || clean.endsWith(".mkv") || clean.endsWith(".webm")
                || clean.endsWith(".mov") || clean.endsWith(".avi") || clean.endsWith(".m3u8")
                || clean.endsWith(".mp3") || clean.endsWith(".zip") || clean.endsWith(".apk");
    }

    private void showMediaDownloadsDialog() {
        startNotificationProgressTrackerStatic(appContextRef);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dpToPx(12));

        TextView title = new TextView(this);
        title.setText("Downloads");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(title);

        TextView btnClearDone = new TextView(this);
        btnClearDone.setText("Clear");
        btnClearDone.setTextColor(Color.parseColor("#94A3B8"));
        btnClearDone.setTextSize(12f);
        btnClearDone.setTypeface(modernFont, Typeface.BOLD);
        btnClearDone.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        attachTouchBounceAnimation(btnClearDone);
        headerRow.addView(btnClearDone);

        root.addView(headerRow);

        ScrollView scroll = new ScrollView(this);
        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnClearDone.setOnClickListener(v -> {
            Iterator<Map.Entry<Long, CustomDownloadTask>> it = customTasksMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, CustomDownloadTask> entry = it.next();
                if (entry.getValue().status != DownloadManager.STATUS_RUNNING) {
                    it.remove();
                }
            }
            sharedPrefs.edit().putString(PREF_DL_HISTORY_KEY, "[]").apply();
            populateLiveDownloadsList(listContainer, dialog);
        });

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(12), 0, 0);

        TextView btnNewDl = createDialogButton("+ New Download", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams ndParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ndParams.setMargins(0, 0, dpToPx(8), 0);
        btnNewDl.setLayoutParams(ndParams);
        attachTouchBounceAnimation(btnNewDl);
        btnNewDl.setOnClickListener(v -> {
            dismissPopupWithAnimation(dialog, root, () -> showUniversalLinkDownloaderDialog(""));
        });

        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnClose);
        btnClose.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));

        bottomRow.addView(btnNewDl);
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        mediaLiveRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                populateLiveDownloadsList(listContainer, dialog);
                mainHandler.postDelayed(this, 1000);
            }
        };

        dialog.setOnDismissListener(d -> {
            if (mediaLiveRefreshRunnable != null) {
                mainHandler.removeCallbacks(mediaLiveRefreshRunnable);
            }
        });

        populateLiveDownloadsList(listContainer, dialog);
        showAnimatedDialog(dialog, root);
        mainHandler.postDelayed(mediaLiveRefreshRunnable, 1000);
    }

    private void populateLiveDownloadsList(LinearLayout container, final AlertDialog parentDialog) {
        container.removeAllViews();
        int itemCount = 0;
        Set<Long> renderedIds = new HashSet<>();

        for (final CustomDownloadTask task : customTasksMap.values()) {
            if (task.cancelled || task.status == DownloadManager.STATUS_FAILED) continue;
            renderedIds.add(task.id);
            itemCount++;
            int pct = task.getPercent();
            long dl = task.downloadedBytes.get();
            String speedStr = task.speedBps > 0 ? ("  •  " + formatSpeedStatic(task.speedBps)) : "";
            String statusText;
            String badgeColor;
            boolean isRunning = (task.status == DownloadManager.STATUS_RUNNING);

            if (isRunning) {
                String sz = task.totalBytes > 0
                        ? (formatBytesStatic(dl) + " / " + formatBytesStatic(task.totalBytes))
                        : formatBytesStatic(dl);
                statusText = pct + "%  •  " + sz + speedStr;
                badgeColor = "#00E5FF";
            } else {
                pct = 100;
                statusText = formatBytesStatic(dl) + "  •  Completed";
                badgeColor = "#00F5A0";
            }

            addMediaDownloadCardRow(container, task.id, task.title, statusText, badgeColor, pct, isRunning, v -> {
                if (!isRunning) {
                    parentDialog.dismiss();
                    openDownloadedVideoItem(task.publicUriString, task.outputFile != null ? task.outputFile.getAbsolutePath() : "");
                }
            }, v -> {
                cancelOrRemoveDownloadStatic(appContextRef, task.id);
                populateLiveDownloadsList(container, parentDialog);
            });
        }

        try {
            String rawHist = sharedPrefs.getString(PREF_DL_HISTORY_KEY, "[]");
            JSONArray histArr = new JSONArray(rawHist);
            for (int i = 0; i < histArr.length() && itemCount < 20; i++) {
                JSONObject o = histArr.optJSONObject(i);
                if (o == null) continue;
                final long hid = o.optLong("id", -1L);
                if (hid == -1L || renderedIds.contains(hid)) continue;

                final String hTitle = o.optString("title", "Video.mp4");
                final long hSize = o.optLong("size", 0L);
                final String hPath = o.optString("path", "");
                final String hUri = o.optString("uri", "");

                if (!hPath.isEmpty() && !(new File(hPath).exists()) && hUri.isEmpty()) {
                    continue;
                }

                renderedIds.add(hid);
                itemCount++;
                String statusText = formatBytesStatic(hSize) + "  •  Completed";
                addMediaDownloadCardRow(container, hid, hTitle, statusText, "#00F5A0", 100, false, v -> {
                    parentDialog.dismiss();
                    openDownloadedVideoItem(hUri, hPath);
                }, v -> {
                    cancelOrRemoveDownloadStatic(appContextRef, hid);
                    populateLiveDownloadsList(container, parentDialog);
                });
            }
        } catch (Exception ignored) {}

        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null && !trackedDownloadIds.isEmpty()) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterByStatus(
                        DownloadManager.STATUS_RUNNING
                                | DownloadManager.STATUS_PENDING
                                | DownloadManager.STATUS_PAUSED
                                | DownloadManager.STATUS_SUCCESSFUL);

                Cursor c = dm.query(query);
                if (c != null) {
                    int idIdx = c.getColumnIndex(DownloadManager.COLUMN_ID);
                    int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
                    int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int soFarIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int uriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                    while (c.moveToNext() && itemCount < 20) {
                        final long id = idIdx >= 0 ? c.getLong(idIdx) : -1L;
                        if (!trackedDownloadIds.contains(String.valueOf(id)) || renderedIds.contains(id)) {
                            continue;
                        }

                        final String fileTitle = (titleIdx >= 0 && c.getString(titleIdx) != null)
                                ? c.getString(titleIdx) : "Video.mp4";
                        final int status = statusIdx >= 0 ? c.getInt(statusIdx) : 0;
                        long soFar = soFarIdx >= 0 ? c.getLong(soFarIdx) : 0L;
                        long total = totalIdx >= 0 ? c.getLong(totalIdx) : -1L;
                        final String localUri = uriIdx >= 0 ? c.getString(uriIdx) : "";

                        renderedIds.add(id);
                        itemCount++;
                        int percent = (total > 0) ? (int) ((soFar * 100L) / total) : 0;
                        long speedBps = currentSpeedMap.containsKey(id) ? currentSpeedMap.get(id) : 0L;
                        String speedLabel = speedBps > 0 ? ("  •  " + formatSpeedStatic(speedBps)) : "";
                        boolean isRunning = (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING);

                        String statusText;
                        String badgeColor;
                        if (isRunning) {
                            statusText = (total > 0)
                                    ? (percent + "%  •  " + formatBytesStatic(soFar) + " / " + formatBytesStatic(total) + speedLabel)
                                    : (formatBytesStatic(soFar) + speedLabel);
                            badgeColor = "#00E5FF";
                        } else {
                            percent = 100;
                            statusText = formatBytesStatic(total > 0 ? total : soFar) + "  •  Completed";
                            badgeColor = "#00F5A0";
                        }

                        addMediaDownloadCardRow(container, id, fileTitle, statusText, badgeColor, percent, isRunning, v -> {
                            if (!isRunning) {
                                parentDialog.dismiss();
                                openDownloadedVideoItem(localUri, "");
                            }
                        }, v -> {
                            cancelOrRemoveDownloadStatic(appContextRef, id);
                            populateLiveDownloadsList(container, parentDialog);
                        });
                    }
                    c.close();
                }
            }
        } catch (Exception ignored) {}

        if (itemCount == 0) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No downloads yet");
            emptyTv.setTextColor(Color.parseColor("#64748B"));
            emptyTv.setTextSize(13f);
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setPadding(dpToPx(8), dpToPx(28), dpToPx(8), dpToPx(28));
            container.addView(emptyTv);
        }
    }

    private void openDownloadedVideoItem(final String publicUriStr, final String fallbackPath) {
        File localFile = (fallbackPath != null && !fallbackPath.isEmpty()) ? new File(fallbackPath) : null;
        final boolean hasLocalFile = (localFile != null && localFile.exists() && localFile.length() > 0);
        final boolean hasContentUri = (publicUriStr != null && publicUriStr.startsWith("content://"));

        if (!hasLocalFile && !hasContentUri) {
            Toast.makeText(this, "Video file not found on storage", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final LinearLayout root = createStyledDialogCard();

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(0, 0, 0, dpToPx(10));

            TextView tvTitle = new TextView(this);
            tvTitle.setText(localFile != null ? localFile.getName() : "ProStream Player");
            tvTitle.setTextColor(Color.WHITE);
            tvTitle.setTextSize(15f);
            tvTitle.setTypeface(modernFont, Typeface.BOLD);
            tvTitle.setSingleLine(true);
            tvTitle.setEllipsize(TextUtils.TruncateAt.END);
            tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            header.addView(tvTitle);
            root.addView(header);

            FrameLayout videoContainer = new FrameLayout(this);
            LinearLayout.LayoutParams vcParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(240));
            vcParams.setMargins(0, 0, 0, dpToPx(12));
            videoContainer.setLayoutParams(vcParams);
            videoContainer.setBackgroundColor(Color.BLACK);

            final VideoView videoView = new VideoView(this);
            FrameLayout.LayoutParams vvParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
            videoView.setLayoutParams(vvParams);
            videoContainer.addView(videoView);
            root.addView(videoContainer);

            builder.setView(root);
            final AlertDialog playerDialog = builder.create();
            if (playerDialog.getWindow() != null) {
                playerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            try {
                MediaController mc = new MediaController(this);
                mc.setAnchorView(videoContainer);
                videoView.setMediaController(mc);
                if (hasLocalFile) {
                    videoView.setVideoPath(localFile.getAbsolutePath());
                } else {
                    videoView.setVideoURI(Uri.parse(publicUriStr));
                }
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(false);
                    videoView.start();
                });
                videoView.setOnErrorListener((mp, what, extra) -> {
                    playerDialog.dismiss();
                    launchExternalVideoPlayerContentUri(publicUriStr, fallbackPath);
                    return true;
                });
            } catch (Exception e) {
                launchExternalVideoPlayerContentUri(publicUriStr, fallbackPath);
                return;
            }

            LinearLayout bottomRow = new LinearLayout(this);
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.END);

            if (hasContentUri) {
                TextView btnExternal = createDialogButton("External Player", "#7DD3FC", "#13263B");
                LinearLayout.LayoutParams extParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                extParams.setMargins(0, 0, dpToPx(8), 0);
                btnExternal.setLayoutParams(extParams);
                attachTouchBounceAnimation(btnExternal);
                btnExternal.setOnClickListener(v -> {
                    try { videoView.stopPlayback(); } catch (Exception ignored) {}
                    dismissPopupWithAnimation(playerDialog, root, () -> launchExternalVideoPlayerContentUri(publicUriStr, fallbackPath));
                });
                bottomRow.addView(btnExternal);
            }

            TextView btnClose = createDialogButton("Close", "#06101E", "#00F5A0");
            attachTouchBounceAnimation(btnClose);
            btnClose.setOnClickListener(v -> {
                try { videoView.stopPlayback(); } catch (Exception ignored) {}
                dismissPopupWithAnimation(playerDialog, root, null);
            });
            bottomRow.addView(btnClose);
            root.addView(bottomRow);

            playerDialog.setOnDismissListener(d -> {
                try { videoView.stopPlayback(); } catch (Exception ignored) {}
            });

            showAnimatedDialog(playerDialog, root);
        } catch (Exception e) {
            launchExternalVideoPlayerContentUri(publicUriStr, fallbackPath);
        }
    }

    private void launchExternalVideoPlayerContentUri(String publicUriStr, String fallbackPath) {
        try {
            if (publicUriStr != null && publicUriStr.startsWith("content://")) {
                launchingInternalActivity = true;
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(Uri.parse(publicUriStr), "video/*");
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(openIntent, "Play Video"));
                return;
            }
            if (fallbackPath != null && !fallbackPath.isEmpty()) {
                File f = new File(fallbackPath);
                if (f.exists()) {
                    String exportedUri = exportCompletedFileToPublicDownloads(f, f.getName());
                    if (exportedUri != null && exportedUri.startsWith("content://")) {
                        launchingInternalActivity = true;
                        Intent openIntent = new Intent(Intent.ACTION_VIEW);
                        openIntent.setDataAndType(Uri.parse(exportedUri), "video/*");
                        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(openIntent, "Play Video"));
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
        Toast.makeText(this, "Saved in Downloads/ProStream folder", Toast.LENGTH_LONG).show();
    }

    private void addMediaDownloadCardRow(LinearLayout container, long dlId, String fileTitle, String statusText, String badgeColor, int percent, boolean isRunning, View.OnClickListener clickListener, View.OnClickListener removeListener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cp);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(14));
        bg.setColor(Color.parseColor("#1C202E"));
        bg.setStroke(dpToPx(1), Color.parseColor(isRunning ? "#00E5FF" : "#2B3042"));
        card.setBackground(bg);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(fileTitle);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(13f);
        tvName.setTypeface(modernFont, Typeface.BOLD);
        tvName.setSingleLine(true);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(tvName);

        TextView btnRemove = new TextView(this);
        btnRemove.setText("✕");
        btnRemove.setTextColor(Color.parseColor("#94A3B8"));
        btnRemove.setTextSize(12f);
        btnRemove.setPadding(dpToPx(8), dpToPx(2), dpToPx(4), dpToPx(2));
        btnRemove.setOnClickListener(removeListener);
        topRow.addView(btnRemove);

        card.addView(topRow);

        TextView tvStat = new TextView(this);
        tvStat.setText(statusText);
        tvStat.setTextColor(Color.parseColor(badgeColor));
        tvStat.setTextSize(11f);
        tvStat.setPadding(0, dpToPx(3), 0, dpToPx(6));
        card.addView(tvStat);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(Math.max(2, percent));
        pb.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(5)));
        card.addView(pb);

        attachTouchBounceAnimation(card);
        card.setOnClickListener(clickListener);
        container.addView(card);
    }

    private void startProDownload(String url, String userAgent, String contentDisposition, String mimeType, String customFileName) {
        if (url == null || url.isEmpty()) return;

        String lowerUrl = url.toLowerCase(Locale.ROOT);
        if (lowerUrl.contains(".m4s") || lowerUrl.contains("seg-") || lowerUrl.contains("/ping.mp4")
                || lowerUrl.endsWith(".ts")) {
            return;
        }
        if (lowerUrl.startsWith("blob:")) {
            if (!liveDetectedStreamsMap.isEmpty()) {
                List<Map.Entry<String, String>> entries = new ArrayList<>(liveDetectedStreamsMap.entrySet());
                Map.Entry<String, String> latest = entries.get(entries.size() - 1);
                String detectedUrl = latest.getKey();
                String detectedTitle = latest.getValue();
                String ref = (webView != null && webView.getUrl() != null) ? webView.getUrl() : detectedUrl;
                launchBestDownloadEngine(detectedUrl, ref, detectedTitle, detectedUrl.toLowerCase(Locale.ROOT).contains(".m3u8"));
                return;
            }
            if (webView != null && webView.getUrl() != null) {
                showUniversalLinkDownloaderDialog(webView.getUrl());
                return;
            }
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return;
        }

        String referer = (webView != null && webView.getUrl() != null) ? webView.getUrl() : url;
        String guessedName = (customFileName != null && !customFileName.isEmpty())
                ? customFileName
                : URLUtil.guessFileName(url, contentDisposition, mimeType);
        String cleanName = buildCleanVideoFileName(url, guessedName);
        boolean isHls = lowerUrl.contains(".m3u8") || (mimeType != null && mimeType.toLowerCase(Locale.ROOT).contains("mpegurl"));

        startCustomStreamOrDirectDownload(url, referer, cleanName, isHls);
    }

    private void applyWebViewDarkMode(WebView targetView, boolean enableDark) {
        if (targetView == null) return;
        String title = targetView.getTitle() != null ? targetView.getTitle().toLowerCase(Locale.ROOT) : "";
        if (title.contains("just a moment") || title.contains("attention required")) {
            return;
        }

        String darkJs = enableDark
                ? "(function(){" +
                  "var t=(document.title||'').toLowerCase();" +
                  "if(t.indexOf('just a moment')!==-1||t.indexOf('attention required')!==-1||document.getElementById('cf-wrapper')||document.getElementById('challenge-running')||document.querySelector('iframe[src*=\"cloudflare\"], iframe[src*=\"turnstile\"]')){" +
                  "var old=document.getElementById('ps-dark-style');if(old)old.remove();return;" +
                  "}" +
                  "var st=document.getElementById('ps-dark-style');" +
                  "if(!st){st=document.createElement('style');st.id='ps-dark-style';" +
                  "st.innerHTML='html{background:#0d1117 !important;filter:invert(0.92) hue-rotate(180deg) !important;} " +
                  "img,video,iframe,canvas,picture,[style*=\"background-image\"]{filter:invert(1) hue-rotate(180deg) !important;}';" +
                  "if(document.head)document.head.appendChild(st);else document.documentElement.appendChild(st);}" +
                  "})();"
                : "(function(){var st=document.getElementById('ps-dark-style');if(st)st.remove();})();";
        targetView.evaluateJavascript(darkJs, null);
    }

    private void showSiteManagerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("Sites");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(12));
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                siteList.size() > 4 ? dpToPx(250) : ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(scroll, scrollParams);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        for (int i = 0; i < siteList.size(); i++) {
            final int idx = i;
            final SiteItem s = siteList.get(i);
            int cInt = parseColorSafe(s.color, appColor);

            LinearLayout row = new LinearLayout(this);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dpToPx(8));
            row.setLayoutParams(rowParams);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(dpToPx(14));
            rowBg.setColor(Color.parseColor("#1C202E"));
            rowBg.setStroke(dpToPx(1), Color.parseColor("#2B3042"));
            row.setBackground(rowBg);

            ImageView icon = new ImageView(this);
            icon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)));
            loadSiteOgPhotoAsync(s, icon, cInt);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            colParams.setMargins(dpToPx(10), 0, dpToPx(8), 0);
            col.setLayoutParams(colParams);

            TextView tvN = new TextView(this);
            tvN.setText(s.name);
            tvN.setTextColor(Color.WHITE);
            tvN.setTextSize(14f);
            tvN.setTypeface(modernFont, Typeface.BOLD);
            tvN.setSingleLine(true);

            TextView tvU = new TextView(this);
            tvU.setText(s.url.replaceFirst("^https?://", "").replaceAll("/$", ""));
            tvU.setTextColor(Color.parseColor("#94A3B8"));
            tvU.setTextSize(11f);
            tvU.setSingleLine(true);

            col.addView(tvN);
            col.addView(tvU);

            TextView editBtn = new TextView(this);
            editBtn.setText("Edit");
            editBtn.setTextColor(Color.parseColor("#00F5A0"));
            editBtn.setTextSize(12f);
            editBtn.setTypeface(modernFont, Typeface.BOLD);

            row.addView(icon);
            row.addView(col);
            row.addView(editBtn);

            attachTouchBounceAnimation(row);
            row.setOnClickListener(v -> {
                dismissPopupWithAnimation(dialog, root, () -> showAddOrEditSiteDialog(idx));
            });
            listContainer.addView(row);
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(12), 0, 0);

        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnClose);
        btnClose.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));

        TextView btnAddNew = createDialogButton("+ Add Site", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addParams.setMargins(dpToPx(8), 0, 0, 0);
        btnAddNew.setLayoutParams(addParams);
        attachTouchBounceAnimation(btnAddNew);
        btnAddNew.setOnClickListener(v -> {
            dismissPopupWithAnimation(dialog, root, () -> showAddOrEditSiteDialog(-1));
        });

        bottomRow.addView(btnClose);
        bottomRow.addView(btnAddNew);
        root.addView(bottomRow);

        showAnimatedDialog(dialog, root);
    }

    private void showAddOrEditSiteDialog(final int editIndex) {
        final boolean isNew = (editIndex < 0 || editIndex >= siteList.size());
        final SiteItem existing = isNew ? new SiteItem("", "", "#00F5A0", "") : siteList.get(editIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText(isNew ? "Add Site" : "Edit Site");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(12));
        root.addView(title);

        final EditText inputName = createStyledEditText("Name", existing.name, false);
        final EditText inputUrl = createStyledEditText("https://...", existing.url, false);
        root.addView(inputName);
        root.addView(inputUrl);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        btnRow.setPadding(0, dpToPx(8), 0, 0);

        if (!isNew && siteList.size() > 1) {
            TextView btnDelete = createDialogButton("Delete", "#FDA4AF", "#3B1824");
            LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            delParams.setMargins(0, 0, dpToPx(8), 0);
            btnDelete.setLayoutParams(delParams);
            attachTouchBounceAnimation(btnDelete);
            btnDelete.setOnClickListener(v -> {
                SiteItem removed = siteList.remove(editIndex);
                WebView oldWv = webViewPool.remove(removed.url);
                if (oldWv != null && webViewHolder != null) {
                    webViewHolder.removeView(oldWv);
                    oldWv.destroy();
                }
                saveSitesToPrefs();
                dismissPopupWithAnimation(dialog, root, () -> switchToSiteInstant(siteList.get(0), false));
            });
            btnRow.addView(btnDelete);
        }

        TextView btnCancel = createDialogButton("Cancel", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnCancel);
        btnCancel.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));

        TextView btnSave = createDialogButton("Save", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(dpToPx(8), 0, 0, 0);
        btnSave.setLayoutParams(saveParams);
        attachTouchBounceAnimation(btnSave);
        btnSave.setOnClickListener(v -> {
            String n = inputName.getText().toString().trim();
            String u = inputUrl.getText().toString().trim();
            if (n.isEmpty() || u.isEmpty()) {
                return;
            }
            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                u = "https://" + u;
            }
            String ogToKeep = (!isNew && existing.url.equalsIgnoreCase(u)) ? existing.ogImage : "";
            final SiteItem updated = new SiteItem(n, u, "#00F5A0", ogToKeep);
            if (isNew) {
                siteList.add(updated);
            } else {
                if (!existing.url.equalsIgnoreCase(u)) {
                    WebView oldWv = webViewPool.remove(existing.url);
                    if (oldWv != null && webViewHolder != null) {
                        webViewHolder.removeView(oldWv);
                        oldWv.destroy();
                    }
                }
                siteList.set(editIndex, updated);
            }
            saveSitesToPrefs();
            preloadSiteOgBitmapInBackground(updated);
            dismissPopupWithAnimation(dialog, root, () -> switchToSiteInstant(updated, false));
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnSave);
        root.addView(btnRow);

        showAnimatedDialog(dialog, root);
    }

    private WebView createConfiguredWebView(final SiteItem ownerSite) {
        final WebView wv = new WebView(this);
        wv.setBackgroundColor(Color.parseColor("#080B12"));
        wv.setKeepScreenOn(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
            } catch (Exception ignored) {}
        }

        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setGeolocationEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(false);
        }

        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        settings.setUserAgentString(browserUserAgent);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(wv, true);

        wv.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                startProDownload(url, userAgent, contentDisposition, mimetype, null);
            }
        });

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (isLikelyRealVideoStreamUrl(url)) {
                    String title = (view != null && view.getTitle() != null && !view.getTitle().isEmpty())
                            ? view.getTitle() : "Detected Video";
                    synchronized (liveDetectedStreamsMap) {
                        liveDetectedStreamsMap.put(url, title);
                    }
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String reqUrl = request.getUrl().toString();
                    if (isBlockedAdUrl(reqUrl)) {
                        return new WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
                    }
                    if (isLikelyRealVideoStreamUrl(reqUrl)) {
                        synchronized (liveDetectedStreamsMap) {
                            if (!liveDetectedStreamsMap.containsKey(reqUrl)) {
                                liveDetectedStreamsMap.put(reqUrl, "Detected Video");
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                String url = request.getUrl().toString();

                if (isImageOrFaviconUrl(url) || isBlockedAdUrl(url)) {
                    return true;
                }

                if (url.startsWith("blob:")) {
                    startProDownload(url, browserUserAgent, null, "video/mp4", null);
                    return true;
                }

                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
                    if (isDirectMediaFileExtension(url)) {
                        startProDownload(url, browserUserAgent, null, "video/mp4", null);
                        return true;
                    }
                    return false;
                }

                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null && !fallbackUrl.isEmpty() && !isBlockedAdUrl(fallbackUrl)) {
                                view.loadUrl(fallbackUrl);
                                return true;
                            }
                        }
                    } catch (Exception ignored) {}
                    return true;
                }

                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (url != null && !url.startsWith("about:blank")) {
                    if (view == webView) {
                        hideModernOfflineScreen();
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request == null || !request.isForMainFrame()) {
                    return;
                }

                int errCode = 0;
                String errDesc = "";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && error != null) {
                    errCode = error.getErrorCode();
                    if (error.getDescription() != null) {
                        errDesc = error.getDescription().toString();
                    }
                }

                if (errDesc.contains("ERR_ABORTED")
                        || errDesc.contains("ERR_BLOCKED")
                        || errDesc.contains("ERR_UNKNOWN_URL_SCHEME")
                        || errCode == WebViewClient.ERROR_UNSUPPORTED_SCHEME) {
                    return;
                }

                if (!isNetworkAvailable() || errDesc.contains("ERR_INTERNET_DISCONNECTED")) {
                    String failingUrl = request.getUrl() != null ? request.getUrl().toString() : "";
                    if (failingUrl.startsWith("http")) {
                        lastFailedUrlMap.put(view, failingUrl);
                    }
                    view.stopLoading();
                    if (view == webView) {
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        showModernOfflineScreen(failingUrl);
                    }
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (handler != null) {
                    handler.proceed();
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                try {
                    if (view != null) {
                        String keyToRemove = null;
                        for (Map.Entry<String, WebView> entry : webViewPool.entrySet()) {
                            if (entry.getValue() == view) {
                                keyToRemove = entry.getKey();
                                break;
                            }
                        }
                        if (keyToRemove != null) {
                            webViewPool.remove(keyToRemove);
                        }
                        if (webViewHolder != null) {
                            webViewHolder.removeView(view);
                        }
                        view.destroy();
                    }
                    if (currentSite != null) {
                        mainHandler.postDelayed(() -> switchToSiteInstantInternal(currentSite, true, false), 150);
                    }
                } catch (Throwable ignored) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (swipeRefresh != null && view == webView) {
                    swipeRefresh.setRefreshing(false);
                }
                if (progressBar != null && view == webView) {
                    progressBar.setVisibility(View.GONE);
                }

                if (clearHistoryOnLoadSet.remove(view)) {
                    try {
                        view.clearHistory();
                    } catch (Exception ignored) {}
                }

                if (url == null || url.startsWith("about:blank")) {
                    return;
                }

                CookieManager.getInstance().flush();

                String pageTitleLower = view.getTitle() != null ? view.getTitle().toLowerCase(Locale.ROOT) : "";
                String urlLower = url.toLowerCase(Locale.ROOT);
                if (pageTitleLower.contains("just a moment")
                        || pageTitleLower.contains("attention required")
                        || urlLower.contains("challenges.cloudflare.com")
                        || urlLower.contains("/cdn-cgi/")) {
                    return;
                }

                if (darkModeEnabled) {
                    applyWebViewDarkMode(view, true);
                }

                if (ownerSite != null && isRootHomepageUrl(url, ownerSite.url)) {
                    String ogExtractScript = "(function(){" +
                            "var m=document.querySelector('meta[property=\"og:image\"]')" +
                            "||document.querySelector('meta[name=\"og:image\"]')" +
                            "||document.querySelector('meta[name=\"twitter:image\"]')" +
                            "||document.querySelector('link[rel=\"image_src\"]')" +
                            "||document.querySelector('link[rel*=\"apple-touch-icon\"]');" +
                            "if(m){var c=m.content||m.href||'';if(c)return c;}" +
                            "var lg=document.querySelector('header img, .logo img, img[src*=\"logo\"], img[alt*=\"logo\" i]');" +
                            "return lg?(lg.src||''):'';" +
                            "})();";
                    view.evaluateJavascript(ogExtractScript, val -> {
                        String res = val != null ? val.replace("\"", "").trim() : "";
                        if (!res.isEmpty() && !res.equals("null")) {
                            String absOg = resolveRelativeUrl(url, res);
                            if (absOg.startsWith("http") && !absOg.equals(ownerSite.ogImage)) {
                                ownerSite.ogImage = absOg;
                                saveSitesToPrefs();
                                preloadSiteOgBitmapInBackground(ownerSite);
                            }
                        }
                    });
                }
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    try {
                        request.grant(request.getResources());
                    } catch (Exception ignored) {}
                });
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (callback != null) {
                    callback.invoke(origin, true, true);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (HomeActivity.this.filePathCallback != null) {
                    HomeActivity.this.filePathCallback.onReceiveValue(null);
                }
                HomeActivity.this.filePathCallback = filePathCallback;
                try {
                    launchingInternalActivity = true;
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                } catch (Exception e) {
                    HomeActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                if (message != null && (message.toLowerCase(Locale.ROOT).contains("wrong referer")
                        || message.toLowerCase(Locale.ROOT).contains("adblock")
                        || message.toLowerCase(Locale.ROOT).contains("disable ad"))) {
                    if (result != null) result.confirm();
                    return true;
                }
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                if (message != null && message.toLowerCase(Locale.ROOT).contains("wrong referer")) {
                    if (result != null) result.confirm();
                    return true;
                }
                return super.onJsConfirm(view, url, message, result);
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view != webView) return;
                if (newProgress < 100) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    }
                } else {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    CookieManager.getInstance().flush();
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                mCustomViewCallback = callback;

                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (btnFloatingHub != null) btnFloatingHub.setVisibility(View.GONE);

                acquireTurboDownloadBoosterStatic(appContextRef);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                decorView.addView(mCustomView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onHideCustomView() {
                exitCustomFullscreen();
            }
        });

        return wv;
    }

    private boolean isRootHomepageUrl(String currentUrl, String siteBaseUrl) {
        if (currentUrl == null || siteBaseUrl == null) return false;
        try {
            Uri cur = Uri.parse(currentUrl);
            Uri base = Uri.parse(siteBaseUrl);
            String curHost = cur.getHost() != null ? cur.getHost().replaceFirst("^www\\.", "") : "";
            String baseHost = base.getHost() != null ? base.getHost().replaceFirst("^www\\.", "") : "";
            if (!curHost.equalsIgnoreCase(baseHost)) {
                return false;
            }
            String path = cur.getPath();
            return path == null || path.isEmpty() || "/".equals(path)
                    || "/index.html".equalsIgnoreCase(path) || "/index.php".equalsIgnoreCase(path);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isImageOrFaviconUrl(String url) {
        if (url == null || url.isEmpty()) return true;
        String lower = url.toLowerCase(Locale.ROOT);
        String pathOnly = lower.split("\\?")[0];
        return lower.contains("google.com/s2/favicons")
                || lower.contains("gstatic.com/favicon")
                || pathOnly.endsWith(".png")
                || pathOnly.endsWith(".jpg")
                || pathOnly.endsWith(".jpeg")
                || pathOnly.endsWith(".gif")
                || pathOnly.endsWith(".webp")
                || pathOnly.endsWith(".svg")
                || pathOnly.endsWith(".ico")
                || pathOnly.endsWith(".bmp");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        launchingInternalActivity = false;
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }

    private void exitCustomFullscreen() {
        if (mCustomView == null) return;
        try {
            FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
            decorView.removeView(mCustomView);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } catch (Exception ignored) {}
        mCustomView = null;
        if (mCustomViewCallback != null) {
            try {
                mCustomViewCallback.onCustomViewHidden();
            } catch (Exception ignored) {}
            mCustomViewCallback = null;
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (btnFloatingHub != null) btnFloatingHub.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!updateLiveDownloadNotificationsAndSpeedStatic(appContextRef)) {
            releaseTurboDownloadBoosterStatic();
        }
    }

    private File getPersistentSiteOgFile(SiteItem site) {
        File dir = new File(getFilesDir(), "site_og_v6");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String hostKey = site.url != null ? site.url.toLowerCase(Locale.ROOT).replaceFirst("^https?://", "").replaceAll("[^a-z0-9]", "_") : "site";
        return new File(dir, "og_" + hostKey + ".png");
    }

    private void preloadSiteOgBitmapInBackground(final SiteItem site) {
        if (site == null || site.url == null || site.url.isEmpty()) return;
        final String cacheKey = "og_v6_" + Math.abs(site.url.toLowerCase(Locale.ROOT).hashCode());
        final int fallbackColor = parseColorSafe(site.color, appColor);

        bgExecutor.execute(() -> {
            try {
                File ogFile = getPersistentSiteOgFile(site);
                if (ogFile.exists() && ogFile.length() > 100 && (site.ogImage == null || site.ogImage.isEmpty())) {
                    Bitmap diskBmp = BitmapFactory.decodeFile(ogFile.getAbsolutePath());
                    if (diskBmp != null) {
                        Bitmap rounded = makeRoundedPhotoBitmap(diskBmp, fallbackColor);
                        ogMemoryCache.put(cacheKey, rounded);
                        return;
                    }
                }

                String ogUrl = site.ogImage;
                if (ogUrl == null || ogUrl.isEmpty()) {
                    ogUrl = fetchHomepageOgImageUrl(site.url);
                    if (ogUrl != null && !ogUrl.isEmpty()) {
                        site.ogImage = ogUrl;
                        saveSitesToPrefs();
                    }
                }

                if (ogUrl != null && !ogUrl.isEmpty()) {
                    Bitmap rawBmp = downloadBitmapFromUrl(ogUrl, site.url);
                    if (rawBmp != null && rawBmp.getWidth() > 8) {
                        FileOutputStream fos = new FileOutputStream(ogFile);
                        rawBmp.compress(Bitmap.CompressFormat.PNG, 92, fos);
                        fos.close();

                        Bitmap rounded = makeRoundedPhotoBitmap(rawBmp, fallbackColor);
                        ogMemoryCache.put(cacheKey, rounded);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private void loadSiteOgPhotoAsync(final SiteItem site, final ImageView targetView, final int fallbackColor) {
        if (site == null || site.url == null || site.url.isEmpty() || targetView == null) return;
        final String cacheKey = "og_v6_" + Math.abs(site.url.toLowerCase(Locale.ROOT).hashCode());
        targetView.setTag(cacheKey);

        Bitmap memBmp = ogMemoryCache.get(cacheKey);
        if (memBmp != null) {
            targetView.setImageBitmap(memBmp);
            return;
        }

        targetView.setImageBitmap(createRoundedLetterBitmap(site.name, fallbackColor));

        if (fetchingOgUrls.contains(cacheKey)) {
            return;
        }
        fetchingOgUrls.add(cacheKey);

        bgExecutor.execute(() -> {
            try {
                File ogFile = getPersistentSiteOgFile(site);
                if (ogFile.exists() && ogFile.length() > 100) {
                    Bitmap diskBmp = BitmapFactory.decodeFile(ogFile.getAbsolutePath());
                    if (diskBmp != null) {
                        final Bitmap rounded = makeRoundedPhotoBitmap(diskBmp, fallbackColor);
                        ogMemoryCache.put(cacheKey, rounded);
                        mainHandler.post(() -> {
                            if (cacheKey.equals(targetView.getTag())) {
                                targetView.setImageBitmap(rounded);
                            }
                        });
                        return;
                    }
                }

                String imageUrl = site.ogImage;
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = fetchHomepageOgImageUrl(site.url);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        site.ogImage = imageUrl;
                        saveSitesToPrefs();
                    }
                }

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Bitmap rawBmp = downloadBitmapFromUrl(imageUrl, site.url);
                    if (rawBmp != null && rawBmp.getWidth() > 8) {
                        FileOutputStream fos = new FileOutputStream(ogFile);
                        rawBmp.compress(Bitmap.CompressFormat.PNG, 92, fos);
                        fos.close();

                        final Bitmap rounded = makeRoundedPhotoBitmap(rawBmp, fallbackColor);
                        ogMemoryCache.put(cacheKey, rounded);
                        mainHandler.post(() -> {
                            if (cacheKey.equals(targetView.getTag())) {
                                targetView.setImageBitmap(rounded);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            } finally {
                fetchingOgUrls.remove(cacheKey);
            }
        });
    }

    private String fetchHomepageOgImageUrl(String siteHomeUrl) {
        HttpURLConnection conn = null;
        try {
            conn = openBrowserLikeConnectionWithRedirects(siteHomeUrl, siteHomeUrl, null);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) return "";
            String finalBase = conn.getURL() != null ? conn.getURL().toString() : siteHomeUrl;

            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && sb.length() < 160000) {
                sb.append(line).append("\n");
                if (line.toLowerCase(Locale.ROOT).contains("</head>")) break;
            }
            reader.close();
            String headHtml = sb.toString();

            String[] patterns = {
                    "<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']",
                    "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']og:image[\"']",
                    "<meta[^>]+name=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']",
                    "<meta[^>]+name=[\"']twitter:image[\"'][^>]+content=[\"']([^\"']+)[\"']",
                    "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+name=[\"']twitter:image[\"']",
                    "<link[^>]+rel=[\"']image_src[\"'][^>]+href=[\"']([^\"']+)[\"']",
                    "<link[^>]+rel=[\"'][^\"']*apple-touch-icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']"
            };
            for (String pat : patterns) {
                Matcher m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(headHtml);
                if (m.find() && m.group(1) != null) {
                    String found = m.group(1).trim().replace("&amp;", "&");
                    if (!found.isEmpty()) {
                        return resolveRelativeUrl(finalBase, found);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return "";
    }

    private Bitmap downloadBitmapFromUrl(String imgUrl, String refererUrl) {
        HttpURLConnection conn = null;
        try {
            conn = openBrowserLikeConnectionWithRedirects(imgUrl, refererUrl, null);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            InputStream in = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(in);
            in.close();
            return bmp;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String resolveRelativeUrl(String baseUrl, String relUrl) {
        try {
            if (relUrl.startsWith("http://") || relUrl.startsWith("https://")) return relUrl;
            if (relUrl.startsWith("//")) return "https:" + relUrl;
            URL base = new URL(baseUrl);
            return new URL(base, relUrl).toString();
        } catch (Exception e) {
            return relUrl;
        }
    }

    private Bitmap makeRoundedPhotoBitmap(Bitmap src, int borderColor) {
        int size = 120;
        Bitmap scaled = Bitmap.createScaledBitmap(src, size, size, true);
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#10141F"));
        RectF rect = new RectF(0, 0, size, size);
        canvas.drawRoundRect(rect, 28f, 28f, bgPaint);

        Paint imgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        imgPaint.setShader(new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, 28f, 28f, imgPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(borderColor);
        strokePaint.setStrokeWidth(4f);
        RectF strokeRect = new RectF(2, 2, size - 2, size - 2);
        canvas.drawRoundRect(strokeRect, 26f, 26f, strokePaint);

        return out;
    }

    private void showAppLockDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("App Lock");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(10));
        root.addView(title);

        int lockType = sharedPrefs.getInt("LOCK_TYPE", 1);
        boolean useBio = sharedPrefs.getBoolean("USE_BIOMETRIC", true);
        String currentPin = sharedPrefs.getString("APP_PASSWORD", "1234");

        final RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);

        final RadioButton rbNone = new RadioButton(this);
        rbNone.setText("Off");
        rbNone.setTextColor(Color.WHITE);
        rbNone.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));

        final RadioButton rbPin = new RadioButton(this);
        rbPin.setText("PIN & Biometric");
        rbPin.setTextColor(Color.WHITE);
        rbPin.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));

        rg.addView(rbNone);
        rg.addView(rbPin);
        if (lockType == 0) rbNone.setChecked(true);
        else rbPin.setChecked(true);
        root.addView(rg);

        final EditText inputPin = createStyledEditText("PIN", currentPin, false);
        root.addView(inputPin);

        final CheckBox cbBio = new CheckBox(this);
        cbBio.setText("Fingerprint Unlock");
        cbBio.setTextColor(Color.parseColor("#CBD5E1"));
        cbBio.setChecked(useBio);
        cbBio.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        root.addView(cbBio);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        btnRow.setPadding(0, dpToPx(10), 0, 0);

        TextView btnCancel = createDialogButton("Cancel", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnCancel);
        btnCancel.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));

        TextView btnSave = createDialogButton("Save", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(dpToPx(8), 0, 0, 0);
        btnSave.setLayoutParams(saveParams);
        attachTouchBounceAnimation(btnSave);
        btnSave.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            if (rbNone.isChecked()) {
                editor.putInt("LOCK_TYPE", 0);
            } else {
                String pin = inputPin.getText().toString().trim();
                if (pin.isEmpty()) {
                    return;
                }
                editor.putInt("LOCK_TYPE", 1);
                editor.putString("APP_PASSWORD", pin);
            }
            editor.putBoolean("USE_BIOMETRIC", cbBio.isChecked());
            editor.apply();
            dismissPopupWithAnimation(dialog, root, null);
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnSave);
        root.addView(btnRow);

        showAnimatedDialog(dialog, root);
    }

    // =========================================================================
    // In-App Auto Update & Direct APK Installer + App Sharing System
    // =========================================================================
    private String getConfiguredGithubRepo() {
        String saved = sharedPrefs.getString("UPDATE_GITHUB_REPO", "").trim();
        if (!saved.isEmpty()) {
            return normalizeGithubRepoInput(saved);
        }
        try {
            int resId = getResources().getIdentifier("github_repo", "string", getPackageName());
            if (resId != 0) {
                String fromXml = getString(resId).trim();
                if (!fromXml.isEmpty() && !fromXml.contains("${{")) {
                    return normalizeGithubRepoInput(fromXml);
                }
            }
        } catch (Exception ignored) {}
        return "YasinAdnan/ProStrem";
    }

    private String normalizeGithubRepoInput(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("http://") || s.startsWith("https://")) {
            if (s.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                return s;
            }
            try {
                Uri u = Uri.parse(s);
                List<String> segs = u.getPathSegments();
                if (segs != null && segs.size() >= 2) {
                    return segs.get(0) + "/" + segs.get(1).replace(".git", "");
                }
            } catch (Exception ignored) {}
        }
        return s.replaceAll("^/+|/+$", "");
    }

    private int getInstalledBuildNumber() {
        int savedBuild = sharedPrefs.getInt("INSTALLED_BUILD_NUMBER_V6", -1);
        int xmlBuild = 1;
        try {
            int resId = getResources().getIdentifier("build_number", "string", getPackageName());
            if (resId != 0) {
                String raw = getString(resId).trim();
                Matcher m = Pattern.compile("(\\d+)").matcher(raw);
                if (m.find() && m.group(1) != null) {
                    xmlBuild = Integer.parseInt(m.group(1));
                }
            }
        } catch (Exception ignored) {}
        return Math.max(savedBuild, xmlBuild);
    }

    private String getInstalledVersionLabel() {
        try {
            int resId = getResources().getIdentifier("app_version", "string", getPackageName());
            if (resId != 0) {
                String v = getString(resId).trim();
                if (!v.isEmpty() && !v.contains("${{")) {
                    return v;
                }
            }
        } catch (Exception ignored) {}
        return "2.0." + getInstalledBuildNumber();
    }

    private void showInAppUpdateCenterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("🔄 ProStream In-App Updater");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(6));
        root.addView(title);

        final TextView tvCurrentVer = new TextView(this);
        tvCurrentVer.setText("Installed Version: v" + getInstalledVersionLabel() + " (Build #" + getInstalledBuildNumber() + ")");
        tvCurrentVer.setTextColor(Color.parseColor("#94A3B8"));
        tvCurrentVer.setTextSize(12f);
        tvCurrentVer.setPadding(0, 0, 0, dpToPx(12));
        root.addView(tvCurrentVer);

        final LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        LinearLayout.LayoutParams scParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scParams.setMargins(0, 0, 0, dpToPx(12));
        statusCard.setLayoutParams(scParams);
        GradientDrawable scBg = new GradientDrawable();
        scBg.setCornerRadius(dpToPx(14));
        scBg.setColor(Color.parseColor("#10141F"));
        scBg.setStroke(dpToPx(1), Color.parseColor("#3300E5FF"));
        statusCard.setBackground(scBg);

        final TextView tvStatusTitle = new TextView(this);
        tvStatusTitle.setText("🔍 Checking for updates...");
        tvStatusTitle.setTextColor(Color.parseColor("#00E5FF"));
        tvStatusTitle.setTextSize(13.5f);
        tvStatusTitle.setTypeface(modernFont, Typeface.BOLD);
        statusCard.addView(tvStatusTitle);

        final TextView tvStatusNotes = new TextView(this);
        tvStatusNotes.setText("Connecting to GitHub Release server...");
        tvStatusNotes.setTextColor(Color.parseColor("#CBD5E1"));
        tvStatusNotes.setTextSize(11.5f);
        tvStatusNotes.setPadding(0, dpToPx(4), 0, 0);
        statusCard.addView(tvStatusNotes);

        root.addView(statusCard);

        TextView repoLabel = new TextView(this);
        repoLabel.setText("GitHub Repo (owner/repo) or Direct APK URL:");
        repoLabel.setTextColor(Color.parseColor("#94A3B8"));
        repoLabel.setTextSize(11f);
        repoLabel.setPadding(0, 0, 0, dpToPx(4));
        root.addView(repoLabel);

        final EditText inputRepo = createStyledEditText("e.g. username/ProStrem or https://.../ProStream.apk", getConfiguredGithubRepo(), false);
        root.addView(inputRepo);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final String[] latestDownloadUrlHolder = {""};
        final int[] latestBuildNumHolder = {getInstalledBuildNumber()};

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setWeightSum(2f);
        LinearLayout.LayoutParams arParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arParams.setMargins(0, 0, 0, dpToPx(10));
        actionRow.setLayoutParams(arParams);

        final TextView btnCheckAgain = createDialogButton("🔍 Check Now", "#7DD3FC", "#13263B");
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p1.setMargins(0, 0, dpToPx(5), 0);
        btnCheckAgain.setLayoutParams(p1);
        attachTouchBounceAnimation(btnCheckAgain);

        final TextView btnInstallUpdate = createDialogButton("⚡ Update Now", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p2.setMargins(dpToPx(5), 0, 0, 0);
        btnInstallUpdate.setLayoutParams(p2);
        attachTouchBounceAnimation(btnInstallUpdate);

        actionRow.addView(btnCheckAgain);
        actionRow.addView(btnInstallUpdate);
        root.addView(actionRow);

        final Runnable triggerCheck = () -> {
            String entered = normalizeGithubRepoInput(inputRepo.getText().toString());
            if (!entered.isEmpty()) {
                sharedPrefs.edit().putString("UPDATE_GITHUB_REPO", entered).apply();
            }
            final String targetRepoOrUrl = entered.isEmpty() ? getConfiguredGithubRepo() : entered;
            tvStatusTitle.setText("🔍 Checking for updates...");
            tvStatusTitle.setTextColor(Color.parseColor("#00E5FF"));
            tvStatusNotes.setText("Source: " + targetRepoOrUrl);

            bgExecutor.execute(() -> {
                try {
                    if (targetRepoOrUrl.startsWith("http") && targetRepoOrUrl.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                        latestDownloadUrlHolder[0] = targetRepoOrUrl;
                        mainHandler.post(() -> {
                            tvStatusTitle.setText("✓ Direct APK Link Ready");
                            tvStatusTitle.setTextColor(Color.parseColor("#00F5A0"));
                            tvStatusNotes.setText("Tap 'Update Now' to download and install the latest APK directly inside ProStream.");
                        });
                        return;
                    }

                    String apiUrl = "https://api.github.com/repos/" + targetRepoOrUrl + "/releases/latest";
                    HttpURLConnection conn = openBrowserLikeConnectionWithRedirects(apiUrl, "https://github.com/" + targetRepoOrUrl, null);
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                    int code = conn.getResponseCode();

                    if (code >= 200 && code < 300) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        reader.close();
                        conn.disconnect();

                        JSONObject releaseJson = new JSONObject(sb.toString());
                        final String tagName = releaseJson.optString("tag_name", "latest");
                        final String relTitle = releaseJson.optString("name", tagName);
                        final String bodyNotes = releaseJson.optString("body", "Bug fixes and performance improvements.");
                        String apkDownloadUrl = "https://github.com/" + targetRepoOrUrl + "/releases/latest/download/ProStream.apk";
                        long apkSize = 0;

                        JSONArray assets = releaseJson.optJSONArray("assets");
                        if (assets != null) {
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.optJSONObject(i);
                                if (asset != null) {
                                    String bUrl = asset.optString("browser_download_url", "");
                                    if (bUrl.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                                        apkDownloadUrl = bUrl;
                                        apkSize = asset.optLong("size", 0);
                                        break;
                                    }
                                }
                            }
                        }

                        int remoteBuildNum = getInstalledBuildNumber();
                        Matcher m = Pattern.compile("(\\d+)").matcher(tagName);
                        if (m.find() && m.group(1) != null) {
                            remoteBuildNum = Integer.parseInt(m.group(1));
                        }

                        latestDownloadUrlHolder[0] = apkDownloadUrl;
                        latestBuildNumHolder[0] = remoteBuildNum;
                        final int finalRemoteBuild = remoteBuildNum;
                        final String sizeLabel = apkSize > 0 ? (" • " + formatBytesStatic(apkSize)) : "";

                        mainHandler.post(() -> {
                            if (finalRemoteBuild > getInstalledBuildNumber()) {
                                tvStatusTitle.setText("🔥 New Update Available: " + relTitle + sizeLabel);
                                tvStatusTitle.setTextColor(Color.parseColor("#00F5A0"));
                                tvStatusNotes.setText(bodyNotes + "\n\nTap 'Update Now' to install directly.");
                            } else {
                                tvStatusTitle.setText("✓ You have the latest version (" + tagName + sizeLabel + ")");
                                tvStatusTitle.setTextColor(Color.parseColor("#00F5A0"));
                                tvStatusNotes.setText("You can still tap 'Update Now' anytime to reinstall or force-update from the latest GitHub release.");
                            }
                        });
                    } else {
                        conn.disconnect();
                        latestDownloadUrlHolder[0] = "https://github.com/" + targetRepoOrUrl + "/releases/latest/download/ProStream.apk";
                        mainHandler.post(() -> {
                            tvStatusTitle.setText("ℹ️ Ready via Direct Release Link");
                            tvStatusTitle.setTextColor(Color.parseColor("#FDE047"));
                            tvStatusNotes.setText("Enter your GitHub repo (username/repo) above and tap 'Update Now' to fetch ProStream.apk.");
                        });
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        tvStatusTitle.setText("⚠️ Could not reach update server");
                        tvStatusTitle.setTextColor(Color.parseColor("#FDA4AF"));
                        tvStatusNotes.setText("Check your internet connection or verify your GitHub repo name above.");
                    });
                }
            });
        };

        btnCheckAgain.setOnClickListener(v -> triggerCheck.run());

        btnInstallUpdate.setOnClickListener(v -> {
            String entered = normalizeGithubRepoInput(inputRepo.getText().toString());
            if (!entered.isEmpty()) {
                sharedPrefs.edit().putString("UPDATE_GITHUB_REPO", entered).apply();
            }
            String dlUrl = latestDownloadUrlHolder[0];
            if (dlUrl == null || dlUrl.isEmpty()) {
                if (entered.startsWith("http") && entered.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                    dlUrl = entered;
                } else {
                    String repo = entered.isEmpty() ? getConfiguredGithubRepo() : entered;
                    dlUrl = "https://github.com/" + repo + "/releases/latest/download/ProStream.apk";
                }
            }
            final String finalDlUrl = dlUrl;
            final int targetBuild = latestBuildNumHolder[0];
            dismissPopupWithAnimation(dialog, root, () -> downloadAndInstallApkInApp(finalDlUrl, targetBuild));
        });

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnClose);
        btnClose.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        showAnimatedDialog(dialog, root);
        triggerCheck.run();
    }

    private void checkForAppUpdateInBackground(final boolean showToastIfUpToDate) {
        final String repoOrUrl = getConfiguredGithubRepo();
        if (repoOrUrl.isEmpty() || repoOrUrl.equalsIgnoreCase("YasinAdnan/ProStrem")) {
            return;
        }
        bgExecutor.execute(() -> {
            try {
                if (repoOrUrl.startsWith("http")) return;
                String apiUrl = "https://api.github.com/repos/" + repoOrUrl + "/releases/latest";
                HttpURLConnection conn = openBrowserLikeConnectionWithRedirects(apiUrl, "https://github.com/" + repoOrUrl, null);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    conn.disconnect();
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                conn.disconnect();

                JSONObject releaseJson = new JSONObject(sb.toString());
                final String tagName = releaseJson.optString("tag_name", "");
                final String relTitle = releaseJson.optString("name", tagName);
                final String bodyNotes = releaseJson.optString("body", "New update available!");
                String apkUrl = "https://github.com/" + repoOrUrl + "/releases/latest/download/ProStream.apk";

                JSONArray assets = releaseJson.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset != null) {
                            String bUrl = asset.optString("browser_download_url", "");
                            if (bUrl.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                                apkUrl = bUrl;
                                break;
                            }
                        }
                    }
                }

                int remoteBuild = -1;
                Matcher m = Pattern.compile("(\\d+)").matcher(tagName);
                if (m.find() && m.group(1) != null) {
                    remoteBuild = Integer.parseInt(m.group(1));
                }

                if (remoteBuild > getInstalledBuildNumber()) {
                    final String finalApkUrl = apkUrl;
                    final int finalRemoteBuild = remoteBuild;
                    mainHandler.post(() -> {
                        if (!isFinishing()) {
                            showNewUpdatePromptDialog(relTitle, tagName, bodyNotes, finalApkUrl, finalRemoteBuild);
                        }
                    });
                } else if (showToastIfUpToDate) {
                    mainHandler.post(() -> Toast.makeText(HomeActivity.this, "ProStream is up to date!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception ignored) {}
        });
    }

    private void showNewUpdatePromptDialog(String relTitle, String tagName, String notes, final String apkUrl, final int newBuildNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("🚀 Update Available!");
        title.setTextColor(Color.parseColor("#00F5A0"));
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(6));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText(relTitle + " (" + tagName + ")\n\n" + notes);
        sub.setTextColor(Color.parseColor("#E2E8F0"));
        sub.setTextSize(12.5f);
        sub.setPadding(0, 0, 0, dpToPx(14));
        root.addView(sub);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        TextView btnLater = createDialogButton("Later", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnLater);
        btnLater.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));

        TextView btnUpdateNow = createDialogButton("⚡ Update Now", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams upParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        upParams.setMargins(dpToPx(8), 0, 0, 0);
        btnUpdateNow.setLayoutParams(upParams);
        attachTouchBounceAnimation(btnUpdateNow);
        btnUpdateNow.setOnClickListener(v -> {
            dismissPopupWithAnimation(dialog, root, () -> downloadAndInstallApkInApp(apkUrl, newBuildNum));
        });

        btnRow.addView(btnLater);
        btnRow.addView(btnUpdateNow);
        root.addView(btnRow);

        showAnimatedDialog(dialog, root);
    }

    private void downloadAndInstallApkInApp(final String apkUrl, final int targetBuildNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("⚡ Updating ProStream...");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16.5f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(8));
        root.addView(title);

        final TextView tvProgInfo = new TextView(this);
        tvProgInfo.setText("Connecting to server...");
        tvProgInfo.setTextColor(Color.parseColor("#00E5FF"));
        tvProgInfo.setTextSize(12f);
        tvProgInfo.setPadding(0, 0, 0, dpToPx(8));
        root.addView(tvProgInfo);

        final ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(2);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(8));
        pbParams.setMargins(0, 0, 0, dpToPx(14));
        pb.setLayoutParams(pbParams);
        root.addView(pb);

        final boolean[] cancelled = {false};

        builder.setView(root);
        final AlertDialog progDialog = builder.create();
        progDialog.setCancelable(false);
        if (progDialog.getWindow() != null) {
            progDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        TextView btnCancel = createDialogButton("Cancel", "#FDA4AF", "#3B1824");
        attachTouchBounceAnimation(btnCancel);
        btnCancel.setOnClickListener(v -> {
            cancelled[0] = true;
            dismissPopupWithAnimation(progDialog, root, null);
        });
        bottomRow.addView(btnCancel);
        root.addView(bottomRow);

        showAnimatedDialog(progDialog, root);

        customDlExecutor.execute(() -> {
            HttpURLConnection conn = null;
            InputStream in = null;
            FileOutputStream fos = null;
            final File apkFile = getSafeWritableDownloadFile("ProStream_Update.apk");
            if (apkFile.exists()) {
                apkFile.delete();
            }

            try {
                conn = openBrowserLikeConnectionWithRedirects(apkUrl, apkUrl, null);
                int code = conn.getResponseCode();
                if (code < 200 || code >= 400) {
                    throw new Exception("HTTP " + code);
                }

                long totalLen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? conn.getContentLengthLong() : conn.getContentLength();
                in = new BufferedInputStream(conn.getInputStream(), 128 * 1024);
                fos = new FileOutputStream(apkFile);

                byte[] buf = new byte[64 * 1024];
                long downloaded = 0;
                long lastTick = System.currentTimeMillis();
                long lastBytes = 0;
                long speedBps = 0;
                int r;

                while ((r = in.read(buf)) != -1) {
                    if (cancelled[0]) {
                        fos.close();
                        in.close();
                        apkFile.delete();
                        return;
                    }
                    fos.write(buf, 0, r);
                    downloaded += r;

                    long now = System.currentTimeMillis();
                    if (now - lastTick >= 350) {
                        speedBps = ((downloaded - lastBytes) * 1000L) / Math.max(1L, now - lastTick);
                        lastTick = now;
                        lastBytes = downloaded;

                        final int pct = totalLen > 0 ? (int) Math.min(99L, (downloaded * 100L) / totalLen) : 50;
                        final String lineText = pct + "%  •  " + formatBytesStatic(downloaded)
                                + (totalLen > 0 ? (" / " + formatBytesStatic(totalLen)) : "")
                                + "  •  " + formatSpeedStatic(speedBps);
                        mainHandler.post(() -> {
                            pb.setProgress(Math.max(4, pct));
                            tvProgInfo.setText(lineText);
                        });
                    }
                }
                fos.flush();
                fos.close();
                fos = null;
                in.close();
                in = null;

                if (apkFile.length() < 100 * 1024) {
                    apkFile.delete();
                    throw new Exception("Downloaded file is not a valid APK");
                }

                if (targetBuildNum > getInstalledBuildNumber()) {
                    sharedPrefs.edit().putInt("INSTALLED_BUILD_NUMBER_V6", targetBuildNum).apply();
                }

                mainHandler.post(() -> {
                    dismissPopupWithAnimation(progDialog, root, () -> installDownloadedUpdateApk(apkFile));
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (progDialog.isShowing()) {
                        progDialog.dismiss();
                    }
                    Toast.makeText(HomeActivity.this, "Update download failed. Check repo/link in Update settings.", Toast.LENGTH_LONG).show();
                });
            } finally {
                try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void installDownloadedUpdateApk(File apkFile) {
        if (apkFile == null || !apkFile.exists()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    pendingUpdateApkFile = apkFile;
                    Toast.makeText(this, "Allow 'Install unknown apps' to finish updating ProStream", Toast.LENGTH_LONG).show();
                    launchingInternalActivity = true;
                    Intent permIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                    startActivity(permIntent);
                    return;
                }
            }

            // Android 10+ MediaStore content:// URI দিয়ে ১০০% ক্র্যাশহীনভাবে সিস্টেম প্যাকেজ ইনস্টলার ওপেন করা
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getApplicationContext().getContentResolver();
                ContentValues values = new ContentValues();
                String exportName = "ProStream_Update_" + (System.currentTimeMillis() % 10000) + ".apk";
                values.put(MediaStore.Downloads.DISPLAY_NAME, exportName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ProStream");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri apkContentUri = resolver.insert(collection, values);
                if (apkContentUri != null) {
                    OutputStream os = resolver.openOutputStream(apkContentUri);
                    FileInputStream fis = new FileInputStream(apkFile);
                    if (os != null) {
                        byte[] buf = new byte[128 * 1024];
                        int r;
                        while ((r = fis.read(buf)) != -1) {
                            os.write(buf, 0, r);
                        }
                        os.flush();
                        os.close();
                    }
                    fis.close();

                    ContentValues done = new ContentValues();
                    done.put(MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(apkContentUri, done, null, null);

                    launchingInternalActivity = true;
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(apkContentUri, "application/vnd.android.package-archive");
                    installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(installIntent);
                    return;
                }
            }

            // PackageInstaller Session fallback for Android 7-9
            PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(getPackageName());
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            OutputStream out = session.openWrite("ProStream_Update", 0, apkFile.length());
            FileInputStream fis = new FileInputStream(apkFile);
            byte[] buffer = new byte[64 * 1024];
            int c;
            while ((c = fis.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            fis.close();
            out.close();

            Intent callbackIntent = new Intent(this, HomeActivity.class);
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE)
                    : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(this, sessionId, callbackIntent, flags);
            session.commit(pi.getIntentSender());
            session.close();
        } catch (Exception e) {
            Toast.makeText(this, "Update APK saved in Downloads/ProStream folder. Tap to install.", Toast.LENGTH_LONG).show();
        }
    }

    private void showShareProStreamDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("📤 Share ProStream");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(10));
        root.addView(title);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final String repo = getConfiguredGithubRepo();
        final String ghReleaseLink = repo.startsWith("http")
                ? repo
                : ("https://github.com/" + repo + "/releases/latest/download/ProStream.apk");
        String[] parts = repo.split("/");
        final String landingUrl = (parts.length == 2)
                ? ("https://" + parts[0].toLowerCase(Locale.ROOT) + ".github.io/" + parts[1] + "/")
                : ghReleaseLink;

        root.addView(createCompactSettingRow("🌐  Share Landing Page & Link", "Share Link", "#00F5A0", v -> {
            dismissPopupWithAnimation(dialog, root, () -> {
                try {
                    String shareMsg = "⚡ ProStream — High-Speed Streaming Browser & 4x Turbo Video Downloader!\n\n"
                            + "🌐 Official Page & Tutorial:\n" + landingUrl + "\n\n"
                            + "📥 Direct APK Download:\n" + ghReleaseLink + "\n\n"
                            + "✈️ Support: " + TELEGRAM_BOT_URL;
                    launchingInternalActivity = true;
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, "ProStream Android App");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
                    startActivity(Intent.createChooser(sendIntent, "Share ProStream Link"));
                } catch (Exception ignored) {}
            });
        }));

        root.addView(createCompactSettingRow("📦  Send Installed APK File", "Direct APK", "#00E5FF", v -> {
            dismissPopupWithAnimation(dialog, root, this::shareInstalledApkFileDirectly);
        }));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(8), 0, 0);
        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnClose);
        btnClose.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        showAnimatedDialog(dialog, root);
    }

    private void shareInstalledApkFileDirectly() {
        bgExecutor.execute(() -> {
            try {
                String sourceApkPath = getApplicationInfo().sourceDir;
                File srcFile = new File(sourceApkPath);
                if (!srcFile.exists()) return;

                String exportedUri = "";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getApplicationContext().getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, "ProStream_Share_" + (System.currentTimeMillis() % 10000) + ".apk");
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ProStream");
                    values.put(MediaStore.Downloads.IS_PENDING, 1);

                    Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    Uri itemUri = resolver.insert(collection, values);
                    if (itemUri != null) {
                        OutputStream os = resolver.openOutputStream(itemUri);
                        FileInputStream fis = new FileInputStream(srcFile);
                        if (os != null) {
                            byte[] buf = new byte[128 * 1024];
                            int r;
                            while ((r = fis.read(buf)) != -1) {
                                os.write(buf, 0, r);
                            }
                            os.flush();
                            os.close();
                        }
                        fis.close();

                        ContentValues done = new ContentValues();
                        done.put(MediaStore.Downloads.IS_PENDING, 0);
                        resolver.update(itemUri, done, null, null);
                        exportedUri = itemUri.toString();
                    }
                }

                final String finalUriStr = exportedUri;
                mainHandler.post(() -> {
                    try {
                        if (!finalUriStr.isEmpty()) {
                            launchingInternalActivity = true;
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("application/vnd.android.package-archive");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(finalUriStr));
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(shareIntent, "Send ProStream.apk"));
                        } else {
                            Toast.makeText(HomeActivity.this, "Use 'Share Landing Page & Link' on this Android version", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        });
    }

    private void showAllSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(10));
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        list.addView(createCompactSettingRow("🔄  App Update (In-App)", "v" + getInstalledVersionLabel(), "#00F5A0", v -> {
            dismissPopupWithAnimation(dialog, root, this::showInAppUpdateCenterDialog);
        }));

        list.addView(createCompactSettingRow("📤  Share ProStream App", "Link / APK", "#38BDF8", v -> {
            dismissPopupWithAnimation(dialog, root, this::showShareProStreamDialog);
        }));

        list.addView(createCompactSettingRow("📥  Video Downloader", "Open", "#00E5FF", v -> {
            dismissPopupWithAnimation(dialog, root, () -> showUniversalLinkDownloaderDialog(""));
        }));

        list.addView(createCompactSettingRow("🎞️  Downloads", "View", "#F9A8D4", v -> {
            dismissPopupWithAnimation(dialog, root, this::showMediaDownloadsDialog);
        }));

        list.addView(createCompactSettingRow("🔋  Battery Saver", batterySaverEnabled ? "ON" : "OFF", batterySaverEnabled ? "#00F5A0" : "#94A3B8", v -> {
            dismissPopupWithAnimation(dialog, root, this::showBatterySaverDialog);
        }));

        list.addView(createCompactSettingRow("⚡  AdBlock DNS", "Setup", "#7DD3FC", v -> {
            dismissPopupWithAnimation(dialog, root, this::showPrivateDnsSetupDialog);
        }));

        list.addView(createCompactSettingRow("🌙  Dark Mode", darkModeEnabled ? "ON" : "OFF", darkModeEnabled ? "#FDE047" : "#94A3B8", v -> {
            darkModeEnabled = !darkModeEnabled;
            sharedPrefs.edit().putBoolean("DARK_MODE", darkModeEnabled).apply();
            for (WebView wv : webViewPool.values()) {
                applyWebViewDarkMode(wv, darkModeEnabled);
            }
            dismissPopupWithAnimation(dialog, root, null);
        }));

        list.addView(createCompactSettingRow("🎨  Theme & Accent Color", "Customize", "#38BDF8", v -> {
            dismissPopupWithAnimation(dialog, root, () -> {
                launchingInternalActivity = true;
                startActivity(new Intent(HomeActivity.this, InterfaceActivity.class));
            });
        }));

        list.addView(createCompactSettingRow("🔗  Manage Sites", String.valueOf(siteList.size()), "#6EE7B7", v -> {
            dismissPopupWithAnimation(dialog, root, this::showSiteManagerDialog);
        }));

        int lockType = sharedPrefs.getInt("LOCK_TYPE", 1);
        list.addView(createCompactSettingRow("🛡️  App Lock", lockType == 0 ? "OFF" : "PIN", "#C4B5FD", v -> {
            dismissPopupWithAnimation(dialog, root, this::showAppLockDialog);
        }));

        list.addView(createCompactSettingRow("🔓  App Permissions", "Grant", "#00F5A0", v -> {
            dismissPopupWithAnimation(dialog, root, () -> requestAllMobileAndChromePermissions(true));
        }));

        list.addView(createCompactSettingRow("🗑️  Clear Cache", getFormattedCacheSize(), "#FDA4AF", v -> {
            dismissPopupWithAnimation(dialog, root, this::clearAppCacheNow);
        }));

        list.addView(createCompactSettingRow("✈️  Telegram Support", "@contactyasinbot", "#00F5A0", v -> {
            dismissPopupWithAnimation(dialog, root, () -> openUrlSafely(TELEGRAM_BOT_URL));
        }));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(8), 0, 0);
        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnClose);
        btnClose.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        showAnimatedDialog(dialog, root);
    }

    private void showBatterySaverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("Battery Saver");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(12));
        root.addView(title);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout ecoRow = new LinearLayout(this);
        ecoRow.setOrientation(LinearLayout.HORIZONTAL);
        ecoRow.setGravity(Gravity.CENTER_VERTICAL);
        ecoRow.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        LinearLayout.LayoutParams erParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        erParams.setMargins(0, 0, 0, dpToPx(8));
        ecoRow.setLayoutParams(erParams);

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setCornerRadius(dpToPx(14));
        rowBg.setColor(Color.parseColor("#1C202E"));
        rowBg.setStroke(dpToPx(1), Color.parseColor("#2B3042"));
        ecoRow.setBackground(rowBg);

        TextView tvEcoLabel = new TextView(this);
        tvEcoLabel.setText("🔋  Eco Mode");
        tvEcoLabel.setTextColor(Color.WHITE);
        tvEcoLabel.setTextSize(14f);
        tvEcoLabel.setTypeface(modernFont, Typeface.BOLD);
        tvEcoLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final Switch swEco = new Switch(this);
        swEco.setChecked(batterySaverEnabled);
        swEco.setOnCheckedChangeListener((buttonView, isChecked) -> {
            batterySaverEnabled = isChecked;
            sharedPrefs.edit().putBoolean("BATTERY_SAVER", batterySaverEnabled).apply();
        });

        ecoRow.addView(tvEcoLabel);
        ecoRow.addView(swEco);
        ecoRow.setOnClickListener(v -> swEco.setChecked(!swEco.isChecked()));
        root.addView(ecoRow);

        boolean isUnrestricted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                isUnrestricted = pm.isIgnoringBatteryOptimizations(getPackageName());
            }
        }

        root.addView(createCompactSettingRow(
                "⚡  Background Activity",
                isUnrestricted ? "Allowed" : "Allow",
                isUnrestricted ? "#00F5A0" : "#7DD3FC",
                v -> {
                    dismissPopupWithAnimation(dialog, root, () -> {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                                    launchingInternalActivity = true;
                                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                }
                            }
                        } catch (Exception ignored) {}
                    });
                }));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(8), 0, 0);
        TextView btnClose = createDialogButton("Done", "#06101E", "#00F5A0");
        attachTouchBounceAnimation(btnClose);
        btnClose.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        showAnimatedDialog(dialog, root);
    }

    private void showPrivateDnsSetupDialog() {
        final String dnsHost = "p2.freedns.controld.com";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout root = createStyledDialogCard();

        TextView title = new TextView(this);
        title.setText("AdBlock Private DNS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setTypeface(modernFont, Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(12));
        root.addView(title);

        TextView hostBox = new TextView(this);
        hostBox.setText(dnsHost);
        hostBox.setTextColor(Color.parseColor("#00F5A0"));
        hostBox.setTextSize(14f);
        hostBox.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        hostBox.setGravity(Gravity.CENTER);
        hostBox.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setCornerRadius(dpToPx(14));
        boxBg.setColor(Color.parseColor("#10141F"));
        boxBg.setStroke(dpToPx(1), Color.parseColor("#3300F5A0"));
        hostBox.setBackground(boxBg);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, 0, 0, dpToPx(14));
        hostBox.setLayoutParams(boxParams);
        root.addView(hostBox);

        builder.setView(root);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.END);

        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        attachTouchBounceAnimation(btnClose);
        btnClose.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));

        TextView btnCopyDns = createDialogButton("Copy & Open Settings", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        copyParams.setMargins(dpToPx(8), 0, 0, 0);
        btnCopyDns.setLayoutParams(copyParams);
        attachTouchBounceAnimation(btnCopyDns);
        btnCopyDns.setOnClickListener(v -> {
            dismissPopupWithAnimation(dialog, root, () -> {
                try {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Private DNS", dnsHost);
                    if (clipboard != null) clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Copied: " + dnsHost, Toast.LENGTH_SHORT).show();
                    launchingInternalActivity = true;
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                } catch (Exception ignored) {}
            });
        });

        bottomRow.addView(btnClose);
        bottomRow.addView(btnCopyDns);
        root.addView(bottomRow);

        showAnimatedDialog(dialog, root);
    }

    private String getFormattedCacheSize() {
        try {
            long bytes = getFolderSize(getCacheDir());
            double mb = bytes / (1024.0 * 1024.0);
            return String.format(Locale.US, "%.1f MB", mb);
        } catch (Exception e) {
            return "0 MB";
        }
    }

    private long getFolderSize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    size += f.isDirectory() ? getFolderSize(f) : f.length();
                }
            }
        }
        return size;
    }

    private void clearAppCacheNow() {
        try {
            for (WebView wv : webViewPool.values()) {
                wv.clearCache(true);
            }
            CookieManager.getInstance().flush();
            reloadActivePageCleanly();
            Toast.makeText(this, "Cache cleared (Cookies & OG icons preserved)", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void openUrlSafely(String url) {
        try {
            launchingInternalActivity = true;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            if (webView != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                webView.loadUrl(url);
            }
        }
    }

    private void saveSitesToPrefs() {
        try {
            JSONArray arr = new JSONArray();
            for (SiteItem s : siteList) {
                JSONObject o = new JSONObject();
                o.put("name", s.name);
                o.put("url", s.url);
                o.put("color", s.color);
                o.put("ogImage", s.ogImage != null ? s.ogImage : "");
                arr.put(o);
            }
            sharedPrefs.edit().putString("SITES_JSON", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private LinearLayout createStyledDialogCard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(18);
        root.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(24));
        bg.setColor(Color.parseColor("#141722"));
        bg.setStroke(dpToPx(1), Color.parseColor("#28FFFFFF"));
        root.setBackground(bg);
        return root;
    }

    private EditText createStyledEditText(String hint, String initialText, boolean isPassword) {
        EditText et = new EditText(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dpToPx(10));
        et.setLayoutParams(params);
        et.setHint(hint);
        et.setText(initialText);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.parseColor("#64748B"));
        et.setTextSize(14f);
        et.setSingleLine(true);
        if (isPassword) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        et.setPadding(dpToPx(14), dpToPx(11), dpToPx(14), dpToPx(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(12));
        bg.setColor(Color.parseColor("#1C202E"));
        bg.setStroke(dpToPx(1), Color.parseColor("#33FFFFFF"));
        et.setBackground(bg);
        return et;
    }

    private View createCompactSettingRow(String titleText, String badgeText, String badgeColorHex, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dpToPx(8));
        item.setLayoutParams(params);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(14));
        bg.setColor(Color.parseColor("#1C202E"));
        bg.setStroke(dpToPx(1), Color.parseColor("#2B3042"));
        item.setBackground(bg);

        TextView t = new TextView(this);
        t.setText(titleText);
        t.setTextColor(Color.WHITE);
        t.setTextSize(13.5f);
        t.setTypeface(modernFont, Typeface.BOLD);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = new TextView(this);
        badge.setText(badgeText);
        badge.setTextColor(Color.parseColor(badgeColorHex));
        badge.setTextSize(11.5f);
        badge.setTypeface(modernFont, Typeface.BOLD);
        badge.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));

        GradientDrawable bBg = new GradientDrawable();
        bBg.setCornerRadius(dpToPx(8));
        bBg.setColor(Color.parseColor("#121726"));
        badge.setBackground(bBg);

        item.addView(t);
        item.addView(badge);
        attachTouchBounceAnimation(item);
        item.setOnClickListener(listener);
        return item;
    }

    private TextView createDialogButton(String text, String textColorHex, String bgColorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(textColorHex));
        tv.setTextSize(12f);
        tv.setTypeface(modernFont, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(12));
        bg.setColor(Color.parseColor(bgColorHex));
        tv.setBackground(bg);
        return tv;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!launchingInternalActivity) {
            leftAppToBackground = true;
        }
    }

    private void resetAllWebViewsToSiteHome() {
        liveDetectedStreamsMap.clear();
        for (SiteItem s : siteList) {
            WebView wv = webViewPool.get(s.url);
            if (wv != null) {
                try {
                    wv.stopLoading();
                    wv.evaluateJavascript("(function(){var m=document.querySelectorAll('video,audio');for(var i=0;i<m.length;i++){try{m[i].pause();m[i].src='';}catch(e){}}})();", null);
                    String currentUrl = wv.getUrl();
                    if (currentUrl != null && !isRootHomepageUrl(currentUrl, s.url)) {
                        clearHistoryOnLoadSet.add(wv);
                        wv.loadUrl(s.url);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        forceEnableAnimations();
        sessionAlive = true;
        launchingInternalActivity = false;
        leftAppToBackground = false;
        if (pendingUpdateApkFile != null && pendingUpdateApkFile.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls()) {
                File toInstall = pendingUpdateApkFile;
                pendingUpdateApkFile = null;
                installDownloadedUpdateApk(toInstall);
            }
        }
        darkModeEnabled = sharedPrefs.getBoolean("DARK_MODE", false);
        batterySaverEnabled = sharedPrefs.getBoolean("BATTERY_SAVER", true);
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
        startFloatingHubBreathingAnimation();
        startNotificationProgressTrackerStatic(appContextRef);
        applyDynamicTheme();
    }

    @Override
    protected void onPause() {
        exitCustomFullscreen();
        if (webView != null) {
            try {
                webView.evaluateJavascript("(function(){var m=document.querySelectorAll('video,audio');for(var i=0;i<m.length;i++){try{m[i].pause();}catch(e){}}})();", null);
            } catch (Exception ignored) {}
        }
        saveSitesToPrefs();
        boolean hasActiveDl = updateLiveDownloadNotificationsAndSpeedStatic(appContextRef);
        if (webView != null && !hasActiveDl) {
            webView.onPause();
        }
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && (leftAppToBackground || !launchingInternalActivity)) {
            resetAllWebViewsToSiteHome();
        }
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        sessionAlive = false;
        sessionUnlocked = false;
        stopPopupAnimators();
        for (ValueAnimator a : hubIdleAnimators) {
            try { a.cancel(); } catch (Exception ignored) {}
        }
        hubIdleAnimators.clear();
        if (mediaLiveRefreshRunnable != null) {
            mainHandler.removeCallbacks(mediaLiveRefreshRunnable);
        }

        saveSitesToPrefs();

        boolean hasActiveDl = updateLiveDownloadNotificationsAndSpeedStatic(appContextRef);
        if (!hasActiveDl) {
            bgDownloadHandler.removeCallbacks(staticLiveDownloadPoller);
            isNotificationTrackerRunning = false;
            releaseTurboDownloadBoosterStatic();
        } else {
            startBackgroundDownloadKeeperService(appContextRef);
            acquireTurboDownloadBoosterStatic(appContextRef);
            startNotificationProgressTrackerStatic(appContextRef);
        }

        try {
            CookieManager.getInstance().flush();
            for (WebView wv : webViewPool.values()) {
                if (wv != null) {
                    wv.stopLoading();
                    wv.destroy();
                }
            }
            webViewPool.clear();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    private void applyDynamicTheme() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#080B12"));
        window.setNavigationBarColor(Color.parseColor("#080B12"));

        int customAccent = parseColorSafe(sharedPrefs.getString("APP_COLOR", "#00F5A0"), Color.parseColor("#00F5A0"));
        if (btnFloatingHub != null) {
            btnFloatingHub.setStrokeColor(customAccent);
        }
        if (tvHubBadge != null) {
            tvHubBadge.setTextColor(customAccent);
        }
        if (progressBar != null) {
            progressBar.setIndicatorColor(customAccent);
        }

        if (currentSite != null) {
            updateUIForCurrentSite();
        }
    }

    private void loadCustomLinksAndPreloadAll() {
        siteList.clear();
        String json = sharedPrefs.getString("SITES_JSON", "");
        boolean migratedSingleSite = sharedPrefs.getBoolean("SINGLE_PRMOVIES_DEFAULT_V3", false);
        boolean cleanedSubpageOg = sharedPrefs.getBoolean("OG_CLEAN_ROOT_V6", false);

        if (json.isEmpty() || !migratedSingleSite) {
            ArrayList<SiteItem> keptCustomSites = new ArrayList<>();
            if (!json.isEmpty()) {
                try {
                    JSONArray oldArr = new JSONArray(json);
                    for (int i = 0; i < oldArr.length(); i++) {
                        JSONObject o = oldArr.getJSONObject(i);
                        String u = o.optString("url", "");
                        String lowerU = u.toLowerCase(Locale.ROOT);
                        if (!lowerU.contains("watchomovies") && !lowerU.contains("medicare") && !lowerU.contains("prmovies")) {
                            SiteItem item = new SiteItem(
                                    o.optString("name", "Site"),
                                    u,
                                    o.optString("color", "#00F5A0"),
                                    "");
                            keptCustomSites.add(item);
                        }
                    }
                } catch (Exception ignored) {}
            }

            siteList.add(new SiteItem("Prmovies", DEFAULT_PRMOVIES_URL, "#00F5A0", ""));
            siteList.addAll(keptCustomSites);
            saveSitesToPrefs();
            sharedPrefs.edit()
                    .putBoolean("SINGLE_PRMOVIES_DEFAULT_V3", true)
                    .putBoolean("OG_CLEAN_ROOT_V6", true)
                    .apply();
        } else {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String siteColor = obj.optString("color", "#00F5A0");
                    String ogImg = cleanedSubpageOg ? obj.optString("ogImage", "") : "";
                    SiteItem item = new SiteItem(obj.getString("name"), obj.getString("url"), siteColor, ogImg);
                    siteList.add(item);
                }
                if (!cleanedSubpageOg) {
                    sharedPrefs.edit().putBoolean("OG_CLEAN_ROOT_V6", true).apply();
                    saveSitesToPrefs();
                }
            } catch (Exception ignored) {}
        }

        if (siteList.isEmpty()) {
            siteList.add(new SiteItem("Prmovies", DEFAULT_PRMOVIES_URL, "#00F5A0", ""));
            saveSitesToPrefs();
        }

        for (SiteItem s : siteList) {
            preloadSiteOgBitmapInBackground(s);
        }

        if (currentSite == null && !siteList.isEmpty()) {
            switchToSiteInstantInternal(siteList.get(0), false, false);
        }
    }

    private void updateUIForCurrentSite() {
        if (currentSite == null) return;
        int customAccent = parseColorSafe(sharedPrefs.getString("APP_COLOR", "#00F5A0"), Color.parseColor("#00F5A0"));
        if (progressBar != null) progressBar.setIndicatorColor(customAccent);
        if (tvHubBadge != null) {
            String letter = currentSite.name.isEmpty() ? "P" : String.valueOf(currentSite.name.charAt(0)).toUpperCase(Locale.ROOT);
            tvHubBadge.setText("HUB • " + letter);
        }
    }

    private Bitmap createRoundedLetterBitmap(String name, int bgColor) {
        int size = 96;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(bgColor);
        RectF rect = new RectF(0, 0, size, size);
        canvas.drawRoundRect(rect, 28f, 28f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(42f);
        paint.setTypeface(Typeface.create(modernFont, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        int yPos = (int) ((canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f));
        String letter = (name == null || name.isEmpty()) ? "S" : String.valueOf(name.charAt(0)).toUpperCase(Locale.ROOT);
        canvas.drawText(letter, size / 2f, yPos, paint);
        return bitmap;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private int parseColorSafe(String colorStr, int fallback) {
        try {
            return Color.parseColor(colorStr);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            exitCustomFullscreen();
        } else if (modernOfflineOverlay != null && modernOfflineOverlay.getVisibility() == View.VISIBLE) {
            hideModernOfflineScreen();
            if (webView != null && webView.canGoBack()) {
                goBackWithSmoothAnimation();
            }
        } else if (webView != null && webView.canGoBack()) {
            goBackWithSmoothAnimation();
        } else if (!siteHistoryStack.isEmpty()) {
            SiteItem prevSite = siteHistoryStack.pop();
            switchToSiteInstantInternal(prevSite, false, false);
        } else {
            final boolean isDlRunning = updateLiveDownloadNotificationsAndSpeedStatic(appContextRef);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final LinearLayout root = createStyledDialogCard();

            TextView title = new TextView(this);
            title.setText("Exit ProStream?");
            title.setTextColor(Color.WHITE);
            title.setTextSize(17f);
            title.setTypeface(modernFont, Typeface.BOLD);
            title.setPadding(0, 0, 0, isDlRunning ? dpToPx(6) : dpToPx(14));
            root.addView(title);

            if (isDlRunning) {
                TextView sub = new TextView(this);
                sub.setText("Active downloads will continue in background.");
                sub.setTextColor(Color.parseColor("#94A3B8"));
                sub.setTextSize(12.5f);
                sub.setPadding(0, 0, 0, dpToPx(14));
                root.addView(sub);
            }

            builder.setView(root);
            final AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.END);

            TextView btnStay = createDialogButton("Cancel", "#94A3B8", "#1E2230");
            attachTouchBounceAnimation(btnStay);
            btnStay.setOnClickListener(v -> dismissPopupWithAnimation(dialog, root, null));

            TextView btnExit = createDialogButton("Exit", "#FFFFFF", isDlRunning ? "#00A884" : "#E50914");
            LinearLayout.LayoutParams exitParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            exitParams.setMargins(dpToPx(8), 0, 0, 0);
            btnExit.setLayoutParams(exitParams);
            attachTouchBounceAnimation(btnExit);
            btnExit.setOnClickListener(v -> {
                dismissPopupWithAnimation(dialog, root, () -> {
                    sessionAlive = false;
                    sessionUnlocked = false;
                    resetAllWebViewsToSiteHome();
                    saveSitesToPrefs();
                    CookieManager.getInstance().flush();
                    if (isDlRunning) {
                        startBackgroundDownloadKeeperService(appContextRef);
                        acquireTurboDownloadBoosterStatic(appContextRef);
                        startNotificationProgressTrackerStatic(appContextRef);
                    }
                    finishAffinity();
                });
            });

            btnRow.addView(btnStay);
            btnRow.addView(btnExit);
            root.addView(btnRow);

            showAnimatedDialog(dialog, root);
        }
    }

    static class SiteItem {
        String name, url, color, ogImage;

        SiteItem(String name, String url, String color, String ogImage) {
            this.name = name;
            this.url = url;
            this.color = color;
            this.ogImage = ogImage != null ? ogImage : "";
        }

        String getEffectiveUrl() {
            return url;
        }
    }
}
