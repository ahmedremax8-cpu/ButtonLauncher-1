package com.example.buttonlauncher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SetupWizardActivity extends Activity {

    private static final int PERMISSION_REQ_CODE = 200;

    private TextView statusPhonePerm;
    private TextView statusAudioPerm;
    private TextView statusGpsPerm;

    private EditText nameInput;
    private EditText dutyDaysInput;
    private EditText vacDaysInput;
    private EditText daysRemainingInput;
    private EditText levelInput;

    private Spinner themeSpinner;
    private Spinner fontScaleSpinner;
    private Spinner timeFormatSpinner;
    private Spinner lockShortcutSpinner;
    private CheckBox lockDefaultBox;
    private CheckBox showPrayerBox;
    private CheckBox showVacationBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final SharedPreferences sp = getSharedPreferences("launcher", MODE_PRIVATE);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0A0A0A);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scrollView.addView(root);

        // Header
        TextView title = new TextView(this);
        title.setText("⚙️ إعداد وتشغيل Button Launcher");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("مرحباً بك! اضبط الصلاحيات والإعدادات لتناسب هاتفك 2.8\" أو 3\" أو أي شاشة.");
        subtitle.setTextColor(0xFFAAAAAA);
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, dp(14));
        root.addView(subtitle);

        // 1. Permissions Card
        LinearLayout permCard = makeCard("1. الأذونات والصلاحيات المطلوبة (Permissions)");
        TextView permDesc = new TextView(this);
        permDesc.setText("يحتاج اللانشر لهذه الأذونات لتشغيل تطبيق الهاتف، مشغل الموسيقى، وحساب مواقيت الصلاة بدقة GPS:");
        permDesc.setTextColor(0xFFCCCCCC);
        permDesc.setTextSize(11);
        permCard.addView(permDesc);

        statusPhonePerm = makeStatusRow(permCard, "📞 جهات الاتصال والمكالمات (الهاتف)");
        statusAudioPerm = makeStatusRow(permCard, "🎵 الملفات الصوتية (مشغل الموسيقى)");
        statusGpsPerm = makeStatusRow(permCard, "📍 الموقع الجغرافي GPS (مواقيت الصلاة)");

        Button btnGrant = new Button(this);
        btnGrant.setText("🔓 منح جميع الأذونات الآن");
        btnGrant.setBackgroundColor(0xFF16A34A);
        btnGrant.setTextColor(Color.WHITE);
        btnGrant.setTextSize(13);
        btnGrant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestRequiredPermissions();
            }
        });
        permCard.addView(btnGrant);
        root.addView(permCard);

        // 2. Military Duty & Vacation Cycle Card
        LinearLayout vacCard = makeCard("2. إعدادات عداد الخدمة والإجازة (أحمد غنيمي)");
        
        nameInput = addField(vacCard, "اسم المستخدم المعروض في الواجهة:", sp.getString("userName", "أحمد غنيمي"));
        dutyDaysInput = addField(vacCard, "عدد أيام الخدمة في الدورة (مثلاً: 20):", String.valueOf(sp.getInt("dutyCycleDays", 20)));
        vacDaysInput = addField(vacCard, "عدد أيام الإجازة في الدورة (مثلاً: 10):", String.valueOf(sp.getInt("vacationCycleDays", 10)));
        daysRemainingInput = addField(vacCard, "الأيام المتبقية حتى الإجازة القادمة:", String.valueOf(sp.getInt("daysRemaining", 7)));
        levelInput = addField(vacCard, "رتبة / مستوى الحساب (Level):", String.valueOf(sp.getInt("userLevel", 3)));

        showVacationBox = new CheckBox(this);
        showVacationBox.setText("إظهار ويدجت عداد الإجازة في الواجهة الرئيسية");
        showVacationBox.setTextColor(Color.WHITE);
        showVacationBox.setChecked(sp.getBoolean("countdownEnabled", true));
        vacCard.addView(showVacationBox);

        root.addView(vacCard);

        // 3. Theme & Screen Scaling Card
        LinearLayout themeCard = makeCard("3. ثيم الألوان وحجم الخط وتوافق الشاشات");

        addLabel(themeCard, "اختر ثيم الألوان للواجهة:");
        themeSpinner = new Spinner(this);
        themeSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
                "أحادي اللون أسود نقي (Pure OLED Black) - كلاسيكي",
                "حبر إلكتروني أبيض وتباين عالي (E-Ink High Contrast)",
                "أخضر ماتريكس وتيرمينال قديم (Matrix Cyber Green)",
                "عنبري دافئ كلاسيكي (Amber CRT Retro)",
                "كحلي ليلي هادئ (Dark Navy Slate)"
        }));
        setSpinnerSelectionByTag(themeSpinner, sp.getString("themeId", "pure_black"), new String[]{
                "pure_black", "e_ink_light", "matrix_green", "amber_crt", "dark_navy"
        });
        themeCard.addView(themeSpinner);

        addLabel(themeCard, "حجم الخط وتوافق الشاشات (2.8\" و 3.0\" و شاشات عادية):");
        fontScaleSpinner = new Spinner(this);
        fontScaleSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
                "شاشات صغيرة 2.8\" و 3.0\" مدمج 85% (Qin F21 / Cat S22 / Jelly 2)",
                "شاشة عادية قياسية (100% Normal)",
                "خط كبير مريح (120% Large)",
                "خط ضخم جداً (140% Extra Large)"
        }));
        float currentScale = sp.getFloat("fontScale", 1.0f);
        if (Math.abs(currentScale - 0.85f) < 0.05f) fontScaleSpinner.setSelection(0);
        else if (Math.abs(currentScale - 1.2f) < 0.05f) fontScaleSpinner.setSelection(2);
        else if (Math.abs(currentScale - 1.4f) < 0.05f) fontScaleSpinner.setSelection(3);
        else fontScaleSpinner.setSelection(1);
        themeCard.addView(fontScaleSpinner);

        addLabel(themeCard, "صيغة الساعة:");
        timeFormatSpinner = new Spinner(this);
        timeFormatSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
                "12 ساعة (ص / م)",
                "24 ساعة عسكرية"
        }));
        timeFormatSpinner.setSelection("12h".equals(sp.getString("timeFormat", "24h")) ? 0 : 1);
        themeCard.addView(timeFormatSpinner);

        showPrayerBox = new CheckBox(this);
        showPrayerBox.setText("إظهار ويدجت مواقيت الصلاة الفلكية حسب الـ GPS");
        showPrayerBox.setTextColor(Color.WHITE);
        showPrayerBox.setChecked(sp.getBoolean("showPrayerTimes", true));
        themeCard.addView(showPrayerBox);

        root.addView(themeCard);

        // 4. Touch Lock & Hardware Shortcuts
        LinearLayout lockCard = makeCard("4. قفل اللمس واختصارات الأزرار الفيزيائية");

        lockDefaultBox = new CheckBox(this);
        lockDefaultBox.setText("تفعيل قفل شاشة اللمس تلقائياً (التحكم بالأزرار فقط)");
        lockDefaultBox.setTextColor(Color.WHITE);
        lockDefaultBox.setChecked(sp.getBoolean("touchLock", true));
        lockCard.addView(lockDefaultBox);

        addLabel(lockCard, "زر / اختصار فك وقفل شاشة اللمس السريع:");
        lockShortcutSpinner = new Spinner(this);
        lockShortcutSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
                "ضغطتان متتاليتان على خفض الصوت (Double-click Vol-) [موصى به]",
                "ضغطتان متتاليتان على رفع الصوت (Double-click Vol+)",
                "ضغطتان متتاليتان على زر الرجوع (Double-click Back)",
                "3 ضغطات سريعة على أي زر هاردوير في الهاتف"
        }));
        setSpinnerSelectionByTag(lockShortcutSpinner, sp.getString("lockToggleMode", "double_vol_down"), new String[]{
                "double_vol_down", "double_vol_up", "double_back", "triple_any"
        });
        lockCard.addView(lockShortcutSpinner);

        root.addView(lockCard);

        // Finish & Save Button
        Button btnSaveAndLaunch = new Button(this);
        btnSaveAndLaunch.setText("💾 حفظ الإعدادات وبدء تشغيل اللانشر فوراً");
        btnSaveAndLaunch.setBackgroundColor(0xFF2563EB);
        btnSaveAndLaunch.setTextColor(Color.WHITE);
        btnSaveAndLaunch.setTextSize(14);
        btnSaveAndLaunch.setTypeface(Typeface.DEFAULT_BOLD);
        btnSaveAndLaunch.setPadding(0, dp(12), 0, dp(12));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, dp(10), 0, dp(20));
        btnSaveAndLaunch.setLayoutParams(btnParams);

        btnSaveAndLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllSettings(sp);
            }
        });
        root.addView(btnSaveAndLaunch);

        setContentView(scrollView);
        updatePermissionStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms;
            if (Build.VERSION.SDK_INT >= 33) {
                perms = new String[]{
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };
            } else {
                perms = new String[]{
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };
            }
            requestPermissions(perms, PERMISSION_REQ_CODE);
        } else {
            Toast.makeText(this, "تم منح جميع الصلاحيات تلقائياً في هذا الإصدار!", Toast.LENGTH_SHORT).show();
            updatePermissionStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        boolean hasPhone = checkPerm(Manifest.permission.READ_CONTACTS) && checkPerm(Manifest.permission.CALL_PHONE);
        boolean hasAudio = Build.VERSION.SDK_INT >= 33 
                ? checkPerm(Manifest.permission.READ_MEDIA_AUDIO)
                : checkPerm(Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean hasGps = checkPerm(Manifest.permission.ACCESS_FINE_LOCATION) || checkPerm(Manifest.permission.ACCESS_COARSE_LOCATION);

        setStatus(statusPhonePerm, hasPhone);
        setStatus(statusAudioPerm, hasAudio);
        setStatus(statusGpsPerm, hasGps);
    }

    private boolean checkPerm(String p) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void setStatus(TextView tv, boolean granted) {
        if (tv == null) return;
        if (granted) {
            tv.setText("✔️ تم المنح بنجاح");
            tv.setTextColor(0xFF4ADE80);
        } else {
            tv.setText("⚠️ لم يتم المنح بعد (اضغط الزر بالأعلى)");
            tv.setTextColor(0xFFFBBF24);
        }
    }

    private void saveAllSettings(SharedPreferences sp) {
        SharedPreferences.Editor ed = sp.edit();

        // 1. Mark setup completed so launcher opens normally next time
        ed.putBoolean("setup_completed", true);

        // 2. User & Vacation Cycle
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) name = "أحمد غنيمي";
        ed.putString("userName", name);

        try {
            ed.putInt("dutyCycleDays", Integer.parseInt(dutyDaysInput.getText().toString().trim()));
            ed.putInt("vacationCycleDays", Integer.parseInt(vacDaysInput.getText().toString().trim()));
            ed.putInt("daysRemaining", Integer.parseInt(daysRemainingInput.getText().toString().trim()));
            ed.putInt("userLevel", Integer.parseInt(levelInput.getText().toString().trim()));
        } catch (NumberFormatException ignored) {}

        ed.putBoolean("countdownEnabled", showVacationBox.isChecked());

        // 3. Theme
        String[] themes = {"pure_black", "e_ink_light", "matrix_green", "amber_crt", "dark_navy"};
        int themeIdx = themeSpinner.getSelectedItemPosition();
        if (themeIdx >= 0 && themeIdx < themes.length) ed.putString("themeId", themes[themeIdx]);

        // 4. Font Scale
        int scaleIdx = fontScaleSpinner.getSelectedItemPosition();
        float[] scales = {0.85f, 1.0f, 1.2f, 1.4f};
        if (scaleIdx >= 0 && scaleIdx < scales.length) ed.putFloat("fontScale", scales[scaleIdx]);

        // 5. Time Format
        ed.putString("timeFormat", timeFormatSpinner.getSelectedItemPosition() == 0 ? "12h" : "24h");
        ed.putBoolean("showPrayerTimes", showPrayerBox.isChecked());

        // 6. Touch Lock
        ed.putBoolean("touchLock", lockDefaultBox.isChecked());
        String[] lockModes = {"double_vol_down", "double_vol_up", "double_back", "triple_any"};
        int lockIdx = lockShortcutSpinner.getSelectedItemPosition();
        if (lockIdx >= 0 && lockIdx < lockModes.length) ed.putString("lockToggleMode", lockModes[lockIdx]);

        ed.apply();

        Toast.makeText(this, "تم حفظ الإعدادات بنجاح! جاري تشغيل اللانشر...", Toast.LENGTH_SHORT).show();

        Intent launcher = new Intent(this, MainActivity.class);
        launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(launcher);
        finish();
    }

    private LinearLayout makeCard(String headerTitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF161616);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), 0xFF2A2A2A);
        card.setBackground(bg);
        int p = dp(12);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        TextView head = new TextView(this);
        head.setText(headerTitle);
        head.setTextColor(0xFF38BDF8);
        head.setTextSize(13);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setPadding(0, 0, 0, dp(8));
        card.addView(head);

        return card;
    }

    private TextView makeStatusRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(Color.WHITE);
        lbl.setTextSize(11);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lbl.setLayoutParams(lp);
        row.addView(lbl);

        TextView status = new TextView(this);
        status.setText("...");
        status.setTextSize(10);
        row.addView(status);

        parent.addView(row);
        return status;
    }

    private EditText addField(LinearLayout parent, String label, String defValue) {
        addLabel(parent, label);
        EditText et = new EditText(this);
        et.setText(defValue);
        et.setTextColor(Color.WHITE);
        et.setTextSize(12);
        et.setBackgroundColor(0xFF222222);
        et.setPadding(dp(8), dp(6), dp(8), dp(6));
        parent.addView(et);
        return et;
    }

    private void addLabel(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFAAAAAA);
        tv.setTextSize(11);
        tv.setPadding(0, dp(6), 0, dp(2));
        parent.addView(tv);
    }

    private void setSpinnerSelectionByTag(Spinner spinner, String currentTag, String[] tags) {
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equals(currentTag)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
