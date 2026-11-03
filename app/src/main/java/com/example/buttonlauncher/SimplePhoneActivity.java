package com.example.buttonlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SimplePhoneActivity extends Activity {

    public static class CallItem {
        String name;
        String number;
        int type;
        long date;
        int duration;
    }

    private LinearLayout listContainer;
    private EditText searchInput;
    private EditText dialInput;
    private int activeTab = 0; // 0: Contacts, 1: Call Log, 2: Keypad
    private int callLogFilter = 0; // 0: All, 1: Missed, 2: Outgoing, 3: Incoming
    private final List<CallItem> allCalls = new ArrayList<CallItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        showContacts();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int pad = dp(16);
        root.setPadding(pad, dp(24), pad, pad);

        // Header
        TextView header = new TextView(this);
        header.setText("PHONE & CALL LOG");
        header.setTextSize(18);
        header.setTextColor(Color.WHITE);
        header.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        root.addView(header);

        // Tabs
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(10), 0, dp(10));

        Button btnContacts = tabButton("الأسماء");
        btnContacts.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showContacts(); }
        });

        Button btnLogs = tabButton("سجل المكالمات");
        btnLogs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showCallLogs(); }
        });

        Button btnKeypad = tabButton("لوحة الاتصال");
        btnKeypad.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showKeypad(); }
        });

        tabs.addView(btnContacts);
        tabs.addView(btnLogs);
        tabs.addView(btnKeypad);
        root.addView(tabs);

        // Search Input for quick filtering
        searchInput = new EditText(this);
        searchInput.setHint("بحث في الأسماء أو سجل المكالمات...");
        searchInput.setHintTextColor(Color.GRAY);
        searchInput.setTextColor(Color.WHITE);
        searchInput.setBackgroundColor(0xFF1E1E1E);
        searchInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        searchInput.setTextSize(13);
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (activeTab == 0) filterContacts(s.toString());
                else if (activeTab == 1) filterCallLogs(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });
        root.addView(searchInput);

        // Content Area
        ScrollView scroll = new ScrollView(this);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);

        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Back button
        Button backBtn = new Button(this);
        backBtn.setText("< الرجوع للشاشة الرئيسية");
        backBtn.setBackgroundColor(0xFF222222);
        backBtn.setTextColor(Color.LTGRAY);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        root.addView(backBtn);

        setContentView(root);
    }

    private Button tabButton(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(0xFF1E1E1E);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(p);
        return b;
    }

    private void showContacts() {
        activeTab = 0;
        searchInput.setVisibility(View.VISIBLE);
        searchInput.setText("");
        filterContacts("");
    }

    private void filterContacts(String query) {
        listContainer.removeAllViews();
        try {
            String selection = null;
            String[] args = null;
            if (query != null && query.trim().length() > 0) {
                selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
                args = new String[]{"%" + query + "%"};
            }

            Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    selection, args, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    final String name = cursor.getString(0);
                    final String number = cursor.getString(1);

                    LinearLayout item = new LinearLayout(this);
                    item.setOrientation(LinearLayout.VERTICAL);
                    item.setPadding(dp(8), dp(10), dp(8), dp(10));
                    item.setBackgroundColor(0xFF111111);

                    TextView nameView = new TextView(this);
                    nameView.setText("👤 " + name);
                    nameView.setTextColor(Color.WHITE);
                    nameView.setTextSize(15);
                    nameView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

                    TextView numView = new TextView(this);
                    numView.setText("   " + number);
                    numView.setTextColor(0xFF4ADE80);
                    numView.setTextSize(13);

                    item.addView(nameView);
                    item.addView(numView);

                    item.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            dial(number);
                        }
                    });

                    listContainer.addView(item);
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            TextView err = new TextView(this);
            err.setText("يرجى منح إذن الأسماء READ_CONTACTS");
            err.setTextColor(Color.RED);
            listContainer.addView(err);
        }
    }

    private void showCallLogs() {
        activeTab = 1;
        searchInput.setVisibility(View.VISIBLE);
        searchInput.setText("");
        loadAllCalls();
        renderCalls(allCalls);
    }

    private void loadAllCalls() {
        allCalls.clear();
        try {
            Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{
                            CallLog.Calls.CACHED_NAME,
                            CallLog.Calls.NUMBER,
                            CallLog.Calls.TYPE,
                            CallLog.Calls.DATE,
                            CallLog.Calls.DURATION
                    },
                    null, null, CallLog.Calls.DATE + " DESC"
            );

            if (cursor != null) {
                int count = 0;
                while (cursor.moveToNext() && count < 60) {
                    count++;
                    CallItem item = new CallItem();
                    item.name = cursor.getString(0);
                    item.number = cursor.getString(1);
                    item.type = cursor.getInt(2);
                    item.date = cursor.getLong(3);
                    item.duration = cursor.getInt(4);
                    allCalls.add(item);
                }
                cursor.close();
            }
        } catch (SecurityException ignored) {}
    }

    private void filterCallLogs(String query) {
        if (query == null || query.trim().length() == 0) {
            renderCalls(allCalls);
            return;
        }
        List<CallItem> filtered = new ArrayList<CallItem>();
        String q = query.toLowerCase();
        for (CallItem c : allCalls) {
            if ((c.name != null && c.name.toLowerCase().contains(q)) || (c.number != null && c.number.contains(q))) {
                filtered.add(c);
            }
        }
        renderCalls(filtered);
    }

    private void renderCalls(List<CallItem> calls) {
        listContainer.removeAllViews();
        if (calls.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد مكالمات مسجلة تطابق البحث.");
            empty.setTextColor(Color.GRAY);
            empty.setPadding(dp(10), dp(20), dp(10), dp(10));
            listContainer.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        for (final CallItem item : calls) {
            final String displayName = (item.name != null && item.name.length() > 0) ? item.name : item.number;
            String typeStr = "📥 واردة";
            int color = Color.WHITE;
            if (item.type == CallLog.Calls.OUTGOING_TYPE) {
                typeStr = "📤 صادرة";
                color = 0xFF4ADE80;
            } else if (item.type == CallLog.Calls.MISSED_TYPE) {
                typeStr = "❌ فائتة";
                color = 0xFFF87171;
            }

            final String dateFormatted = sdf.format(new Date(item.date));
            final String durationFormatted = (item.duration > 0) ? (item.duration / 60) + " د " + (item.duration % 60) + " ث" : "لم يتم الرد";
            final String finalType = typeStr;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(8), dp(10), dp(8), dp(10));
            row.setBackgroundColor(0xFF111111);

            TextView t1 = new TextView(this);
            t1.setText(typeStr + " : " + displayName);
            t1.setTextColor(color);
            t1.setTextSize(14);
            t1.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

            TextView t2 = new TextView(this);
            t2.setText("   " + dateFormatted + " | المدة: " + durationFormatted);
            t2.setTextColor(Color.GRAY);
            t2.setTextSize(11);

            row.addView(t1);
            row.addView(t2);

            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showCallDetailDialog(displayName, item.number, finalType, dateFormatted, durationFormatted);
                }
            });

            View div = new View(this);
            div.setBackgroundColor(0xFF222222);
            listContainer.addView(row);
            listContainer.addView(div, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        }
    }

    private void showCallDetailDialog(final String name, final String number, String status, String date, String duration) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تفاصيل المكالمة");
        builder.setMessage("الاسم: " + name + "\nالرقم: " + number + "\nالحالة: " + status + "\nالوقت والتاريخ: " + date + "\nالمدة: " + duration);
        builder.setPositiveButton("اتصال الآن 📞", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                dial(number);
            }
        });
        builder.setNegativeButton("إغلاق", null);
        builder.show();
    }

    private void showKeypad() {
        activeTab = 2;
        searchInput.setVisibility(View.GONE);
        listContainer.removeAllViews();

        dialInput = new EditText(this);
        dialInput.setTextSize(26);
        dialInput.setTextColor(Color.WHITE);
        dialInput.setBackgroundColor(0xFF1E1E1E);
        dialInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        dialInput.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        listContainer.addView(dialInput);

        Button callBtn = new Button(this);
        callBtn.setText("📞 اتصال (CALL)");
        callBtn.setBackgroundColor(0xFF15803D);
        callBtn.setTextColor(Color.WHITE);
        callBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String num = dialInput.getText().toString().trim();
                if (num.length() > 0) dial(num);
            }
        });
        listContainer.addView(callBtn);
    }

    private void dial(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number)));
        try {
            startActivity(intent);
        } catch (SecurityException e) {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)));
            startActivity(dialIntent);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
