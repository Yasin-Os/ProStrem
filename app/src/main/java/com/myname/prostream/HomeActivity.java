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
import android.os.Message;
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
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
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

    // বর্তমান পেজে প্লে হওয়া ভিডিওগুলোর রিয়েল-টাইম ক্যাশ (০% CPU ওভারহেডে তাৎক্ষণিক ডাউনলোডের জন্য)
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

    public static boolean isSessionAlive() {
        return sessionAlive;
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

    private SiteItem currentSite;
    private final int appColor = Color.parseColor("#00A884");
    private boolean darkModeEnabled = false;
    private boolean batterySaverEnabled = true;
    private Typeface modernFont;
    private String browserUserAgent;

    private final ExecutorService bgExecutor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable mediaLiveRefreshRunnable;
    private final ArrayList<ValueAnimator> activePopupAnimators = new ArrayList<>();
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

        ogMemoryCache = new LruCache<>(28);
        browserUserAgent = WebSettings.getDefaultUserAgent(this).replace("; wv", "");

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
        restoreCookiesFromVault();

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
        startNotificationProgressTrackerStatic(appContextRef);

        if (getIntent() != null && getIntent().getBooleanExtra("OPEN_MEDIA_DIALOG", false)) {
            mainHandler.postDelayed(this::showMediaDownloadsDialog, 350);
        }
    }

    private void saveCookiesToVault(String url) {
        try {
            CookieManager cm = CookieManager.getInstance();
            cm.flush();
            if (url == null || !url.startsWith("http")) return;

            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) return;
            String baseOrigin = uri.getScheme() + "://" + host;

            String rawCookies = cm.getCookie(url);
            if (rawCookies == null || rawCookies.isEmpty()) {
                rawCookies = cm.getCookie(baseOrigin);
            }
            if (rawCookies == null || rawCookies.isEmpty()) return;

            String vaultStr = sharedPrefs.getString("COOKIE_VAULT_V4", "{}");
            JSONObject vault = new JSONObject(vaultStr);
            vault.put(baseOrigin, rawCookies);
            sharedPrefs.edit().putString("COOKIE_VAULT_V4", vault.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void restoreCookiesFromVault() {
        try {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            String vaultStr = sharedPrefs.getString("COOKIE_VAULT_V4", "{}");
            JSONObject vault = new JSONObject(vaultStr);
            Iterator<String> keys = vault.keys();
            while (keys.hasNext()) {
                String origin = keys.next();
                String cookieLine = vault.optString(origin, "");
                if (!cookieLine.isEmpty()) {
                    String[] pairs = cookieLine.split(";");
                    for (String pair : pairs) {
                        String trimmed = pair.trim();
                        if (!trimmed.isEmpty()) {
                            cm.setCookie(origin, trimmed + "; Path=/");
                        }
                    }
                }
            }
            cm.flush();
        } catch (Exception ignored) {}
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
                    requestPermissions(needed.toArray(new String[0]), 701);
                } catch (Exception ignored) {}
            } else if (requestedByUser) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            }

            if (requestedByUser) {
                try {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
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
        target.goBack();

        target.setTranslationX(-dpToPx(16));
        target.animate()
                .translationX(0f)
                .setDuration(140)
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
        btnRetry.setOnClickListener(v -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
            reloadActivePageCleanly();
        });
        card.addView(btnRetry);

        TextView btnSwitch = createDialogButton("Switch Site", "#7DD3FC", "#17253B");
        btnSwitch.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
            modernOfflineOverlay.animate().alpha(1f).setDuration(200).start();
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
                if (child instanceof WebView && batterySaverEnabled) {
                    try {
                        ((WebView) child).onPause();
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
        targetWv.setTranslationX(dpToPx(12));
        targetWv.animate()
                .translationX(0f)
                .setDuration(150)
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

    private void preloadRemainingSitesInBackground() {
        if (batterySaverEnabled) {
            return;
        }
        for (int i = 0; i < siteList.size(); i++) {
            final SiteItem item = siteList.get(i);
            if (webViewPool.containsKey(item.url)) continue;

            mainHandler.postDelayed(() -> {
                if (!isFinishing() && !batterySaverEnabled && webViewHolder != null && !webViewPool.containsKey(item.url)) {
                    WebView bgWv = createConfiguredWebView(item);
                    bgWv.setVisibility(View.GONE);
                    webViewPool.put(item.url, bgWv);
                    webViewHolder.addView(bgWv, 0, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    bgWv.loadUrl(item.getEffectiveUrl());
                }
            }, 3000L * (i + 1));
        }
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
                        v.animate().scaleX(0.86f).scaleY(0.86f).setDuration(90).start();
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
                        v.animate().scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(new OvershootInterpolator(1.6f)).start();
                        if (!isDragging) {
                            if (ivHubIcon != null) {
                                ivHubIcon.animate().rotationBy(180f).setDuration(320).start();
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
                .scaleX(0.84f)
                .scaleY(0.84f)
                .translationY(dpToPx(28))
                .setDuration(150)
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
                    v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(new OvershootInterpolator(1.5f)).start();
                    break;
            }
            return false;
        });
    }

    private void showInstantAppSwitcherDialog() {
        try {
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
            animTarget.setScaleX(0.72f);
            animTarget.setScaleY(0.72f);
            animTarget.setTranslationY(dpToPx(56));

            dialog.setOnShowListener(d -> {
                animTarget.post(() -> {
                    animTarget.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationY(0f)
                            .setDuration(340)
                            .setInterpolator(new OvershootInterpolator(1.28f))
                            .start();
                });
            });

            dialog.setOnDismissListener(d -> stopPopupAnimators());

            View headerRow = dialogView.findViewById(R.id.dialogHeaderRow);
            if (headerRow != null) {
                animateCascadeItem(headerRow, 20);
            }

            if (rootCard != null) {
                ValueAnimator borderGlow = ValueAnimator.ofArgb(
                        Color.parseColor("#00F5A0"),
                        Color.parseColor("#00E5FF"),
                        Color.parseColor("#7C3AED"),
                        Color.parseColor("#00F5A0"));
                borderGlow.setDuration(3200);
                borderGlow.setRepeatCount(ValueAnimator.INFINITE);
                borderGlow.addUpdateListener(a -> rootCard.setStrokeColor((int) a.getAnimatedValue()));
                borderGlow.start();
                activePopupAnimators.add(borderGlow);
            }

            View cardFlashBadge = dialogView.findViewById(R.id.cardFlashBadge);
            if (cardFlashBadge != null) {
                ObjectAnimator logoPulse = ObjectAnimator.ofPropertyValuesHolder(
                        cardFlashBadge,
                        PropertyValuesHolder.ofFloat("scaleX", 1f, 1.09f, 1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f, 1.09f, 1f)
                );
                logoPulse.setDuration(2000);
                logoPulse.setRepeatCount(ValueAnimator.INFINITE);
                logoPulse.start();
                activePopupAnimators.add(logoPulse);
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
                    icon.setImageBitmap(createRoundedLetterBitmap(site.name, siteColorInt));
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

                    animateCascadeItem(itemLayout, 40 + (40 * Math.min(i, 3)));
                    container.addView(itemLayout);
                }
            }

            View btnMedia = dialogView.findViewById(R.id.btnDialogTheme);
            if (btnMedia != null) {
                animateCascadeItem(btnMedia, 120);
                attachTouchBounceAnimation(btnMedia);
                btnMedia.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showMediaDownloadsDialog);
                });
            }

            View btnDownloader = dialogView.findViewById(R.id.btnDialogDownloader);
            if (btnDownloader != null) {
                animateCascadeItem(btnDownloader, 145);
                attachTouchBounceAnimation(btnDownloader);
                btnDownloader.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, () -> showUniversalLinkDownloaderDialog(""));
                });
            }

            View btnAddSite = dialogView.findViewById(R.id.btnDialogAddSite);
            if (btnAddSite != null) {
                animateCascadeItem(btnAddSite, 170);
                attachTouchBounceAnimation(btnAddSite);
                btnAddSite.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showSiteManagerDialog);
                });
            }

            View btnSettings = dialogView.findViewById(R.id.btnDialogSettings);
            if (btnSettings != null) {
                animateCascadeItem(btnSettings, 195);
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
                animateCascadeItem(btnQuickDark, 215);
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
                animateCascadeItem(btnQuickAppLock, 235);
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
                animateCascadeItem(btnQuickEco, 255);
                attachTouchBounceAnimation(btnQuickEco);
                btnQuickEco.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showBatterySaverDialog);
                });
            }

            View btnQuickAdBlock = dialogView.findViewById(R.id.btnQuickAdBlock);
            if (btnQuickAdBlock != null) {
                animateCascadeItem(btnQuickAdBlock, 275);
                attachTouchBounceAnimation(btnQuickAdBlock);
                btnQuickAdBlock.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::showPrivateDnsSetupDialog);
                });
            }

            View btnQuickCache = dialogView.findViewById(R.id.btnQuickCache);
            if (btnQuickCache != null) {
                animateCascadeItem(btnQuickCache, 295);
                attachTouchBounceAnimation(btnQuickCache);
                btnQuickCache.setOnClickListener(v -> {
                    dismissPopupWithAnimation(dialog, animTarget, this::clearAppCacheNow);
                });
            }

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void animateCascadeItem(final View view, int delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(dpToPx(18));
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);
        view.post(() -> {
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(delayMs)
                    .setDuration(250)
                    .setInterpolator(new OvershootInterpolator(1.15f))
                    .start();
        });
    }

    // =========================================================================
    // Smart Universal Downloader Dialog (With Instant Detected Video + Link Paste)
    // =========================================================================
    private void showUniversalLinkDownloaderDialog(String prefillUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = createStyledDialogCard();

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

        // যদি বর্তমান পেজে কোনো ভিডিও প্লে হয়ে থাকে, তবে ১-ট্যাপে ডাউনলোডের জন্য Instant Detected Card দেখাবে
        if (!liveDetectedStreamsMap.isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(liveDetectedStreamsMap.entrySet());
            final Map.Entry<String, String> latest = entries.get(entries.size() - 1);
            final String detectedStreamUrl = latest.getKey();
            final String detectedTitle = latest.getValue();

            View instantBtn = createCompactSettingRow("⚡  " + detectedTitle, "Download Now", "#00F5A0", v -> {
                dialog.dismiss();
                String referer = (webView != null && webView.getUrl() != null) ? webView.getUrl() : detectedStreamUrl;
                boolean isHls = detectedStreamUrl.toLowerCase(Locale.ROOT).contains(".m3u8");
                launchBestDownloadEngine(detectedStreamUrl, referer, detectedTitle, isHls);
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
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        final TextView btnStartDl = createDialogButton("Download", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams dlParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlParams.setMargins(dpToPx(8), 0, 0, 0);
        btnStartDl.setLayoutParams(dlParams);

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

        dialog.show();
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
        LinearLayout root = createStyledDialogCard();

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
                qDialog.dismiss();
                launchBestDownloadEngine(item.url, refererPageUrl, baseTitle, item.isHls);
            });
            list.addView(row);
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(8), 0, 0);
        TextView btnClose = createDialogButton("Cancel", "#94A3B8", "#1E2230");
        btnClose.setOnClickListener(v -> qDialog.dismiss());
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        qDialog.show();
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

    // =========================================================================
    // 4x Multi-Threaded Parallel Range + Auto-Resume Turbo Download Engine
    // =========================================================================
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

            // যদি ফাইলটি ২ মেগাবাইটের বড় হয় এবং সার্ভার Range সাপোর্ট করে, তবে ৪টি থ্রেডে একসাথে ৪ গুণ দ্রুত ডাউনলোড করবে!
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
                // যদি কোনো CDN প্যারালাল কানেকশন লিমিট করে, তবে সিঙ্গেল স্ট্রিম রিজিউম মোডে ফলব্যাক করবে
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

    // =========================================================================
    // 4x Parallel Batch HLS (.m3u8) Segment Downloader
    // =========================================================================
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
                        if (i + 1 < lines.length) {
                            String next = lines[i + 1].trim();
                            if (!next.isEmpty() && !next.startsWith("#") && bw >= bestBw) {
                                bestBw = bw;
                                bestSubUrl = resolveRelativeUrl(activePlaylistUrl, next);
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
            for (String line : playlistText.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("#EXT-X-MAP:")) {
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
                    final String segUrl = segmentUrls.get(j);
                    batchCallables.add(() -> downloadSegmentBytesWithRetry(segUrl, refererUrl, task));
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

    // =========================================================================
    // Clean, Real Downloads Dialog (Shows ONLY ProStream Downloads, Never Chrome Cache!)
    // =========================================================================
    private void showMediaDownloadsDialog() {
        startNotificationProgressTrackerStatic(appContextRef);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = createStyledDialogCard();

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
        btnNewDl.setOnClickListener(v -> {
            dialog.dismiss();
            showUniversalLinkDownloaderDialog("");
        });

        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        btnClose.setOnClickListener(v -> dialog.dismiss());

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
        dialog.show();
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

    private void openDownloadedVideoItem(String publicUriStr, String fallbackPath) {
        try {
            if (publicUriStr != null && !publicUriStr.isEmpty()) {
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(Uri.parse(publicUriStr), "video/*");
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(openIntent, "Open Video"));
                return;
            }
        } catch (Exception ignored) {}
        try {
            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
        } catch (Exception ignored) {}
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
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            openUrlSafely(url);
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
        LinearLayout root = createStyledDialogCard();

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
            icon.setImageBitmap(createRoundedLetterBitmap(s.name, cInt));
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

            row.setOnClickListener(v -> {
                dialog.dismiss();
                showAddOrEditSiteDialog(idx);
            });
            listContainer.addView(row);
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(12), 0, 0);

        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        btnClose.setOnClickListener(v -> dialog.dismiss());

        TextView btnAddNew = createDialogButton("+ Add Site", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addParams.setMargins(dpToPx(8), 0, 0, 0);
        btnAddNew.setLayoutParams(addParams);
        btnAddNew.setOnClickListener(v -> {
            dialog.dismiss();
            showAddOrEditSiteDialog(-1);
        });

        bottomRow.addView(btnClose);
        bottomRow.addView(btnAddNew);
        root.addView(bottomRow);

        dialog.show();
    }

    private void showAddOrEditSiteDialog(final int editIndex) {
        final boolean isNew = (editIndex < 0 || editIndex >= siteList.size());
        final SiteItem existing = isNew ? new SiteItem("", "", "#00F5A0", "") : siteList.get(editIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = createStyledDialogCard();

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
            btnDelete.setOnClickListener(v -> {
                SiteItem removed = siteList.remove(editIndex);
                WebView oldWv = webViewPool.remove(removed.url);
                if (oldWv != null && webViewHolder != null) {
                    webViewHolder.removeView(oldWv);
                    oldWv.destroy();
                }
                saveSitesToPrefs();
                switchToSiteInstant(siteList.get(0), false);
                dialog.dismiss();
            });
            btnRow.addView(btnDelete);
        }

        TextView btnCancel = createDialogButton("Cancel", "#94A3B8", "#1E2230");
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView btnSave = createDialogButton("Save", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(dpToPx(8), 0, 0, 0);
        btnSave.setLayoutParams(saveParams);
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
            SiteItem updated = new SiteItem(n, u, "#00F5A0", ogToKeep);
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
            switchToSiteInstant(updated, false);
            dialog.dismiss();
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnSave);
        root.addView(btnRow);

        dialog.show();
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
        try {
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
        } catch (Exception ignored) {}
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setGeolocationEnabled(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settings.setOffscreenPreRaster(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(false);
        }

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
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
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                String url = request.getUrl().toString();

                if (isImageOrFaviconUrl(url)) {
                    return true;
                }

                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("blob:") || url.startsWith("data:")) {
                    return false;
                }

                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
                                view.loadUrl(fallbackUrl);
                                return true;
                            }
                            if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                                startActivity(intent);
                                return true;
                            }
                        }
                    } catch (Exception ignored) {}
                    return true;
                }

                openUrlSafely(url);
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
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (swipeRefresh != null && view == webView) {
                    swipeRefresh.setRefreshing(false);
                }
                if (progressBar != null && view == webView) {
                    progressBar.setVisibility(View.GONE);
                }

                if (url == null || url.startsWith("about:blank")) {
                    return;
                }

                saveCookiesToVault(url);

                // সাইটের DOM বা target="_blank" পরিবর্তন করা হবে না, যাতে কোনো অ্যাড ওভারলে মেইন পেজ হাইজ্যাক করতে না পারে
                String pageEnhancerScript = "(function(){" +
                        "if(document.title==='Webpage not available' || (document.body && document.body.innerText.indexOf('ERR_INTERNET_DISCONNECTED')!==-1)){" +
                        "return 'OFFLINE_PAGE_DETECTED';" +
                        "}" +
                        "var t=(document.title||'').toLowerCase();" +
                        "if(t.indexOf('just a moment')!==-1 || t.indexOf('attention required')!==-1 || document.getElementById('cf-wrapper') || document.getElementById('challenge-running')){" +
                        "var st=document.getElementById('ps-dark-style');if(st)st.remove();" +
                        "return 'CF_PAGE';" +
                        "}" +
                        "try{" +
                        "var vids=document.querySelectorAll('video');" +
                        "for(var k=0;k<vids.length;k++){" +
                        "vids[k].preload='auto';" +
                        "vids[k].setAttribute('playsinline','true');" +
                        "vids[k].setAttribute('webkit-playsinline','true');" +
                        "}" +
                        "}catch(e){}" +
                        "var og=document.querySelector('meta[property=\"og:image\"]')||document.querySelector('meta[name=\"twitter:image\"]')||document.querySelector('link[rel*=\"apple-touch-icon\"]');" +
                        "return og?(og.content||og.href||''):'';" +
                        "})()";

                view.evaluateJavascript(pageEnhancerScript, val -> {
                    String res = val != null ? val.replace("\"", "").trim() : "";
                    if ("OFFLINE_PAGE_DETECTED".equals(res)) {
                        lastFailedUrlMap.put(view, url);
                        if (view == webView) {
                            showModernOfflineScreen(url);
                        }
                        return;
                    }
                    if ("CF_PAGE".equals(res)) {
                        return;
                    }
                    if (ownerSite != null && url.startsWith("http") && !isImageOrFaviconUrl(url)) {
                        ownerSite.lastVisitedUrl = url;
                        saveCookiesToVault(url);
                    }
                    if (darkModeEnabled) {
                        applyWebViewDarkMode(view, true);
                    }
                    if (!res.isEmpty() && !res.equals("null") && ownerSite != null) {
                        String absOg = resolveRelativeUrl(url, res);
                        if (!absOg.equals(ownerSite.ogImage)) {
                            ownerSite.ogImage = absOg;
                            saveSitesToPrefs();
                        }
                    }
                });
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(final WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                if (resultMsg == null) return false;
                final String parentUrl = view != null ? view.getUrl() : "";

                // সকল পপআপ ও নতুন ট্যাব ব্যাকগ্রাউন্ডে tempWebView-এর ভেতর নিরাপদে যাচাই করা হবে:
                // ১. যদি এটি ফাইল ডাউনলোড হয় -> সাথে সাথে ডাউনলোড শুরু হবে
                // ২. যদি এটি বৈধ ডাউনলোড সার্ভার হয় -> মেইন পেজে ওপেন হবে
                // ৩. যদি এটি বিজ্ঞাপন বা পপআন্ডার হয় -> মেইন পেজকে বিরক্ত না করে নিঃশব্দে বন্ধ হয়ে যাবে!
                final WebView tempWebView = new WebView(HomeActivity.this);
                tempWebView.getSettings().setJavaScriptEnabled(true);
                tempWebView.getSettings().setDomStorageEnabled(true);
                tempWebView.getSettings().setUserAgentString(browserUserAgent);
                CookieManager.getInstance().setAcceptThirdPartyCookies(tempWebView, true);

                tempWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
                    startProDownload(url, userAgent, contentDisposition, mimetype, null);
                    destroyTempWebViewSafely(tempWebView);
                });

                tempWebView.setWebViewClient(new WebViewClient() {
                    private boolean handled = false;

                    private boolean inspectPopupTarget(String targetUrl) {
                        if (handled || targetUrl == null || targetUrl.isEmpty() || targetUrl.startsWith("about:blank")) {
                            return false;
                        }
                        if (isImageOrFaviconUrl(targetUrl)) {
                            handled = true;
                            destroyTempWebViewSafely(tempWebView);
                            return true;
                        }
                        if (isDirectMediaFileExtension(targetUrl) || isLikelyRealVideoStreamUrl(targetUrl)) {
                            handled = true;
                            startProDownload(targetUrl, browserUserAgent, null, "video/mp4", null);
                            destroyTempWebViewSafely(tempWebView);
                            return true;
                        }
                        if (isKnownDownloadServerOrSameSite(targetUrl, parentUrl)) {
                            handled = true;
                            loadUrlWithReferer(view, targetUrl, parentUrl);
                            destroyTempWebViewSafely(tempWebView);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                        if (req != null && req.getUrl() != null) {
                            return inspectPopupTarget(req.getUrl().toString());
                        }
                        return false;
                    }

                    @Override
                    public void onPageStarted(WebView v, String url, Bitmap favicon) {
                        inspectPopupTarget(url);
                    }
                });

                mainHandler.postDelayed(() -> destroyTempWebViewSafely(tempWebView), 2500);

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(tempWebView);
                resultMsg.sendToTarget();
                return true;
            }

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

    private void destroyTempWebViewSafely(final WebView tempWv) {
        if (tempWv == null) return;
        mainHandler.post(() -> {
            try {
                tempWv.setWebViewClient(null);
                tempWv.setWebChromeClient(null);
                tempWv.stopLoading();
                tempWv.destroy();
            } catch (Throwable ignored) {}
        });
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

    private boolean isRealDownloadOrServerNavigation(String targetUrl, String parentUrl) {
        if (targetUrl == null || targetUrl.isEmpty() || isImageOrFaviconUrl(targetUrl)) return false;
        String lower = targetUrl.toLowerCase(Locale.ROOT);

        String[] adKeywords = {
                "doubleclick.net", "googlesyndication.com", "popads.net", "popcash.net",
                "onclickalgo.com", "onclickperformance.com", "adsterra", "exoclick",
                "juicyads", "propellerads", "hilltopads", "clickadu", "trafficjunky",
                "acscdn.com", "histats.com", "bet365", "1xbet", "melbet", "mostbet"
        };
        for (String kw : adKeywords) {
            if (lower.contains(kw)) return false;
        }
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private boolean isKnownDownloadServerOrSameSite(String targetUrl, String parentUrl) {
        if (!isRealDownloadOrServerNavigation(targetUrl, parentUrl)) return false;
        String lower = targetUrl.toLowerCase(Locale.ROOT);

        try {
            String targetHost = Uri.parse(targetUrl).getHost();
            String parentHost = parentUrl != null ? Uri.parse(parentUrl).getHost() : "";
            if (targetHost != null && parentHost != null && !targetHost.isEmpty() && !parentHost.isEmpty()) {
                if (targetHost.equalsIgnoreCase(parentHost) || targetHost.endsWith("." + parentHost) || parentHost.endsWith("." + targetHost)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        String[] trustedDownloadHosts = {
                "speedostream", "hubcloud", "gdflix", "pixeldrain", "drive.google",
                "mega.nz", "mediafire", "streamtape", "gofile", "1fichier", "katfile",
                "usersdrive", "drop.download", "indiashare", "indishare", "bdupload",
                "clicknupload", "filepress", "gdtot", "filemoon", "streamwish", "vidhide",
                "dood", "voe.sx", "mixdrop", "/download", "/dl/", "/d/", "/f/", "/file/"
        };
        for (String h : trustedDownloadHosts) {
            if (lower.contains(h)) return true;
        }
        return false;
    }

    private void loadUrlWithReferer(WebView targetWv, String url, String referer) {
        if (targetWv == null || url == null || url.isEmpty() || isImageOrFaviconUrl(url)) return;
        if (referer != null && !referer.isEmpty()) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", referer);
            targetWv.loadUrl(url, headers);
        } else {
            targetWv.loadUrl(url);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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

    private void loadSiteOgPhotoAsync(final SiteItem site, final ImageView targetView, final int fallbackColor) {
        if (site == null || site.url == null || site.url.isEmpty()) return;
        final String cacheKey = "og_" + Math.abs(site.url.hashCode());

        Bitmap memBmp = ogMemoryCache.get(cacheKey);
        if (memBmp != null) {
            targetView.setImageBitmap(memBmp);
            return;
        }

        bgExecutor.execute(() -> {
            try {
                File cacheFile = new File(getCacheDir(), cacheKey + ".png");
                if (cacheFile.exists() && cacheFile.length() > 100) {
                    Bitmap diskBmp = BitmapFactory.decodeStream(new FileInputStream(cacheFile));
                    if (diskBmp != null) {
                        Bitmap rounded = makeRoundedPhotoBitmap(diskBmp, fallbackColor);
                        ogMemoryCache.put(cacheKey, rounded);
                        mainHandler.post(() -> targetView.setImageBitmap(rounded));
                        return;
                    }
                }

                String imageUrl = site.ogImage;
                if (imageUrl == null || imageUrl.isEmpty()) {
                    String host = Uri.parse(site.url).getHost();
                    if (host != null) {
                        imageUrl = "https://www.google.com/s2/favicons?sz=128&domain_url=" + host;
                    }
                }

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Bitmap rawBmp = downloadBitmapFromUrl(imageUrl, site.url);
                    if (rawBmp != null && rawBmp.getWidth() > 8) {
                        FileOutputStream fos = new FileOutputStream(cacheFile);
                        rawBmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
                        fos.close();

                        Bitmap rounded = makeRoundedPhotoBitmap(rawBmp, fallbackColor);
                        ogMemoryCache.put(cacheKey, rounded);
                        mainHandler.post(() -> targetView.setImageBitmap(rounded));
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private Bitmap downloadBitmapFromUrl(String imgUrl, String refererUrl) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(imgUrl);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(4500);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", browserUserAgent);
            if (imgUrl.contains("google.com/s2/favicons")) {
                conn.setRequestProperty("Accept", "image/*");
            } else {
                if (refererUrl != null) conn.setRequestProperty("Referer", refererUrl);
                String cookies = CookieManager.getInstance().getCookie(refererUrl != null ? refererUrl : imgUrl);
                if (cookies != null) conn.setRequestProperty("Cookie", cookies);
            }

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
        LinearLayout root = createStyledDialogCard();

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
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView btnSave = createDialogButton("Save", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(dpToPx(8), 0, 0, 0);
        btnSave.setLayoutParams(saveParams);
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
            dialog.dismiss();
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnSave);
        root.addView(btnRow);

        dialog.show();
    }

    private void showAllSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = createStyledDialogCard();

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

        list.addView(createCompactSettingRow("📥  Video Downloader", "Open", "#00E5FF", v -> {
            dialog.dismiss();
            showUniversalLinkDownloaderDialog("");
        }));

        list.addView(createCompactSettingRow("🎞️  Downloads", "View", "#F9A8D4", v -> {
            dialog.dismiss();
            showMediaDownloadsDialog();
        }));

        list.addView(createCompactSettingRow("🔋  Battery Saver", batterySaverEnabled ? "ON" : "OFF", batterySaverEnabled ? "#00F5A0" : "#94A3B8", v -> {
            dialog.dismiss();
            showBatterySaverDialog();
        }));

        list.addView(createCompactSettingRow("⚡  AdBlock DNS", "Setup", "#7DD3FC", v -> {
            dialog.dismiss();
            showPrivateDnsSetupDialog();
        }));

        list.addView(createCompactSettingRow("🌙  Dark Mode", darkModeEnabled ? "ON" : "OFF", darkModeEnabled ? "#FDE047" : "#94A3B8", v -> {
            dialog.dismiss();
            darkModeEnabled = !darkModeEnabled;
            sharedPrefs.edit().putBoolean("DARK_MODE", darkModeEnabled).apply();
            for (WebView wv : webViewPool.values()) {
                applyWebViewDarkMode(wv, darkModeEnabled);
            }
        }));

        list.addView(createCompactSettingRow("🔗  Manage Sites", String.valueOf(siteList.size()), "#6EE7B7", v -> {
            dialog.dismiss();
            showSiteManagerDialog();
        }));

        int lockType = sharedPrefs.getInt("LOCK_TYPE", 1);
        list.addView(createCompactSettingRow("🛡️  App Lock", lockType == 0 ? "OFF" : "PIN", "#C4B5FD", v -> {
            dialog.dismiss();
            showAppLockDialog();
        }));

        list.addView(createCompactSettingRow("🔓  App Permissions", "Grant", "#00F5A0", v -> {
            dialog.dismiss();
            requestAllMobileAndChromePermissions(true);
        }));

        list.addView(createCompactSettingRow("🗑️  Clear Cache", getFormattedCacheSize(), "#FDA4AF", v -> {
            dialog.dismiss();
            clearAppCacheNow();
        }));

        list.addView(createCompactSettingRow("✈️  Telegram Support", "@contactyasinbot", "#00F5A0", v -> {
            dialog.dismiss();
            openUrlSafely(TELEGRAM_BOT_URL);
        }));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(8), 0, 0);
        TextView btnClose = createDialogButton("Close", "#94A3B8", "#1E2230");
        btnClose.setOnClickListener(v -> dialog.dismiss());
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        dialog.show();
    }

    private void showBatterySaverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = createStyledDialogCard();

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
                    dialog.dismiss();
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        }
                    } catch (Exception ignored) {}
                }));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.END);
        bottomRow.setPadding(0, dpToPx(8), 0, 0);
        TextView btnClose = createDialogButton("Done", "#06101E", "#00F5A0");
        btnClose.setOnClickListener(v -> dialog.dismiss());
        bottomRow.addView(btnClose);
        root.addView(bottomRow);

        dialog.show();
    }

    private void showPrivateDnsSetupDialog() {
        final String dnsHost = "p2.freedns.controld.com";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = createStyledDialogCard();

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
        btnClose.setOnClickListener(v -> dialog.dismiss());

        TextView btnCopyDns = createDialogButton("Copy & Open Settings", "#06101E", "#00F5A0");
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        copyParams.setMargins(dpToPx(8), 0, 0, 0);
        btnCopyDns.setLayoutParams(copyParams);
        btnCopyDns.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Private DNS", dnsHost);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied: " + dnsHost, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {}
        });

        bottomRow.addView(btnClose);
        bottomRow.addView(btnCopyDns);
        root.addView(bottomRow);

        dialog.show();
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
            if (webView != null && webView.getUrl() != null) {
                saveCookiesToVault(webView.getUrl());
            }
            for (WebView wv : webViewPool.values()) {
                wv.clearCache(true);
            }
            restoreCookiesFromVault();
            CookieManager.getInstance().flush();
            reloadActivePageCleanly();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void openUrlSafely(String url) {
        try {
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
                o.put("lastVisitedUrl", s.lastVisitedUrl != null ? s.lastVisitedUrl : "");
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
    protected void onResume() {
        super.onResume();
        sessionAlive = true;
        darkModeEnabled = sharedPrefs.getBoolean("DARK_MODE", false);
        batterySaverEnabled = sharedPrefs.getBoolean("BATTERY_SAVER", true);
        restoreCookiesFromVault();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
        startNotificationProgressTrackerStatic(appContextRef);
        applyDynamicTheme();
    }

    @Override
    protected void onPause() {
        if (webView != null && webView.getUrl() != null) {
            saveCookiesToVault(webView.getUrl());
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
    protected void onDestroy() {
        sessionAlive = false;
        stopPopupAnimators();
        if (mediaLiveRefreshRunnable != null) {
            mainHandler.removeCallbacks(mediaLiveRefreshRunnable);
        }

        if (webView != null && webView.getUrl() != null) {
            saveCookiesToVault(webView.getUrl());
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
            if (!hasActiveDl) {
                for (WebView wv : webViewPool.values()) {
                    if (wv != null) {
                        wv.stopLoading();
                        wv.destroy();
                    }
                }
                webViewPool.clear();
            }
        } catch (Exception ignored) {}
        bgExecutor.shutdownNow();
        super.onDestroy();
    }

    private void applyDynamicTheme() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#080B12"));
        window.setNavigationBarColor(Color.parseColor("#080B12"));

        if (currentSite != null) {
            updateUIForCurrentSite();
        }
    }

    private void loadCustomLinksAndPreloadAll() {
        siteList.clear();
        String json = sharedPrefs.getString("SITES_JSON", "");
        boolean migratedSingleSite = sharedPrefs.getBoolean("SINGLE_PRMOVIES_DEFAULT_V3", false);

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
                                    o.optString("ogImage", ""));
                            item.lastVisitedUrl = o.optString("lastVisitedUrl", "");
                            keptCustomSites.add(item);
                        }
                    }
                } catch (Exception ignored) {}
            }

            siteList.add(new SiteItem("Prmovies", DEFAULT_PRMOVIES_URL, "#00F5A0", ""));
            siteList.addAll(keptCustomSites);
            saveSitesToPrefs();
            sharedPrefs.edit().putBoolean("SINGLE_PRMOVIES_DEFAULT_V3", true).apply();
        } else {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String siteColor = obj.optString("color", "#00F5A0");
                    String ogImg = obj.optString("ogImage", "");
                    SiteItem item = new SiteItem(obj.getString("name"), obj.getString("url"), siteColor, ogImg);
                    item.lastVisitedUrl = obj.optString("lastVisitedUrl", "");
                    siteList.add(item);
                }
            } catch (Exception ignored) {}
        }

        if (siteList.isEmpty()) {
            siteList.add(new SiteItem("Prmovies", DEFAULT_PRMOVIES_URL, "#00F5A0", ""));
            saveSitesToPrefs();
        }

        if (currentSite == null && !siteList.isEmpty()) {
            switchToSiteInstantInternal(siteList.get(0), false, false);
        }
        preloadRemainingSitesInBackground();
    }

    private void updateUIForCurrentSite() {
        if (currentSite == null) return;
        if (progressBar != null) progressBar.setIndicatorColor(Color.parseColor("#00F5A0"));
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
            LinearLayout root = createStyledDialogCard();

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
            btnStay.setOnClickListener(v -> dialog.dismiss());

            TextView btnExit = createDialogButton("Exit", "#FFFFFF", isDlRunning ? "#00A884" : "#E50914");
            LinearLayout.LayoutParams exitParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            exitParams.setMargins(dpToPx(8), 0, 0, 0);
            btnExit.setLayoutParams(exitParams);
            btnExit.setOnClickListener(v -> {
                dialog.dismiss();
                if (webView != null && webView.getUrl() != null) {
                    saveCookiesToVault(webView.getUrl());
                }
                saveSitesToPrefs();
                CookieManager.getInstance().flush();
                if (isDlRunning) {
                    startBackgroundDownloadKeeperService(appContextRef);
                    acquireTurboDownloadBoosterStatic(appContextRef);
                    startNotificationProgressTrackerStatic(appContextRef);
                }
                moveTaskToBack(true);
            });

            btnRow.addView(btnStay);
            btnRow.addView(btnExit);
            root.addView(btnRow);

            dialog.show();
        }
    }

    static class SiteItem {
        String name, url, color, ogImage, lastVisitedUrl;
        SiteItem(String name, String url, String color, String ogImage) {
            this.name = name;
            this.url = url;
            this.color = color;
            this.ogImage = ogImage != null ? ogImage : "";
            this.lastVisitedUrl = "";
        }

        String getEffectiveUrl() {
            if (lastVisitedUrl != null && !lastVisitedUrl.isEmpty() && lastVisitedUrl.startsWith("http")) {
                return lastVisitedUrl;
            }
            return url;
        }
    }
}