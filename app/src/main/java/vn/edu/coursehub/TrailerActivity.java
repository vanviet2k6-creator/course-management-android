package vn.edu.coursehub;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

/** Plays direct HTTPS video in the app with seek, pause, buffering and error states. */
public class TrailerActivity extends Activity {
    private VideoView video;
    private TextView status;
    private ProgressBar progress;
    private int position;
    private boolean resume = true;
    private String url;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (state != null) { position = state.getInt("position"); resume = state.getBoolean("playing", true); }
        url = getIntent().getStringExtra("url");
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(15, 32, 43));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = v.getWindowInsetsController();
                if (controller != null) controller.setSystemBarsAppearance(0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()); v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        Button back = new Button(this); back.setText("‹  Quay lại chi tiết"); back.setAllCaps(false); back.setOnClickListener(v -> finish()); root.addView(back);
        TextView title = new TextView(this); title.setText(getIntent().getStringExtra("title")); title.setTextColor(Color.WHITE); title.setTextSize(22); title.setPadding(24, 24, 24, 24); root.addView(title);
        FrameLayout player = new FrameLayout(this); video = new VideoView(this);
        FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER); player.addView(video, vp);
        progress = new ProgressBar(this); player.addView(progress, new FrameLayout.LayoutParams(90, 90, Gravity.CENTER));
        root.addView(player, new LinearLayout.LayoutParams(-1, 0, 1));
        status = new TextView(this); status.setTextColor(Color.WHITE); status.setPadding(24, 24, 24, 24); root.addView(status);
        Button retry = new Button(this); retry.setText("Phát lại / Thử lại"); retry.setAllCaps(false); retry.setOnClickListener(v -> { position = 0; resume = true; prepare(); }); root.addView(retry);
        setContentView(root); root.requestApplyInsets();
        MediaController controls = new MediaController(this); controls.setAnchorView(video); video.setMediaController(controls);
        video.setOnPreparedListener(mp -> { progress.setVisibility(View.GONE); status.setText("Trailer · Chạm video để hiện thanh điều khiển"); if (position > 0) video.seekTo(position); if (resume) video.start(); });
        video.setOnInfoListener((mp, what, extra) -> { if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START) progress.setVisibility(View.VISIBLE); if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END) progress.setVisibility(View.GONE); return false; });
        video.setOnErrorListener((mp, what, extra) -> { progress.setVisibility(View.GONE); status.setText("Không phát được trailer. Kiểm tra Internet hoặc URL video MP4 trực tiếp trong form sửa khóa học."); return true; });
        video.setOnCompletionListener(mp -> { position = 0; resume = false; status.setText("Đã phát xong trailer."); }); prepare();
    }
    private void prepare() {
        if (url == null || !Validation.httpsUrl(url)) { progress.setVisibility(View.GONE); status.setText("URL trailer chưa hợp lệ. Hãy nhập URL HTTPS dẫn trực tiếp tới video."); return; }
        progress.setVisibility(View.VISIBLE); status.setText("Đang tải trailer…"); video.setVideoURI(Uri.parse(url));
    }
    @Override protected void onPause() { position = video.getCurrentPosition(); resume = video.isPlaying(); video.pause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (position > 0) video.seekTo(position); if (resume && !video.isPlaying()) video.start(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putInt("position", video.getCurrentPosition()); out.putBoolean("playing", video.isPlaying()); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { video.stopPlayback(); super.onDestroy(); }
}
