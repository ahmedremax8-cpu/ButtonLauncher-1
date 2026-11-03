package com.example.buttonlauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout rootLayout;
    private TextView userBadgeView, focusBadgeView;
    private TextView dateView, timeView, batteryView;
    private LinearLayout prayerBox;
    private TextView prayerNextView, prayerScheduleView;
    private LinearLayout countdownBox;
    private TextView countdownTitleView, countdownDaysView, countdownProgressView, countdownBarView;
    private LinearLayout listBox;
    private TextView hintView;

    private final List<TextView> rows = new ArrayList<TextView>();
    private final List<String> pkgs = new ArrayList<String>();
    private int selected = 0;
    private boolean touchLock = true;
    private final Handler handler = new Handler();

    private int keyNext = KeyEvent.KEYCODE_VOLUME_DOWN;
    private int keyOpen = KeyEvent.KEYCODE_VOLUME_UP;
    private String lockToggleMode = "double_vol_down";
    private long lastKeyNextPress = 0;
    private long lastKeyOpenPress = 0;
    private long lastBackPress = 0;
    private long lastAnyKeyPress = 0;
    private int anyKeyHitCount = 0;

    private String timeFormat = "24h";
    private String vacationPosition = "top";
    private String prayerPosition = "top";

    // Theme & Font Scale
    private String themeId = "pure_black";
    private float fontScale = 1.0f;

    private int colorBg = Color.BLACK;
    private int colorText = Color.WHITE;
    private int colorSecondary = Color.LTGRAY;
    private int colorAccent = 0xFFFCD34D;
    private int colorBox = 0xFF141414;
    private int colorSelectBg = 0xFF2A2A2A;

    private double currentLat = 30.0444;
    private double currentLng = 31.2357;

    private final Runnable tick = new Runnable() {
        public void run() {
            updateHeader();
            updateCountdown();
            updatePrayerTimes();
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        SharedPreferences sp = getSharedPreferences("launcher", MODE_PRIVATE);
        // If first run, launch Setup Wizard for permissions & full config
        if (!sp.getBoolean("setup_completed", false)) {
            Intent setup = new Intent(this, SetupWizardActivity.class);
            startActivity(setup);
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = getSharedPreferences("launcher", MODE_PRIVATE);
        touchLock = sp.getBoolean("touchLock", true);
        keyNext = sp.getInt("keyNext", KeyEvent.KEYCODE_VOLUME_DOWN);
        keyOpen = sp.getInt("keyOpen", KeyEvent.KEYCODE_VOLUME_UP);
        lockToggleMode = sp.getString("lockToggleMode", "double_vol_down");
        timeFormat = sp.getString("timeFormat", "24h");
        vacationPosition = sp.getString("vacationPosition", "top");
        prayerPosition = sp.getString("prayerPosition", "top");

        themeId = sp.getString("themeId", "pure_black");
        fontScale = sp.getFloat("fontScale", 1.0f);
        applyThemeColors();

        currentLat = Double.longBitsToDouble(sp.getLong("gpsLat", Double.doubleToLongBits(30.0444)));
        currentLng = Double.longBitsToDouble(sp.getLong("gpsLng", Double.doubleToLongBits(31.2357)));

        checkGpsLocation();
        reorderWidgets();
        loadApps(sp.getString("apps", ""));
        updateCountdown();
        updatePrayerTimes();
        hideSystemBars();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void applyThemeColors() {
        if ("e_ink_light".equals(themeId)) {
            colorBg = Color.WHITE;
            colorText = Color.BLACK;
            colorSecondary = 0xFF555555;
            colorAccent = 0xFF000000;
            colorBox = 0xFFF0F0F0;
            colorSelectBg = 0xFFE0E0E0;
        } else if ("matrix_green".equals(themeId)) {
            colorBg = 0xFF050F05;
            colorText = 0xFF4ADE80;
            colorSecondary = 0xFF22C55E;
            colorAccent = 0xFF86EFAC;
            colorBox = 0xFF0D1F0D;
            colorSelectBg = 0xFF14532D;
        } else if ("amber_crt".equals(themeId)) {
            colorBg = 0xFF120C02;
            colorText = 0xFFFBBF24;
            colorSecondary = 0xFFD97706;
            colorAccent = 0xFFFDE68A;
            colorBox = 0xFF241705;
            colorSelectBg = 0xFF78350F;
        } else if ("dark_navy".equals(themeId)) {
            colorBg = 0xFF0B0F19;
            colorText = 0xFFE2E8F0;
            colorSecondary = 0xFF94A3B8;
            colorAccent = 0xFF38BDF8;
            colorBox = 0xFF131D31;
            colorSelectBg = 0xFF1E293B;
        } else {
            // pure_black (default)
            colorBg = Color.BLACK;
            colorText = Color.WHITE;
            colorSecondary = 0xFFA1A1AA;
            colorAccent = 0xFFFCD34D;
            colorBox = 0xFF141414;
            colorSelectBg = 0xFF27272A;
        }

        if (rootLayout != null) rootLayout.setBackgroundColor(colorBg);
        if (userBadgeView != null) userBadgeView.setTextColor(colorAccent);
        if (focusBadgeView != null) focusBadgeView.setTextColor(colorSecondary);
        if (dateView != null) {
            dateView.setTextColor(colorText);
            dateView.setTextSize((int)(18 * fontScale));
        }
        if (timeView != null) {
            timeView.setTextColor(colorText);
            timeView.setTextSize((int)(36 * fontScale));
        }
        if (batteryView != null) {
            batteryView.setTextColor(colorSecondary);
            batteryView.setTextSize((int)(11 * fontScale));
        }
        if (prayerBox != null) applyCardBg(prayerBox, colorBox);
        if (prayerNextView != null) {
            prayerNextView.setTextColor(colorAccent);
            prayerNextView.setTextSize((int)(11 * fontScale));
        }
        if (prayerScheduleView != null) {
            prayerScheduleView.setTextColor(colorSecondary);
            prayerScheduleView.setTextSize((int)(10 * fontScale));
        }
        if (countdownBox != null) applyCardBg(countdownBox, colorBox);
        if (countdownTitleView != null) {
            countdownTitleView.setTextColor(colorAccent);
            countdownTitleView.setTextSize((int)(12 * fontScale));
        }
        if (countdownDaysView != null) {
            countdownDaysView.setTextColor(colorText);
            countdownDaysView.setTextSize((int)(14 * fontScale));
        }
        if (countdownProgressView != null) {
            countdownProgressView.setTextColor(colorSecondary);
            countdownProgressView.setTextSize((int)(11 * fontScale));
        }
        if (countdownBarView != null) {
            countdownBarView.setTextColor(colorAccent);
            countdownBarView.setTextSize((int)(10 * fontScale));
        }
        if (hintView != null) {
            hintView.setTextColor(colorSecondary);
            hintView.setTextSize((int)(10 * fontScale));
        }
    }

    private void applyCardBg(LinearLayout box, int bgCol) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgCol);
        gd.setCornerRadius(dp(6));
        gd.setStroke(dp(1), 0x33888888);
        box.setBackground(gd);
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void checkGpsLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    currentLat = loc.getLatitude();
                    currentLng = loc.getLongitude();
                }
            }
        } catch (SecurityException ignored) {}
    }

    private void buildUi() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int widthDp = (int) (dm.widthPixels / dm.density);
        int heightDp = (int) (dm.heightPixels / dm.density);
        boolean isSmallScreen = (widthDp <= 360 || heightDp <= 600);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(colorBg);

        int padX = isSmallScreen ? dp(10) : dp(16);
        int padY = isSmallScreen ? dp(8) : dp(16);
        rootLayout.setPadding(padX, padY, padX, padY);

        // Header: User avatar badge + Focus Mode badge
        LinearLayout topHeaderRow = new LinearLayout(this);
        topHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        topHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        userBadgeView = text(isSmallScreen ? 11 : 12, colorAccent, Typeface.DEFAULT_BOLD);
        SharedPreferences sp = getSharedPreferences("launcher", MODE_PRIVATE);
        userBadgeView.setText("👤 " + sp.getString("userName", "أحمد غنيمي"));
        topHeaderRow.addView(userBadgeView);

        TextView spacer = new TextView(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 1, 1.0f);
        spacer.setLayoutParams(spLp);
        topHeaderRow.addView(spacer);

        focusBadgeView = text(isSmallScreen ? 9 : 10, colorSecondary, Typeface.MONOSPACE);
        focusBadgeView.setText("🔒 وضع التركيز");
        topHeaderRow.addView(focusBadgeView);
        rootLayout.addView(topHeaderRow);

        // Date, Clock, Battery
        dateView = text(isSmallScreen ? 14 : 17, colorText, Typeface.DEFAULT);
        dateView.setPadding(0, dp(4), 0, 0);
        rootLayout.addView(dateView);

        timeView = text(isSmallScreen ? 32 : 40, colorText, Typeface.MONOSPACE);
        timeView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        rootLayout.addView(timeView);

        batteryView = text(isSmallScreen ? 10 : 11, colorSecondary, Typeface.MONOSPACE);
        batteryView.setPadding(0, 0, 0, dp(6));
        rootLayout.addView(batteryView);

        // 1. Prayer Times Box
        prayerBox = new LinearLayout(this);
        prayerBox.setOrientation(LinearLayout.VERTICAL);
        applyCardBg(prayerBox, colorBox);
        prayerBox.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams prParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        prParams.setMargins(0, dp(4), 0, dp(6));
        prayerBox.setLayoutParams(prParams);

        prayerNextView = text(isSmallScreen ? 10 : 12, colorAccent, Typeface.DEFAULT_BOLD);
        prayerScheduleView = text(isSmallScreen ? 9 : 10, colorSecondary, Typeface.MONOSPACE);
        prayerBox.addView(prayerNextView);
        prayerBox.addView(prayerScheduleView);

        // 2. Vacation & Duty Cycle Box
        countdownBox = new LinearLayout(this);
        countdownBox.setOrientation(LinearLayout.VERTICAL);
        applyCardBg(countdownBox, colorBox);
        countdownBox.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, dp(4), 0, dp(8));
        countdownBox.setLayoutParams(boxParams);

        countdownTitleView = text(isSmallScreen ? 11 : 12, colorAccent, Typeface.DEFAULT_BOLD);
        countdownDaysView = text(isSmallScreen ? 12 : 14, colorText, Typeface.DEFAULT_BOLD);
        countdownProgressView = text(isSmallScreen ? 10 : 11, colorSecondary, Typeface.DEFAULT);
        countdownBarView = text(isSmallScreen ? 9 : 10, colorAccent, Typeface.MONOSPACE);
        countdownBox.addView(countdownTitleView);
        countdownBox.addView(countdownDaysView);
        countdownBox.addView(countdownProgressView);
        countdownBox.addView(countdownBarView);

        // 3. Apps list container
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);

        // 4. Hint footer
        hintView = text(isSmallScreen ? 9 : 10, colorSecondary, Typeface.MONOSPACE);
        hintView.setGravity(Gravity.CENTER);
        hintView.setPadding(0, dp(10), 0, dp(6));
        hintView.setText("💡 اضغط مرتين على زر خفض الصوت لفك/قفل اللمس | اضغط مطولاً للإعدادات");

        reorderWidgets();

        ScrollView outerScroll = new ScrollView(this);
        outerScroll.setVerticalScrollBarEnabled(false);
        outerScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        outerScroll.setFillViewport(true);
        outerScroll.addView(rootLayout, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(outerScroll);
    }

    private void reorderWidgets() {
        rootLayout.removeView(prayerBox);
        rootLayout.removeView(countdownBox);
        rootLayout.removeView(listBox);
        rootLayout.removeView(hintView);

        if ("top".equals(prayerPosition)) rootLayout.addView(prayerBox);
        if ("top".equals(vacationPosition)) rootLayout.addView(countdownBox);

        rootLayout.addView(listBox);

        if ("bottom".equals(prayerPosition)) rootLayout.addView(prayerBox);
        if ("bottom".equals(vacationPosition)) rootLayout.addView(countdownBox);

        rootLayout.addView(hintView);
    }

    private TextView text(int sizeSp, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize((int)(sizeSp * fontScale));
        t.setTextColor(color);
        t.setTypeface(tf);
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void updateHeader() {
        Date now = new Date();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE d MMMM", new Locale("ar"));
        dateView.setText(dayFormat.format(now));

        if ("12h".equals(timeFormat)) {
            timeView.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(now));
        } else {
            timeView.setText(new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(now));
        }

        Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (bat != null) {
            int level = bat.getIntExtra("level", -1);
            int scale = bat.getIntExtra("scale", 100);
            if (level >= 0 && scale > 0) {
                batteryView.setText("🔋 " + (level * 100 / scale) + "% • البطارية");
            }
        }
    }

    private void updatePrayerTimes() {
        SharedPreferences sp = getSharedPreferences("launcher", MODE_PRIVATE);
        boolean showPrayer = sp.getBoolean("showPrayerTimes", true);
        if (!showPrayer) {
            prayerBox.setVisibility(View.GONE);
            return;
        }
        prayerBox.setVisibility(View.VISIBLE);

        PrayerCalculator.PrayerResult res = PrayerCalculator.calculate(currentLat, currentLng, 2.0);
        int h = res.remainingMinutes / 60;
        int m = res.remainingMinutes % 60;
        prayerNextView.setText("🕌 الصلاة القادمة: " + res.nextPrayerName + " (باقي " + (h > 0 ? h + " س و" : "") + m + " د)");
        prayerScheduleView.setText("الفجر " + res.fajr + " | الظهر " + res.dhuhr + " | العصر " + res.asr + " | المغرب " + res.maghrib + " | العشاء " + res.isha);
    }

    private void updateCountdown() {
        SharedPreferences sp = getSharedPreferences("launcher", MODE_PRIVATE);
        boolean enabled = sp.getBoolean("countdownEnabled", true);
        if (!enabled) {
            countdownBox.setVisibility(View.GONE);
            return;
        }
        countdownBox.setVisibility(View.VISIBLE);

        String userName = sp.getString("userName", "أحمد غنيمي");
        int userLevel = sp.getInt("userLevel", 3);
        int dutyDays = sp.getInt("dutyCycleDays", 20);
        int vacationDays = sp.getInt("vacationCycleDays", 10);
        int daysRemaining = sp.getInt("daysRemaining", 7);

        int daysServed = Math.max(0, dutyDays - daysRemaining);
        int percent = Math.min(100, Math.max(0, (daysServed * 100) / dutyDays));

        countdownTitleView.setText("★ " + userName + " | المستوى: " + userLevel);
        if (daysRemaining <= 0) {
            countdownDaysView.setText("🎉 مبروك يا أحمد! بدأت إجازة الـ " + vacationDays + " أيام! (ترقية جديدة ↑)");
            countdownProgressView.setText("تم إكمال دورة الـ " + dutyDays + " يوم خدمة بنجاح!");
            countdownBarView.setText("[■■■■■■■■■■■■■■■■■■■■] 100%");
        } else {
            countdownDaysView.setText("⚔ فاضل: " + daysRemaining + " يوم وتنزل إجازة الـ " + vacationDays + " أيام!");
            countdownProgressView.setText("الخدمة: " + daysServed + " من أصل " + dutyDays + " يوم (" + percent + "%)");

            // ASCII Visual Progress Bar
            int totalBlocks = 18;
            int filled = Math.min(totalBlocks, (daysServed * totalBlocks) / dutyDays);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < filled; i++) sb.append("■");
            for (int i = filled; i < totalBlocks; i++) sb.append("□");
            sb.append("] ").append(percent).append("%");
            countdownBarView.setText(sb.toString());
        }
    }

    private void loadApps(String saved) {
        listBox.removeAllViews();
        rows.clear();
        pkgs.clear();

        // 1. Built-in Phone app
        addAppRow("phone_internal", "📞 الهاتف والأسماء");

        // 2. Built-in Music Player
        addAppRow("music_internal", "🎵 مشغل الموسيقى");

        // 3. User configured apps
        PackageManager pm = getPackageManager();
        for (String p : saved.split(",")) {
            if (p.length() == 0 || p.equals("phone_internal") || p.equals("music_internal") || p.equals("settings_internal")) continue;
            try {
                String label = pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString();
                addAppRow(p, label);
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        // 4. Settings ALWAYS visible and accessible!
        addAppRow("settings_internal", "⚙️ الإعدادات والتحكم");

        if (selected >= rows.size()) selected = 0;
        refreshSelection();
    }

    private void addAppRow(String pkg, String label) {
        final TextView row = text(15, colorText, Typeface.DEFAULT);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setTag(label);

        // Click when touch lock is off
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int idx = rows.indexOf(v);
                if (idx != -1) {
                    selected = idx;
                    refreshSelection();
                    openSelected();
                }
            }
        });

        // Long click to open settings
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
        });

        listBox.addView(row);
        rows.add(row);
        pkgs.add(pkg);
    }

    private void refreshSelection() {
        for (int i = 0; i < rows.size(); i++) {
            TextView r = rows.get(i);
            String label = (String) r.getTag();
            if (i == selected) {
                r.setText("► " + label);
                GradientDrawable selBg = new GradientDrawable();
                selBg.setColor(colorSelectBg);
                selBg.setCornerRadius(dp(6));
                selBg.setStroke(dp(1), colorAccent);
                r.setBackground(selBg);
                r.setTextColor(colorText);
                r.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                r.setText("   " + label);
                r.setBackgroundColor(Color.TRANSPARENT);
                r.setTextColor(colorText);
                r.setTypeface(Typeface.DEFAULT);
            }
        }
    }

    private void move(int delta) {
        if (rows.isEmpty()) return;
        selected = (selected + delta + rows.size()) % rows.size();
        refreshSelection();
    }

    private void openSelected() {
        if (rows.isEmpty()) return;
        String pkg = pkgs.get(selected);
        if ("phone_internal".equals(pkg)) {
            startActivity(new Intent(this, SimplePhoneActivity.class));
            return;
        }
        if ("music_internal".equals(pkg)) {
            startActivity(new Intent(this, SimpleMusicActivity.class));
            return;
        }
        if ("settings_internal".equals(pkg)) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i);
    }

    private void toggleTouchLock() {
        touchLock = !touchLock;
        getSharedPreferences("launcher", MODE_PRIVATE).edit().putBoolean("touchLock", touchLock).apply();
        Toast.makeText(this, touchLock ? "تم قفل شاشة اللمس 🔒" : "تم فتح شاشة اللمس 🔓", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (touchLock) return true;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        long now = System.currentTimeMillis();

        // 1. Triple any key shortcut
        if ("triple_any".equals(lockToggleMode)) {
            if (now - lastAnyKeyPress < 450) {
                anyKeyHitCount++;
                if (anyKeyHitCount >= 2) {
                    toggleTouchLock();
                    anyKeyHitCount = 0;
                    return true;
                }
            } else {
                anyKeyHitCount = 0;
            }
            lastAnyKeyPress = now;
        }

        // 2. Double-click Back shortcut
        if (keyCode == KeyEvent.KEYCODE_BACK && "double_back".equals(lockToggleMode)) {
            if (now - lastBackPress < 450) {
                toggleTouchLock();
                lastBackPress = 0;
                return true;
            }
            lastBackPress = now;
            return true;
        }

        // 3. Double-click Volume Down shortcut
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && "double_vol_down".equals(lockToggleMode)) {
            if (now - lastKeyNextPress < 450) {
                toggleTouchLock();
                lastKeyNextPress = 0;
                return true;
            }
            lastKeyNextPress = now;
        }

        // 4. Double-click Volume Up shortcut
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && "double_vol_up".equals(lockToggleMode)) {
            if (now - lastKeyOpenPress < 450) {
                toggleTouchLock();
                lastKeyOpenPress = 0;
                return true;
            }
            lastKeyOpenPress = now;
        }

        // D-Pad and hardware keys navigation
        if (keyCode == keyNext || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            move(1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            move(-1);
            return true;
        }
        if (keyCode == keyOpen || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            openSelected();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
