package vn.edu.coursehub;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UiSmokeTest {
    private final Instrumentation inst = InstrumentationRegistry.getInstrumentation();
    private final Context context = inst.getTargetContext();
    private Activity activity;
    private String oldUrl;
    private String base;
    @Before public void setup() {
        oldUrl = context.getSharedPreferences("MainActivity", 0).getString("baseUrl", null);
        base = InstrumentationRegistry.getArguments().getString("baseUrl", BuildConfig.DEFAULT_BASE_URL);
        context.getSharedPreferences("MainActivity", 0).edit().putString("baseUrl", base).commit();
    }
    @After public void cleanup() {
        if (activity != null) inst.runOnMainSync(() -> activity.finish());
        if (oldUrl == null) context.getSharedPreferences("MainActivity", 0).edit().remove("baseUrl").commit();
        else context.getSharedPreferences("MainActivity", 0).edit().putString("baseUrl", oldUrl).commit();
    }
    private void launch() { activity = inst.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    private View find(View root, String label) {
        if (label.equals(root.getContentDescription())) return root;
        if (root instanceof TextView && label.contentEquals(((TextView) root).getText())) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i=0; i<group.getChildCount(); i++) { View result = find(group.getChildAt(i), label); if (result != null) return result; }
        }
        return null;
    }
    private View get(String label) {
        AtomicReference<View> result = new AtomicReference<>();
        inst.runOnMainSync(() -> result.set(find(activity.getWindow().getDecorView(), label))); return result.get();
    }
    private View description(View root, String label) {
        if (label.equals(root.getContentDescription())) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup)root).getChildCount(); i++) {
            View result = description(((ViewGroup)root).getChildAt(i), label); if (result != null) return result;
        }
        return null;
    }
    private View getDescription(String label) {
        AtomicReference<View> result = new AtomicReference<>();
        inst.runOnMainSync(() -> result.set(description(activity.getWindow().getDecorView(), label))); return result.get();
    }
    private void waitFor(String label) {
        long deadline = SystemClock.uptimeMillis() + 35000;
        while (SystemClock.uptimeMillis() < deadline) { if (get(label) != null) return; SystemClock.sleep(100); }
        fail("UI did not show: " + label);
    }
    private void click(String label) {
        waitFor(label); View view = get(label);
        inst.runOnMainSync(() -> {
            View target = view;
            while (!target.isClickable() && target.getParent() instanceof View) target = (View) target.getParent();
            assertTrue("Clickable view for " + label, target.performClick());
        }); inst.waitForIdleSync();
    }
    private void screenshot(String name) throws Exception {
        // Give remote cover images a brief chance to render. Screenshot is test evidence only.
        SystemClock.sleep(1000);
        Bitmap bitmap = inst.getUiAutomation().takeScreenshot();
        File directory = new File(context.getExternalFilesDir(null), "screenshots"); directory.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(directory, name + ".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        bitmap.recycle();
    }
    @Test public void listSearchFilterDetailAndFormValidation() throws Exception {
        launch(); waitFor("Khóa học (10)"); screenshot("01-courses");
        EditText search = (EditText) get("Tìm kiếm khóa học");
        inst.runOnMainSync(() -> search.setText("android")); waitFor("Khóa học (2)");
        assertNotNull(get("Lập trình Android cơ bản")); assertNull(get("Thiết kế Web với HTML và CSS"));
        inst.runOnMainSync(() -> search.setText(""));
        Spinner filter = (Spinner) get("Lọc theo danh mục");
        inst.runOnMainSync(() -> filter.setSelection(2)); waitFor("Khóa học (3)");
        assertNotNull(get("Thiết kế Web với HTML và CSS")); assertNull(get("Lập trình Android cơ bản"));
        inst.runOnMainSync(() -> filter.setSelection(0)); waitFor("Khóa học (10)");
        click("Lập trình Android cơ bản"); waitFor("Chỉnh sửa khóa học"); screenshot("02-detail");
        click("Chỉnh sửa khóa học"); waitFor("Tên khóa học *"); screenshot("03-editor");
        EditText title = (EditText) getDescription("Tên khóa học *");
        inst.runOnMainSync(() -> title.setText("")); click("Lưu thay đổi");
        AtomicReference<CharSequence> error = new AtomicReference<>(); inst.runOnMainSync(() -> error.set(title.getError()));
        assertNotNull("Empty title must show validation error", error.get());
    }
    @Test public void categoriesScreenUsesApiNames() throws Exception {
        launch(); waitFor("Khóa học (10)"); click("Danh mục"); waitFor("Tổ chức kiến thức");
        assertNotNull(get("Lập trình Mobile")); assertNotNull(get("Lập trình Web")); assertNotNull(get("Trí tuệ nhân tạo")); screenshot("04-categories");
    }
    @Test public void directTrailerActuallyPlaysInApp() throws Exception {
        Intent intent = new Intent(context, TrailerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("url", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4").putExtra("title", "Trailer mẫu");
        activity = inst.startActivitySync(intent);
        // Allow a cold remote MP4 connection on a software-rendered emulator.
        // The assertion still requires actual playback past 3.5 seconds.
        long deadline = SystemClock.uptimeMillis() + 90000; boolean playing = false;
        while (SystemClock.uptimeMillis() < deadline) {
            AtomicReference<Boolean> result = new AtomicReference<>(false);
            inst.runOnMainSync(() -> result.set(isPlaying(activity.getWindow().getDecorView())));
            if (result.get()) { playing = true; break; } SystemClock.sleep(200);
        }
        assertTrue("Remote MP4 must actually start playing inside VideoView", playing); screenshot("05-trailer");
    }
    private boolean isPlaying(View v) {
        if (v instanceof VideoView) return ((VideoView)v).isPlaying() && ((VideoView)v).getCurrentPosition() >= 3500;
        if (v instanceof ViewGroup) for (int i=0; i<((ViewGroup)v).getChildCount(); i++) if (isPlaying(((ViewGroup)v).getChildAt(i))) return true;
        return false;
    }
}
