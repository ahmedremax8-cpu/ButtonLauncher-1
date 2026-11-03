package com.example.buttonlauncher;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Built-in Minimalist Music Player for Button Launcher.
 * Full playback control (Play, Pause, Next, Prev, Seek, Playlist)
 * Reads audio files directly from phone storage via MediaStore.
 */
public class SimpleMusicActivity extends Activity {

    public static class Song {
        long id;
        String title;
        String artist;
        Uri uri;
    }

    private final List<Song> songList = new ArrayList<Song>();
    private int currentSongIndex = 0;
    private MediaPlayer mediaPlayer;
    private TextView trackTitleView, artistView, timeView;
    private Button playPauseBtn;
    private SeekBar seekBar;
    private LinearLayout playlistContainer;
    private final Handler handler = new Handler();

    private final Runnable updateProgress = new Runnable() {
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int current = mediaPlayer.getCurrentPosition();
                int total = mediaPlayer.getDuration();
                seekBar.setMax(total);
                seekBar.setProgress(current);
                timeView.setText(formatTime(current) + " / " + formatTime(total));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaPlayer = new MediaPlayer();
        buildUi();
        loadAudioFromDevice();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int pad = dp(16);
        root.setPadding(pad, dp(24), pad, pad);

        // Header
        TextView header = new TextView(this);
        header.setText("MUSIC PLAYER");
        header.setTextSize(18);
        header.setTextColor(Color.WHITE);
        header.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        root.addView(header);

        // Now Playing Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF161616);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(12), 0, dp(12));
        card.setLayoutParams(cardParams);

        trackTitleView = new TextView(this);
        trackTitleView.setText("لا يوجد مقطع قيد التشغيل");
        trackTitleView.setTextSize(16);
        trackTitleView.setTextColor(Color.WHITE);
        trackTitleView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        artistView = new TextView(this);
        artistView.setText("اختر مقطعاً من القائمة أدناه");
        artistView.setTextSize(12);
        artistView.setTextColor(0xFF4ADE80);
        artistView.setTypeface(Typeface.MONOSPACE);

        timeView = new TextView(this);
        timeView.setText("00:00 / 00:00");
        timeView.setTextSize(11);
        timeView.setTextColor(Color.GRAY);
        timeView.setTypeface(Typeface.MONOSPACE);

        seekBar = new SeekBar(this);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Controls (Prev, Play/Pause, Next)
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(0, dp(8), 0, dp(4));

        Button prevBtn = controlBtn("⏮ السابق");
        prevBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playPrevious(); }
        });

        playPauseBtn = controlBtn("▶ تشغيل");
        playPauseBtn.setBackgroundColor(0xFF15803D);
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { togglePlayPause(); }
        });

        Button nextBtn = controlBtn("⏭ التالي");
        nextBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playNext(); }
        });

        controls.addView(prevBtn);
        controls.addView(playPauseBtn);
        controls.addView(nextBtn);

        card.addView(trackTitleView);
        card.addView(artistView);
        card.addView(seekBar);
        card.addView(timeView);
        card.addView(controls);
        root.addView(card);

        // Playlist Header
        TextView plHeader = new TextView(this);
        plHeader.setText("قائمة المقاطع الصوتية في الجهاز:");
        plHeader.setTextSize(12);
        plHeader.setTextColor(Color.LTGRAY);
        root.addView(plHeader);

        // Scrollable Playlist
        ScrollView scroll = new ScrollView(this);
        playlistContainer = new LinearLayout(this);
        playlistContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(playlistContainer);

        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Back Button
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

    private Button controlBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(0xFF262626);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(p);
        return b;
    }

    private void loadAudioFromDevice() {
        playlistContainer.removeAllViews();
        songList.clear();

        try {
            Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST
            };
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            Cursor cursor = getContentResolver().query(collection, projection, selection, null, MediaStore.Audio.Media.TITLE + " ASC");

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String title = cursor.getString(1);
                    String artist = cursor.getString(2);
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    Song s = new Song();
                    s.id = id;
                    s.title = title;
                    s.artist = artist != null ? artist : "فنان غير معروف";
                    s.uri = contentUri;
                    songList.add(s);
                }
                cursor.close();
            }

            renderPlaylist();

        } catch (SecurityException e) {
            TextView err = new TextView(this);
            err.setText("يرجى منح إذن قراءة الملفات الصوتية (READ_MEDIA_AUDIO).");
            err.setTextColor(Color.RED);
            playlistContainer.addView(err);
        }
    }

    private void renderPlaylist() {
        playlistContainer.removeAllViews();
        if (songList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("لم يتم العثور على ملفات صوتية في الهاتف.");
            empty.setTextColor(Color.GRAY);
            playlistContainer.addView(empty);
            return;
        }

        for (int i = 0; i < songList.size(); i++) {
            final int index = i;
            final Song s = songList.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(8), dp(10), dp(8), dp(10));
            row.setBackgroundColor(i == currentSongIndex ? 0xFF2A2A2A : 0xFF111111);

            TextView t = new TextView(this);
            t.setText((i + 1) + ". " + s.title);
            t.setTextColor(Color.WHITE);
            t.setTextSize(14);
            t.setTypeface(Typeface.MONOSPACE);

            TextView a = new TextView(this);
            a.setText("   " + s.artist);
            a.setTextColor(Color.GRAY);
            a.setTextSize(11);

            row.addView(t);
            row.addView(a);

            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    playSong(index);
                }
            });

            playlistContainer.addView(row);
        }
    }

    private void playSong(int index) {
        if (index < 0 || index >= songList.size()) return;
        currentSongIndex = index;
        Song s = songList.get(index);

        trackTitleView.setText(s.title);
        artistView.setText(s.artist);

        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(this, s.uri);
                mediaPlayer.prepare();
                mediaPlayer.start();
                playPauseBtn.setText("⏸ إيقاف");
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        playNext();
                    }
                });
                handler.post(updateProgress);
                renderPlaylist();
            }
        } catch (IOException e) {
            Toast.makeText(this, "تعذر تشغيل الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseBtn.setText("▶ تشغيل");
            handler.removeCallbacks(updateProgress);
        } else {
            if (songList.isEmpty()) return;
            if (mediaPlayer.getCurrentPosition() > 0) {
                mediaPlayer.start();
                playPauseBtn.setText("⏸ إيقاف");
                handler.post(updateProgress);
            } else {
                playSong(currentSongIndex);
            }
        }
    }

    private void playNext() {
        if (songList.isEmpty()) return;
        int next = (currentSongIndex + 1) % songList.size();
        playSong(next);
    }

    private void playPrevious() {
        if (songList.isEmpty()) return;
        int prev = (currentSongIndex - 1 + songList.size()) % songList.size();
        playSong(prev);
    }

    private String formatTime(int ms) {
        int sec = (ms / 1000) % 60;
        int min = (ms / (1000 * 60)) % 60;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateProgress);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
