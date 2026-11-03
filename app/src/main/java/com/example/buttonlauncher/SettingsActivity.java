package com.example.buttonlauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends Activity {

    private static class Entry {
        String pkg;
        String label;
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private ListView listView;
    private CheckBox lockBox;
    private CheckBox countdownBox;
    private CheckBox prayerBox;
    private EditText nameInput;
    private EditText levelInput;
    private EditText daysRemainingInput;

    private Spinner themeSpinner;
    private Spinner fontScaleSpinner;
    private Spinner lockTriggerSpinner;
    private Spinner timeFormatSpinner;
    private Spinner vacationPosSpinner;
    private Spinner prayerPosSpinner;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final SharedPreferences sp = getSharedPreferences("launcher", MODE_PRIVATE);
        Set<String> chosen = new HashSet<String>(Arrays.asList(sp.getString("apps", "").split(",")));

        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        Set<String> seen = new HashSet<String>();
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
            Entry e = new Entry();
            e.pkg = pkg;
            e.label = ri.loadLabel(pm).toString();
            entries.add(e);
        }
        Collections.sort(entries, new Comparator<Entry>() {
            public int compare(Entry a, Entry c) {
                return a.label.compareToIgnoreCase(c.label);
            }
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        content.setPadding(pad, pad, pad, pad);

        // 1. Color Themes
        TextView hTheme = new TextView(this);
        hTheme.setText("--- ثيم الألوان والمظهر المينيمالي ---");
        hTheme.setTextSize(14);
        content.addView(hTheme);

        TextView themeLbl = new TextView(this);
        themeLbl.setText("اختر ثيم الألوان للواجهة:");
        content.addView(themeLbl);

        themeSpinner = new Spinner(this);
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "أحادي اللون أسود نقي (Pure OLED Black) - كلاسيكي",
                        "تباين عالي / حبر إلكتروني أبيض (E-Ink High Contrast)",
                        "أخضر ماتريكس وتيرمينال قديم (Matrix Cyber Green)",
                        "عنبري دافئ كلاسيكي (Amber CRT Retro)",
                        "كحلي ليلي هادئ (Dark Navy Slate)"
                });
        themeSpinner.setAdapter(themeAdapter);
        String currentTheme = sp.getString("themeId", "pure_black");
        if ("e_ink_light".equals(currentTheme)) themeSpinner.setSelection(1);
        else if ("matrix_green".equals(currentTheme)) themeSpinner.setSelection(2);
        else if ("amber_crt".equals(currentTheme)) themeSpinner.setSelection(3);
        else if ("dark_navy".equals(currentTheme)) themeSpinner.setSelection(4);
        content.addView(themeSpinner);

        // 2. Font Scaling
        TextView hFont = new TextView(this);
        hFont.setText("\n--- حجم الخط (تكبير أو تصغير) ---");
        hFont.setTextSize(14);
        content.addView(hFont);

        fontScaleSpinner = new Spinner(this);
        ArrayAdapter<String> fontAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "خط صغير مينيمالي (85%)",
                        "خط عادي قياسي (100%)",
                        "خط كبير واضح (120%)",
                        "خط ضخم لكبار السن وضعاف البصر (140%)"
                });
        fontScaleSpinner.setAdapter(fontAdapter);
        float currentScale = sp.getFloat("fontScale", 1.0f);
        if (currentScale < 0.9f) fontScaleSpinner.setSelection(0);
        else if (currentScale > 1.3f) fontScaleSpinner.setSelection(3);
        else if (currentScale > 1.1f) fontScaleSpinner.setSelection(2);
        else fontScaleSpinner.setSelection(1);
        content.addView(fontScaleSpinner);

        // 3. Universal Touch Lock Key Shortcuts
        TextView hLock = new TextView(this);
        hLock.setText("\n--- تخصيص شورتكت وزر قفل وفتح اللمس ---");
        hLock.setTextSize(14);
        content.addView(hLock);

        lockBox = new CheckBox(this);
        lockBox.setText("قفل اللمس مفعّل حالياً (Touch Lock)");
        lockBox.setChecked(sp.getBoolean("touchLock", true));
        content.addView(lockBox);

        TextView lockLbl = new TextView(this);
        lockLbl.setText("اختر الزر أو الشورتكت لقفل وفتح اللمس فوراً:");
        content.addView(lockLbl);

        lockTriggerSpinner = new Spinner(this);
        ArrayAdapter<String> trigAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "ضغطتان سريعتان على زر خفض الصوت (Double Vol-)",
                        "ضغطتان سريعتان على زر رفع الصوت (Double Vol+)",
                        "ضغط مطول على زر رفع الصوت (Long-press Vol+)",
                        "ضغطتان سريعتان على زر الرجوع (Double Back)",
                        "3 نقرات سريعة متتالية على أي زر في الهاتف (Triple Any Key)"
                });
        lockTriggerSpinner.setAdapter(trigAdapter);
        String curTrig = sp.getString("lockToggleMode", "double_vol_down");
        if ("double_vol_up".equals(curTrig)) lockTriggerSpinner.setSelection(1);
        else if ("long_vol_up".equals(curTrig)) lockTriggerSpinner.setSelection(2);
        else if ("double_back".equals(curTrig)) lockTriggerSpinner.setSelection(3);
        else if ("any_key_triple".equals(curTrig)) lockTriggerSpinner.setSelection(4);
        content.addView(lockTriggerSpinner);

        // 4. Time Format
        TextView hTime = new TextView(this);
        hTime.setText("\n--- نظام الوقت والساعة ---");
        hTime.setTextSize(14);
        content.addView(hTime);

        timeFormatSpinner = new Spinner(this);
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"توقيت 24 ساعة (14:30)", "توقيت 12 ساعة (02:30 م)"});
        timeFormatSpinner.setAdapter(timeAdapter);
        if ("12h".equals(sp.getString("timeFormat", "24h"))) timeFormatSpinner.setSelection(1);
        content.addView(timeFormatSpinner);

        // 5. User & Vacation Widget
        TextView hUser = new TextView(this);
        hUser.setText("\n--- أحمد غنيمي ودورة الـ 20 يوم ---");
        hUser.setTextSize(14);
        content.addView(hUser);

        countdownBox = new CheckBox(this);
        countdownBox.setText("تفعيل ويدجت دورة الإجازة والترقية");
        countdownBox.setChecked(sp.getBoolean("countdownEnabled", true));
        content.addView(countdownBox);

        TextView nameLbl = new TextView(this);
        nameLbl.setText("اسم المستخدم:");
        content.addView(nameLbl);
        nameInput = new EditText(this);
        nameInput.setText(sp.getString("userName", "أحمد غنيمي"));
        content.addView(nameInput);

        TextView lvlLbl = new TextView(this);
        lvlLbl.setText("المستوى الحالي:");
        content.addView(lvlLbl);
        levelInput = new EditText(this);
        levelInput.setText(String.valueOf(sp.getInt("userLevel", 3)));
        content.addView(levelInput);

        TextView remLbl = new TextView(this);
        remLbl.setText("الأيام المتبقية حتى الإجازة:");
        content.addView(remLbl);
        daysRemainingInput = new EditText(this);
        daysRemainingInput.setText(String.valueOf(sp.getInt("daysRemaining", 7)));
        content.addView(daysRemainingInput);

        // 6. Widget Positioning
        TextView hPos = new TextView(this);
        hPos.setText("\n--- موضع الويدجات ---");
        hPos.setTextSize(14);
        content.addView(hPos);

        TextView vacPosLbl = new TextView(this);
        vacPosLbl.setText("موضع ويدجت دورة الإجازة:");
        content.addView(vacPosLbl);
        vacationPosSpinner = new Spinner(this);
        vacationPosSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"أعلى الشاشة (فوق التطبيقات)", "أسفل الشاشة (تحت التطبيقات)"}));
        if ("bottom".equals(sp.getString("vacationPosition", "top"))) vacationPosSpinner.setSelection(1);
        content.addView(vacationPosSpinner);

        TextView prayPosLbl = new TextView(this);
        prayPosLbl.setText("موضع ويدجت مواقيت الصلاة:");
        content.addView(prayPosLbl);
        prayerPosSpinner = new Spinner(this);
        prayerPosSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"أعلى الشاشة (فوق التطبيقات)", "أسفل الشاشة (تحت التطبيقات)"}));
        if ("bottom".equals(sp.getString("prayerPosition", "top"))) prayerPosSpinner.setSelection(1);
        content.addView(prayerPosSpinner);

        prayerBox = new CheckBox(this);
        prayerBox.setText("إظهار ويدجت مواقيت الصلاة");
        prayerBox.setChecked(sp.getBoolean("showPrayerTimes", true));
        content.addView(prayerBox);

        // Save
        Button save = new Button(this);
        save.setText("حفظ كافة الإعدادات (Save)");
        save.setBackgroundColor(0xFF16A34A);
        save.setTextColor(0xFFFFFFFF);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int level = 3;
                int remaining = 7;
                try {
                    level = Integer.parseInt(levelInput.getText().toString().trim());
                    remaining = Integer.parseInt(daysRemainingInput.getText().toString().trim());
                } catch (Exception ignored) {}

                String theme = "pure_black";
                int themePos = themeSpinner.getSelectedItemPosition();
                if (themePos == 1) theme = "e_ink_light";
                else if (themePos == 2) theme = "matrix_green";
                else if (themePos == 3) theme = "amber_crt";
                else if (themePos == 4) theme = "dark_navy";

                float fScale = 1.0f;
                int fontPos = fontScaleSpinner.getSelectedItemPosition();
                if (fontPos == 0) fScale = 0.85f;
                else if (fontPos == 1) fScale = 1.0f;
                else if (fontPos == 2) fScale = 1.2f;
                else if (fontPos == 3) fScale = 1.4f;

                String trigger = "double_vol_down";
                int trigPos = lockTriggerSpinner.getSelectedItemPosition();
                if (trigPos == 1) trigger = "double_vol_up";
                else if (trigPos == 2) trigger = "long_vol_up";
                else if (trigPos == 3) trigger = "double_back";
                else if (trigPos == 4) trigger = "any_key_triple";

                String tFmt = (timeFormatSpinner.getSelectedItemPosition() == 1) ? "12h" : "24h";
                String vPos = (vacationPosSpinner.getSelectedItemPosition() == 1) ? "bottom" : "top";
                String pPos = (prayerPosSpinner.getSelectedItemPosition() == 1) ? "bottom" : "top";

                sp.edit()
                        .putString("themeId", theme)
                        .putFloat("fontScale", fScale)
                        .putString("lockToggleMode", trigger)
                        .putString("timeFormat", tFmt)
                        .putString("vacationPosition", vPos)
                        .putString("prayerPosition", pPos)
                        .putBoolean("touchLock", lockBox.isChecked())
                        .putBoolean("countdownEnabled", countdownBox.isChecked())
                        .putBoolean("showPrayerTimes", prayerBox.isChecked())
                        .putString("userName", nameInput.getText().toString())
                        .putInt("userLevel", level)
                        .putInt("daysRemaining", remaining)
                        .commit();

                Toast.makeText(SettingsActivity.this, "تم حفظ الإعدادات وتطبيق الثيم والخط!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        content.addView(save);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
