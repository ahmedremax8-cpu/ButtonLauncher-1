package com.example.buttonlauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout rootLayout;
    private TextView dateView, timeView, batteryView, hintView;
    private LinearLayout countdownBox;
    private TextView countdownTitleView, countdownDaysView, countdownProgressView;
    private TextView prayerWidgetView;
    private LinearLayout listBox;
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
    private String themeId = "pure_black"; // pure_black, e_ink_light, matrix_green, amber_crt, dark_navy
    private float fontScale = 1.0f; // 0.85f, 1.0f, 1.2f, 1.4f

    private int colorBg = Color.BLACK;
    private int colorText = Color.WHITE;
    private int colorSecondary = Color.LTGRAY;
    private int colorAccent = 0xFFFCD34D;
    private int colorBox = 0xFF161616;
    private int colorSelectBg = 0xFF333333;

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
            colorSecondary = 0xFF444444;
            colorAccent = 0xFF000000;
            colorBox = 0xFFEEEEEE;
            colorSelectBg = 0xFFDDDDDD;
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
            colorBox = 0xFF161E2E;
            colorSelectBg = 0xFF1E293B;
        } else {
            // pure_black
            colorBg = Color.BLACK;
            colorText = Color.WHITE;
            colorSecondary = Color.LTGRAY;
            colorAccent = 0xFFFCD34D;
            colorBox = 0xFF161616;
            colorSelectBg = 0xFF333333;
        }

        if (rootLayout != null) rootLayout.setBackgroundColor(colorBg);
        if (dateView != null) {
            dateView.setTextColor(colorText);
            dateView.setTextSize((int)(20 * fontScale));
        }
        if (timeView != null) {
            timeView.setTextColor(colorText);
            timeView.setTextSize((int)(38 * fontScale));
        }
        if (batteryView != null) {
            batteryView.setTextColor(colorSecondary);
            batteryView.setTextSize((int)(12 * fontScale));
        }
        if (prayerWidgetView != null) {
            prayerWidgetView.setTextColor(colorAccent);
            prayerWidgetView.setTextSize((int)(12 * fontScale));
        }
        if (countdownBox != null) countdownBox.setBackgroundColor(colorBox);
        if (countdownTitleView != null) {
            countdownTitleView.setTextColor(colorAccent);
            countdownTitleView.setTextSize((int)(13 * fontScale));
        }
        if (countdownDaysView != null) {
            countdownDaysView.setTextColor(colorText);
            countdownDaysView.setTextSize((int)(15 * fontScale));
        }
        if (countdownProgressView != null) {
            countdownProgressView.setTextColor(colorSecondary);
            countdownProgressView.setTextSize((int)(12 * fontScale));
        }
        if (hintView != null) {
            hintView.setTextColor(colorSecondary);
            hintView.setTextSize((int)(11 * fontScale));
        }
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
        boolean isSmallScreen = (widthDp <= 360 || heightDp <= 600); // 2.8" & 3.0" devices (Qin F21/Cat S22/Jelly 2)

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(colorBg);

        int padX = isSmallScreen ? dp(12) : dp(22);
        int padY = isSmallScreen ? dp(12) : dp(24);
        rootLayout.setPadding(padX, padY, padX, padX);

        int baseDate = isSmallScreen ? 16 : 20;
        int baseTime = isSmallScreen ? 30 : 38;
        int baseBat = isSmallScreen ? 11 : 12;

        // Header: Date, Time, Battery
        dateView = text(baseDate, colorText, Typeface.MONOSPACE);
        timeView = text(baseTime, colorText, Typeface.MONOSPACE);
        batteryView = text(baseBat, colorSecondary, Typeface.MONOSPACE);
        rootLayout.addView(dateView);
        rootLayout.addView(timeView);
        rootLayout.addView(batteryView);

        // Prayer Times Banner
        prayerWidgetView = text(isSmallScreen ? 10 : 12, colorAccent, Typeface.MONOSPACE);
        prayerWidgetView.setPadding(0, dp(4), 0, dp(2));

        // Vacation & Level Box
        countdownBox = new LinearLayout(this);
        countdownBox.setOrientation(LinearLayout.VERTICAL);
        countdownBox.setBackgroundColor(colorBox);
        int boxPadX = isSmallScreen ? dp(8) : dp(12);
        int boxPadY = isSmallScreen ? dp(6) : dp(9);
        countdownBox.setPadding(boxPadX, boxPadY, boxPadX, boxPadY);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, isSmallScreen ? dp(4) : dp(8), 0, isSmallScreen ? dp(6) : dp(12));
        countdownBox.setLayoutParams(boxParams);

        countdownTitleView = text(isSmallScreen ? 11 : 13, colorAccent, Typeface.MONOSPACE);
        countdownDaysView = text(isSmallScreen ? 13 : 15, colorText, Typeface.MONOSPACE);
        countdownProgressView = text(isSmallScreen ? 10 : 12, colorSecondary, Typeface.MONOSPACE);
        countdownBox.addView(countdownTitleView);
        countdownBox.addView(countdownDaysView);
        countdownBox.addView(countdownProgressView);

        // Apps list container
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);

        // Hint footer
        hintView = text(isSmallScreen ? 9 : 11, colorSecondary, Typeface.MONOSPACE);
        hintView.setGravity(Gravity.BOTTOM);

        reorderWidgets();

        // Wrap in ScrollView without scrollbars for zero clipping on 2.8" and 3" screens
        android.widget.ScrollView outerScroll = new android.widget.ScrollView(this);
        outerScroll.setVerticalScrollBarEnabled(false);
        outerScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        outerScroll.setFillViewport(true);
        outerScroll.addView(rootLayout, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(outerScroll);
    }

    private void reorderWidgets() {
        rootLayout.removeView(prayerWidgetView);
        rootLayout.removeView(countdownBox);
        rootLayout.removeView(listBox);
        rootLayout.removeView(hintView);

        if ("top".equals(prayerPosition)) rootLayout.addView(prayerWidgetView);
        if ("top".equals(vacationPosition)) rootLayout.addView(countdownBox);

        rootLayout.addView(listBox);

        if ("bottom".equals(prayerPosition)) rootLayout.addView(prayerWidgetView);
        if ("bottom".equals(vacationPosition)) rootLayout.addView(countdownBox);

        rootLayout.addView(hintView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private TextView text(int sp, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize((int)(sp * fontScale));
        t.setTextColor(color);
        t.setTypeface(tf);
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void updateHeader() {
        Date now = new Date();
        dateView.setText("D: " + new SimpleDateFormat("dd MMM", Locale.ENGLISH).format(now));

        if ("12h".equals(timeFormat)) {
            timeView.setText("T: " + new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(now));
        } else {
            timeView.setText("T: " + new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(now));
        }

        Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (bat != null) {
            int level = bat.getIntExtra("level", -1);
            int scale = bat.getIntExtra("scale", 100);
            if (level >= 0 && scale > 0) {
                batteryView.setText("B: " + (level * 100 / scale) + "%");
            }
        }
    }

    private void updatePrayerTimes() {
        SharedPreferences sp = getSharedPreferences("launcher", MODE_PRIVATE);
        boolean showPrayer = sp.getBoolean("showPrayerTimes", true);
        if (!showPrayer) {
            prayerWidgetView.setVisibility(View.GONE);
            return;
        }
        prayerWidgetView.setVisibility(View.VISIBLE);

        PrayerCalculator.PrayerResult res = PrayerCalculator.calculate(currentLat, currentLng, 2.0);
        int h = res.remainingMinutes / 60;
        int m = res.remainingMinutes % 60;
        prayerWidgetView.setText("🕌 الصلاة: " + res.nextPrayerName + " (باقي " + (h > 0 ? h + " س و" : "") + m + " د)");
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
        } else {
            countdownDaysView.setText("⚔ فاضل: " + daysRemaining + " يوم وتنزل إجازة الـ " + vacationDays + " أيام");
            countdownProgressView.setText("الخدمة: " + daysServed + "/" + dutyDays + " يوم (" + percent + "%)");
        }
    }

    private void loadApps(String saved) {
        listBox.removeAllViews();
        rows.clear();
        pkgs.clear();

        addAppRow("phone_internal", "الهاتف والأسماء");
        addAppRow("music_internal", "مشغل الموسيقى");

        PackageManager pm = getPackageManager();
        for (String p : saved.split(",")) {
            if (p.length() == 0 || p.equals("phone_internal") || p.equals("music_internal")) continue;
            try {
                String label = pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString();
                addAppRow(p, label);
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        if (selected >= rows.size()) selected = 0;
        refreshSelection();
    }

    private void addAppRow(String pkg, String label) {
        TextView row = text(18, colorText, Typeface.MONOSPACE);
        row.setPadding(dp(8), dp(7), dp(8), dp(7));
        row.setTag(label);
        listBox.addView(row);
        rows.add(row);
        pkgs.add(pkg);
    }

    private void refreshSelection() {
        for (int i = 0; i < rows.size(); i++) {
            TextView r = rows.get(i);
            String label = (String) r.getTag();
            if (i == selected) {
                r.setText("> " + label);
                r.setBackgroundColor(colorSelectBg);
                r.setTextColor(colorText);
            } else {
                r.setText("  " + label);
                r.setBackgroundColor(Color.TRANSPARENT);
                r.setTextColor(colorText);
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
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i);
    }

    private void toggleTouchLock() {
        touchLock = !touchLock;
        getSharedPreferences("launcher", MODE_PRIVATE).edit().putBoolean("touchLock", touchLock).apply();
        Toast.makeText(this, touchLock ? "تم قفل اللمس 🔒" : "تم فتح اللمس 🔓", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (touchLock) return true;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        long now = System.currentTimeMillis();

        // Check for 3 quick hits on ANY key if user selected that mode
        if ("any_key_triple".equals(lockToggleMode)) {
            if (now - lastAnyKeyPress < 450) {
                anyKeyHitCount++;
                if (anyKeyHitCount >= 2) {
                    toggleTouchLock();
                    anyKeyHitCount = 0;
                    return true;
                }
            } else {
                anyKeyHitCount = 1;
            }
            lastAnyKeyPress = now;
        }

        if (keyCode == keyNext) {
            if ("double_vol_down".equals(lockToggleMode) && (now - lastKeyNextPress < 450)) {
                toggleTouchLock();
                return true;
            }
            lastKeyNextPress = now;
            if (event.getRepeatCount() == 0) event.startTracking();
            return true;
        }

        if (keyCode == keyOpen) {
            if ("double_vol_up".equals(lockToggleMode) && (now - lastKeyOpenPress < 450)) {
                toggleTouchLock();
                return true;
            }
            lastKeyOpenPress = now;
            if (event.getRepeatCount() == 0) event.startTracking();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ("double_back".equals(lockToggleMode) && (now - lastBackPress < 450)) {
                toggleTouchLock();
                return true;
            }
            lastBackPress = now;
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if ("long_vol_up".equals(lockToggleMode) && keyCode == keyOpen) {
            toggleTouchLock();
            return true;
        }
        if (keyCode == keyNext) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == keyNext) {
            if (!event.isCanceled()) move(1);
            return true;
        }
        if (keyCode == keyOpen) {
            openSelected();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
