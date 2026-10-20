package vn.edu.coursehub;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.bumptech.glide.Glide;
import org.json.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import vn.edu.coursehub.Models.Category;
import vn.edu.coursehub.Models.Course;

/** Four core screens plus API settings. REST operations are delegated to ApiClient. */
public class MainActivity extends Activity {
    private static final int INK = Color.rgb(18, 44, 60), GREEN = Color.rgb(8, 127, 109),
        MUTED = Color.rgb(100, 116, 131), BG = Color.rgb(245, 247, 250), WHITE = Color.WHITE;
    private LinearLayout root, content, bottom, listContainer;
    private TextView resultLabel;
    private ApiClient api;
    private String baseUrl, screen = "courses", detailId = "", filterId = "", query = "";
    private List<Category> categories = new ArrayList<>();
    private List<Course> courses = new ArrayList<>();
    private int generation;
    private boolean mutating, dirty;
    private Course editing;
    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private Spinner categorySpinner;
    private JSONObject restoredDraft;
    private ProgressDialog pendingProgress;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        baseUrl = getPreferences(MODE_PRIVATE).getString("baseUrl", BuildConfig.DEFAULT_BASE_URL);
        if (state != null) {
            screen = state.getString("screen", "courses"); detailId = state.getString("detailId", "");
            filterId = state.getString("filter", ""); query = state.getString("query", "");
            try { if (state.containsKey("draft")) restoredDraft = new JSONObject(state.getString("draft")); }
            catch (JSONException ignored) { }
        }
        api = new ApiClient(baseUrl);
        if (baseUrl.isEmpty()) showSettings();
        else if (screen.equals("settings")) showSettings();
        else if (screen.equals("detail") && !detailId.isEmpty()) showDetail(detailId);
        else if (screen.equals("editor") && restoredDraft != null) {
            final JSONObject draft = restoredDraft;
            final String editId = detailId;
            begin("editor", "Khóa học", true); loading();
            int token = generation;
            api.async(() -> Models.categories(api.list("categories")), callback(token, loaded -> {
                categories = loaded;
                try { draft.put("id", editId); } catch (JSONException ignored) { }
                showEditor(new Course(draft), editId.isEmpty());
                for (String key : fields.keySet()) if (draft.has(key)) fields.get(key).setText(draft.optString(key));
                dirty = true;
            }));
        } else load(screen.equals("categories") ? "categories" : "courses");
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("screen", screen); out.putString("detailId", detailId);
        out.putString("filter", filterId); out.putString("query", query);
        if (screen.equals("editor")) out.putString("draft", draft().toString());
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() { if (pendingProgress != null) pendingProgress.dismiss(); api.close(); super.onDestroy(); }
    @Override public void onBackPressed() {
        if (mutating) { toast("Đang lưu dữ liệu, vui lòng đợi."); return; }
        if (screen.equals("editor")) {
            if (dirty) new AlertDialog.Builder(this).setTitle("Bỏ thay đổi?")
                .setMessage("Các thông tin chưa lưu sẽ bị bỏ qua.")
                .setNegativeButton("Tiếp tục sửa", null).setPositiveButton("Bỏ thay đổi", (d, w) -> leaveEditor()).show();
            else leaveEditor();
        } else if (!screen.equals("courses")) load("courses");
        else super.onBackPressed();
    }
    private void leaveEditor() {
        hideKeyboard(); restoredDraft = null;
        if (editing != null && !editing.id.isEmpty()) showDetail(editing.id); else load("courses");
    }
    private int dp(float n) { return (int) (getResources().getDisplayMetrics().density * n + .5f); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLineSpacing(dp(3), 1); return v;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void gap(LinearLayout l, int h) { View v = new View(this); l.addView(v, new LinearLayout.LayoutParams(1, dp(h))); }
    private Button button(String label, boolean primary, View.OnClickListener click) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTextColor(primary ? WHITE : GREEN); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(primary ? GREEN : Color.rgb(227, 242, 237), 12));
        b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48)); b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setOnClickListener(click); return b;
    }
    private void fullButton(LinearLayout into, Button b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(50)); p.topMargin = dp(10); into.addView(b, p);
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout c = column(); c.setPadding(dp(16), dp(16), dp(16), dp(16)); c.setBackground(shape(WHITE, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(14); parent.addView(c, p); return c;
    }
    private void begin(String name, String title, boolean back) {
        generation++; screen = name; mutating = false;
        root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets sys = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root); root.requestApplyInsets();
        LinearLayout header = row(); header.setPadding(dp(20), dp(8), dp(20), dp(8));
        if (back) { Button b = button("‹", false, v -> onBackPressed()); b.setContentDescription("Quay lại"); header.addView(b, new LinearLayout.LayoutParams(dp(48), dp(48))); }
        TextView brand = text(title, 20, INK, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1); titleParams.leftMargin = dp(back ? 12 : 0);
        header.addView(brand, titleParams);
        if (!name.equals("settings") && !name.equals("editor")) {
            Button settings = button("API", false, v -> { if (!mutating) showSettings(); });
            settings.setContentDescription("Cài đặt API"); header.addView(settings, new LinearLayout.LayoutParams(dp(60), dp(48)));
        }
        root.addView(header);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = column(); content.setPadding(dp(20), dp(12), dp(20), dp(24)); scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (name.equals("courses") || name.equals("categories") || name.equals("settings")) {
            bottom = row(); bottom.setPadding(dp(12), dp(8), dp(12), dp(8)); bottom.setBackgroundColor(WHITE);
            addNav("Khóa học", "courses"); addNav("Danh mục", "categories"); addNav("Cài đặt", "settings"); root.addView(bottom);
        }
    }
    private void addNav(String label, String target) {
        Button b = button(label, screen.equals(target), v -> {
            if (mutating) return;
            if (target.equals("settings")) showSettings();
            else if (baseUrl.isEmpty()) { toast("Hãy cấu hình Base URL trước."); showSettings(); }
            else load(target);
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1); p.setMargins(dp(3), 0, dp(3), 0); bottom.addView(b, p);
    }
    private void loading() {
        content.removeAllViews(); gap(content, 36); ProgressBar bar = new ProgressBar(this);
        content.addView(bar, new LinearLayout.LayoutParams(-1, dp(44))); gap(content, 16);
        TextView t = text("Đang tải dữ liệu…", 15, MUTED, false); t.setGravity(Gravity.CENTER); content.addView(t);
    }
    private void error(String message, Runnable retry) {
        content.removeAllViews(); LinearLayout c = card(content);
        c.addView(text("Chưa tải được dữ liệu", 22, INK, true)); gap(c, 10); c.addView(text(message, 15, MUTED, false));
        fullButton(c, button("Thử lại", true, v -> retry.run()));
        fullButton(c, button("Kiểm tra cấu hình API", false, v -> showSettings()));
    }
    private interface Done<T> { void accept(T value); }
    private <T> ApiClient.Callback<T> callback(int token, Done<T> done) {
        return new ApiClient.Callback<>() {
            public void success(T value) { if (token == generation && !isFinishing()) done.accept(value); }
            public void error(String message) {
                if (token != generation || isFinishing()) return;
                MainActivity.this.error(message, () -> {
                    if (screen.equals("detail")) showDetail(detailId); else load(screen.equals("categories") ? "categories" : "courses");
                });
            }
        };
    }
    private static class Data {
        final List<Category> categories; final List<Course> courses;
        Data(List<Category> a, List<Course> b) { categories = a; courses = b; }
    }
    private Data fetchData() throws Exception { return new Data(Models.categories(api.list("categories")), Models.courses(api.list("courses"))); }
    private void load(String target) {
        if (baseUrl.isEmpty()) { showSettings(); return; }
        hideKeyboard(); begin(target, "CourseHub", false); loading(); int token = generation;
        api.async(this::fetchData, callback(token, data -> {
            categories = data.categories; courses = data.courses;
            if (target.equals("categories")) renderCategories(); else renderCourses();
        }));
    }
    private String categoryName(String id) {
        for (Category c : categories) if (c.id.equals(id)) return c.name;
        return "Danh mục không còn tồn tại";
    }
    private String money(double amount) { return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " ₫"; }
    private String hours(double amount) { return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " giờ"; }
    private ImageView image(LinearLayout into, String url, int h) {
        ImageView i = new ImageView(this); i.setScaleType(ImageView.ScaleType.CENTER_CROP); i.setBackground(shape(0xffd6ede7, 12)); i.setClipToOutline(true);
        i.setContentDescription("Ảnh bìa khóa học"); into.addView(i, new LinearLayout.LayoutParams(-1, dp(h)));
        Glide.with(this).load(url).placeholder(vn.edu.coursehub.R.drawable.cover_placeholder)
            .error(vn.edu.coursehub.R.drawable.cover_placeholder).into(i); return i;
    }
    private void renderCourses() {
        content.removeAllViews();
        LinearLayout hero = column(); hero.setPadding(dp(16), dp(16), dp(16), dp(16)); hero.setBackground(shape(INK, 20));
        hero.addView(text("Thư viện khóa học", 23, WHITE, true)); gap(hero, 6);
        hero.addView(text(courses.size() + " khóa học  ·  " + categories.size() + " danh mục", 14, 0xffcfdee6, false));
        content.addView(hero); gap(content, 18);
        EditText search = new EditText(this); search.setSingleLine(); search.setTextSize(15); search.setTextColor(INK);
        search.setHint("Tìm theo tên khóa học…"); search.setPadding(dp(16), 0, dp(16), 0); search.setBackground(shape(WHITE, 12));
        search.setText(query); search.setContentDescription("Tìm kiếm khóa học"); content.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));
        gap(content, 12);
        Spinner filters = new Spinner(this); List<String> names = new ArrayList<>(); names.add("Tất cả danh mục");
        int selected = 0;
        for (int n = 0; n < categories.size(); n++) { names.add(categories.get(n).name); if (categories.get(n).id.equals(filterId)) selected = n + 1; }
        if (selected == 0) filterId = "";
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names); filters.setAdapter(a);
        filters.setBackground(shape(WHITE, 10)); filters.setPadding(dp(8), 0, dp(8), 0); filters.setContentDescription("Lọc theo danh mục");
        content.addView(filters, new LinearLayout.LayoutParams(-1, dp(50))); filters.setSelection(selected);
        gap(content, 12); LinearLayout actions = row();
        actions.addView(button("＋ Thêm khóa học", true, v -> {
            if (categories.isEmpty()) { toast("Hãy tạo danh mục trước."); load("categories"); } else showEditor(null, true);
        }), new LinearLayout.LayoutParams(0, dp(48), 1));
        Button refresh = button("Tải lại", false, v -> load("courses"));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(95), dp(48)); rp.leftMargin = dp(10); actions.addView(refresh, rp); content.addView(actions);
        gap(content, 12); resultLabel = text("", 18, INK, true); content.addView(resultLabel); gap(content, 8);
        listContainer = column(); content.addView(listContainer); renderFiltered();
        search.addTextChangedListener(new SimpleWatcher() { public void afterTextChanged(Editable s) { query = s.toString(); renderFiltered(); } });
        filters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int position, long id) { filterId = position == 0 ? "" : categories.get(position - 1).id; renderFiltered(); }
            public void onNothingSelected(AdapterView<?> p) { }
        });
    }
    private void renderFiltered() {
        if (listContainer == null || !screen.equals("courses")) return;
        listContainer.removeAllViews(); int count = 0;
        for (Course c : courses) {
            if (!Validation.searchKey(c.title).contains(Validation.searchKey(query))) continue;
            if (!filterId.isEmpty() && !filterId.equals(c.categoryId)) continue;
            count++; LinearLayout item = card(listContainer); item.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout body = row(); LinearLayout cover = column(); image(cover, c.thumbnail, 112);
            body.addView(cover, new LinearLayout.LayoutParams(dp(96), dp(112)));
            LinearLayout details = column(); LinearLayout.LayoutParams info = new LinearLayout.LayoutParams(0, -2, 1); info.leftMargin = dp(12); body.addView(details, info);
            TextView category = text(categoryName(c.categoryId), 11, GREEN, true); category.setSingleLine(); category.setEllipsize(TextUtils.TruncateAt.END); details.addView(category); gap(details, 3);
            TextView title = text(c.title, 17, INK, true); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END); details.addView(title); gap(details, 3);
            TextView instructor = text(c.instructor, 12, MUTED, false); instructor.setSingleLine(); instructor.setEllipsize(TextUtils.TruncateAt.END); details.addView(instructor); gap(details, 6);
            details.addView(text(money(c.price) + "  ·  " + hours(c.duration), 13, GREEN, true)); item.addView(body);
            item.setContentDescription("Xem chi tiết " + c.title); item.setOnClickListener(v -> showDetail(c.id));
            item.setFocusable(true);
        }
        resultLabel.setText("Khóa học (" + count + ")");
        if (count == 0) {
            LinearLayout empty = card(listContainer); empty.addView(text(courses.isEmpty() ? "Chưa có khóa học" : "Không có kết quả", 21, INK, true));
            gap(empty, 8); empty.addView(text(courses.isEmpty() ? "Thêm khóa học đầu tiên hoặc nạp dữ liệu mẫu trong Cài đặt." : "Thử từ khóa hoặc danh mục khác.", 15, MUTED, false));
        }
    }
    private void showDetail(String id) {
        hideKeyboard(); detailId = id; begin("detail", "Chi tiết khóa học", true); loading(); int token = generation;
        api.async(() -> {
            List<Category> cats = Models.categories(api.list("categories"));
            Course course = new Course(api.detail("courses", id));
            return new Object[] { cats, course };
        }, callback(token, data -> {
            @SuppressWarnings("unchecked") List<Category> cats = (List<Category>) data[0]; categories = cats;
            renderDetail((Course) data[1]);
        }));
    }
    private void renderDetail(Course c) {
        content.removeAllViews(); image(content, c.thumbnail, 220); gap(content, 18);
        content.addView(text(categoryName(c.categoryId).toUpperCase(new Locale("vi")), 12, GREEN, true)); gap(content, 8);
        content.addView(text(c.title, 28, INK, true)); gap(content, 8); content.addView(text("Giảng viên · " + c.instructor, 16, MUTED, false)); gap(content, 20);
        LinearLayout info = card(content); info.addView(text(money(c.price), 28, GREEN, true)); gap(info, 8);
        info.addView(text("Thời lượng: " + hours(c.duration) + "\nNgày tạo: " + c.createdAt + "\nMã khóa học: " + c.id, 15, MUTED, false));
        content.addView(text("Về khóa học", 20, INK, true)); gap(content, 10); content.addView(text(c.description, 16, INK, false)); gap(content, 16);
        fullButton(content, button("▶  Xem trailer", true, v -> {
            Intent intent = new Intent(this, TrailerActivity.class); intent.putExtra("url", c.trailerUrl); intent.putExtra("title", c.title); startActivity(intent);
        }));
        fullButton(content, button("Chỉnh sửa khóa học", false, v -> showEditor(c, false)));
        Button delete = button("Xóa khóa học", false, v -> new AlertDialog.Builder(this).setTitle("Xóa khóa học?")
            .setMessage("Xóa “" + c.title + "” khỏi MockAPI? Thao tác này không thể hoàn tác.")
            .setNegativeButton("Giữ lại", null).setPositiveButton("Xóa", (d, w) -> deleteCourse(c)).show());
        delete.setTextColor(0xffb73d47); fullButton(content, delete);
    }
    private void deleteCourse(Course c) {
        mutate(() -> { api.delete("courses", c.id); return true; }, () -> { toast("Đã xóa khóa học."); load("courses"); });
    }
    private EditText field(LinearLayout parent, String key, String label, String initial, int inputType, boolean multiline) {
        gap(parent, 14); parent.addView(text(label, 14, INK, true)); gap(parent, 6);
        EditText e = new EditText(this); e.setTextSize(16); e.setTextColor(INK); e.setPadding(dp(14), dp(10), dp(14), dp(10));
        e.setBackground(shape(WHITE, 10)); e.setInputType(inputType); e.setSingleLine(!multiline); if (multiline) { e.setMinLines(3); e.setGravity(Gravity.TOP); }
        e.setText(initial); e.setContentDescription(label); parent.addView(e, new LinearLayout.LayoutParams(-1, multiline ? dp(104) : dp(52)));
        fields.put(key, e); e.addTextChangedListener(new SimpleWatcher() { public void afterTextChanged(Editable value) { dirty = true; } }); return e;
    }
    private void showEditor(Course course, boolean isNew) {
        editing = isNew ? null : course; detailId = isNew ? "" : course.id; fields.clear(); dirty = false;
        begin("editor", isNew ? "Thêm khóa học" : "Sửa khóa học", true);
        if (categories.isEmpty()) { error("Chưa có danh mục. Hãy thêm danh mục trước khi tạo khóa học.", () -> load("categories")); return; }
        content.addView(text(isNew ? "Tạo cơ hội học tập mới" : "Cập nhật thông tin", 25, INK, true)); gap(content, 5);
        content.addView(text("Các trường có dấu * là bắt buộc.", 14, MUTED, false));
        int normal = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        field(content, "title", "Tên khóa học *", course == null ? "" : course.title, normal, false);
        field(content, "description", "Mô tả *", course == null ? "" : course.description, normal | InputType.TYPE_TEXT_FLAG_MULTI_LINE, true);
        field(content, "instructor", "Giảng viên *", course == null ? "" : course.instructor, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false);
        gap(content, 14); content.addView(text("Danh mục *", 14, INK, true)); gap(content, 6);
        categorySpinner = new Spinner(this); categorySpinner.setContentDescription("Danh mục khóa học");
        categorySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));
        categorySpinner.setBackground(shape(WHITE, 10)); content.addView(categorySpinner, new LinearLayout.LayoutParams(-1, dp(52)));
        int selected = -1;
        if (course != null) for (int i = 0; i < categories.size(); i++) if (categories.get(i).id.equals(course.categoryId)) selected = i;
        if (course == null) selected = 0;
        if (selected >= 0) categorySpinner.setSelection(selected);
        else { categorySpinner.setSelection(-1); toast("Danh mục cũ không còn tồn tại. Hãy chọn danh mục mới."); }
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { dirty = true; }
            public void onNothingSelected(AdapterView<?> p) { }
        });
        int number = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
        field(content, "price", "Học phí (VNĐ) *", course == null ? "" : plain(course.price), number, false);
        field(content, "duration", "Thời lượng (giờ) *", course == null ? "" : plain(course.duration), number, false);
        EditText thumbnail = field(content, "thumbnail", "URL ảnh bìa (HTTPS) *", course == null ? "" : course.thumbnail, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, false);
        gap(content, 10); ImageView preview = image(content, course == null ? "" : course.thumbnail, 170); preview.setContentDescription("Xem trước ảnh bìa");
        TextView previewNote = text("Nhập URL rồi bấm Xem trước ảnh để kiểm tra.", 12, MUTED, false); content.addView(previewNote);
        fullButton(content, button("Xem trước ảnh", false, v -> {
            if (!Validation.httpsUrl(value("thumbnail"))) { thumbnail.setError("Nhập URL HTTPS hợp lệ."); return; }
            Glide.with(this).load(value("thumbnail")).placeholder(vn.edu.coursehub.R.drawable.cover_placeholder)
                .error(vn.edu.coursehub.R.drawable.cover_placeholder).listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object m, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> t, boolean first) {
                        previewNote.setText("Không tải được ảnh. Kiểm tra URL hoặc kết nối mạng."); return false;
                    }
                    public boolean onResourceReady(android.graphics.drawable.Drawable d, Object m, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> t, com.bumptech.glide.load.DataSource source, boolean first) {
                        previewNote.setText("Ảnh đã tải thành công."); return false;
                    }
                }).into(preview);
        }));
        field(content, "trailerUrl", "URL trailer MP4 (HTTPS) *", course == null ? "" : course.trailerUrl, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, false);
        content.addView(text("Dùng liên kết video trực tiếp; liên kết trang YouTube không phải URL MP4.", 12, MUTED, false));
        field(content, "createdAt", "Ngày tạo (yyyy-MM-dd) *", course == null ? LocalDate.now().toString() : course.createdAt, InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE, false);
        fullButton(content, button(isNew ? "Lưu khóa học" : "Lưu thay đổi", true, v -> saveCourse()));
        fullButton(content, button("Hủy", false, v -> onBackPressed())); dirty = false;
    }
    private String plain(double n) { return java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString(); }
    private String value(String name) { return fields.get(name).getText().toString().trim(); }
    private JSONObject draft() {
        JSONObject data = new JSONObject();
        try {
            for (Map.Entry<String, EditText> e : fields.entrySet()) data.put(e.getKey(), e.getValue().getText().toString());
            Category c = categorySpinner == null ? null : (Category) categorySpinner.getSelectedItem();
            data.put("categoryId", c == null ? "" : c.id);
        } catch (JSONException ignored) { }
        return data;
    }
    private boolean fail(String field, String message) { EditText e = fields.get(field); e.setError(message); e.requestFocus(); return false; }
    private void saveCourse() {
        if (mutating) return;
        for (String name : fields.keySet()) if (value(name).isEmpty()) { fail(name, "Vui lòng nhập trường bắt buộc."); return; }
        if (!Validation.nonNegative(value("price"))) { fail("price", "Học phí phải là số hữu hạn và không âm."); return; }
        if (!Validation.positive(value("duration"))) { fail("duration", "Thời lượng phải lớn hơn 0."); return; }
        if (!Validation.httpsUrl(value("thumbnail"))) { fail("thumbnail", "URL ảnh phải là liên kết HTTPS hợp lệ."); return; }
        if (!Validation.httpsUrl(value("trailerUrl"))) { fail("trailerUrl", "URL trailer phải là liên kết HTTPS hợp lệ."); return; }
        if (!Validation.date(value("createdAt"))) { fail("createdAt", "Ngày không hợp lệ. Ví dụ: 2026-10-20."); return; }
        Category category = (Category) categorySpinner.getSelectedItem();
        if (category == null) { toast("Vui lòng chọn danh mục."); return; }
        JSONObject body = draft();
        try { body.put("price", Double.parseDouble(value("price"))); body.put("duration", Double.parseDouble(value("duration"))); }
        catch (JSONException e) { toast("Dữ liệu chưa hợp lệ."); return; }
        String id = editing == null ? null : editing.id;
        mutate(() -> api.saveCourse(id, body), () -> { dirty = false; hideKeyboard(); toast("Đã lưu khóa học lên MockAPI."); load("courses"); });
    }
    private <T> void mutate(ApiClient.Task<T> task, Runnable onSuccess) {
        if (mutating) return; mutating = true; int token = generation;
        setEnabledTree(root, false); ProgressDialog progress = ProgressDialog.show(this, "", "Đang đồng bộ với MockAPI…", true, false); pendingProgress = progress;
        api.async(task, new ApiClient.Callback<>() {
            public void success(T result) { if (!isDestroyed()) progress.dismiss(); if (token != generation || isFinishing()) return; mutating = false; setEnabledTree(root, true); onSuccess.run(); }
            public void error(String message) { if (!isDestroyed()) progress.dismiss(); if (token != generation || isFinishing()) return; mutating = false; setEnabledTree(root, true);
                new AlertDialog.Builder(MainActivity.this).setTitle("Chưa hoàn tất thao tác").setMessage(message + "\nNếu kết nối bị ngắt sau khi gửi, hãy tải lại danh sách trước khi thử lại để tránh tạo trùng.")
                    .setPositiveButton("Đã hiểu", null).show(); }
        });
    }
    private void setEnabledTree(View v, boolean enabled) {
        v.setEnabled(enabled); if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) setEnabledTree(((ViewGroup) v).getChildAt(i), enabled);
    }
    private void renderCategories() {
        content.removeAllViews(); content.addView(text("Tổ chức kiến thức", 28, INK, true)); gap(content, 6);
        content.addView(text("Quản lý danh mục và liên kết khóa học.", 15, MUTED, false));
        fullButton(content, button("＋ Thêm danh mục", true, v -> categoryDialog(null)));
        fullButton(content, button("Tải lại danh mục", false, v -> load("categories"))); gap(content, 20);
        if (categories.isEmpty()) { LinearLayout c = card(content); c.addView(text("Chưa có danh mục", 21, INK, true)); gap(c, 8); c.addView(text("Tạo danh mục trước khi thêm khóa học.", 15, MUTED, false)); }
        for (Category category : categories) {
            long count = courses.stream().filter(c -> c.categoryId.equals(category.id)).count();
            LinearLayout c = card(content); c.addView(text(category.name, 21, INK, true)); gap(c, 7); c.addView(text(category.description, 15, MUTED, false));
            gap(c, 10); c.addView(text(count + " khóa học  ·  Mã " + category.id, 13, GREEN, true)); gap(c, 12);
            LinearLayout actions = row();
            actions.addView(button("Sửa", false, v -> categoryDialog(category)), new LinearLayout.LayoutParams(0, dp(48), 1));
            Button del = button("Xóa", false, v -> confirmDeleteCategory(category)); del.setTextColor(0xffb73d47);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1); p.leftMargin = dp(10); actions.addView(del, p); c.addView(actions);
        }
    }
    private void categoryDialog(Category category) {
        LinearLayout form = column(); form.setPadding(dp(22), dp(8), dp(22), 0);
        form.addView(text("Tên danh mục *", 14, INK, true)); EditText name = new EditText(this); name.setSingleLine(); name.setHint("Ví dụ: Lập trình Mobile"); name.setText(category == null ? "" : category.name); form.addView(name);
        gap(form, 8); form.addView(text("Mô tả *", 14, INK, true)); EditText desc = new EditText(this); desc.setMinLines(2); desc.setText(category == null ? "" : category.description); form.addView(desc);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(category == null ? "Thêm danh mục" : "Sửa danh mục")
            .setView(form).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("Nhập tên danh mục."); return; }
            if (desc.getText().toString().trim().isEmpty()) { desc.setError("Nhập mô tả danh mục."); return; }
            try {
                JSONObject body = new JSONObject().put("name", name.getText().toString().trim()).put("description", desc.getText().toString().trim());
                dialog.dismiss(); hideKeyboard();
                mutate(() -> api.save("categories", category == null ? null : category.id, body), () -> { toast("Đã lưu danh mục."); load("categories"); });
            } catch (JSONException e) { toast("Dữ liệu chưa hợp lệ."); }
        })); dialog.show();
    }
    private void confirmDeleteCategory(Category category) {
        new AlertDialog.Builder(this).setTitle("Xóa danh mục?")
            .setMessage("Chỉ có thể xóa “" + category.name + "” khi không còn khóa học thuộc danh mục này.")
            .setNegativeButton("Giữ lại", null).setPositiveButton("Kiểm tra và xóa", (d, w) -> mutate(() -> {
                // Always check current backend records, not the cached count on screen.
                api.deleteCategory(category.id); return true;
            }, () -> { toast("Đã xóa danh mục."); load("categories"); })).show();
    }
    private void showSettings() {
        hideKeyboard(); begin("settings", "Cài đặt API", !baseUrl.isEmpty());
        content.addView(text("Kết nối không gian\nhọc tập của bạn", 28, INK, true)); gap(content, 12);
        content.addView(text("Dữ liệu được đọc và lưu trực tiếp trên MockAPI qua hai resource categories và courses.", 16, MUTED, false)); gap(content, 22);
        content.addView(text("Base URL", 15, INK, true)); gap(content, 8);
        EditText url = new EditText(this); url.setSingleLine(); url.setTextSize(14); url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setHint("https://PROJECT_ID.mockapi.io"); url.setText(baseUrl); url.setBackground(shape(WHITE, 10)); url.setPadding(dp(12), dp(10), dp(12), dp(10));
        content.addView(url, new LinearLayout.LayoutParams(-1, dp(56))); gap(content, 8);
        content.addView(text("Dán phần URL đứng trước /categories hoặc /courses. Có thể bao gồm tiền tố /api/v1 nếu project đã cấu hình.", 13, MUTED, false));
        fullButton(content, button("Lưu và kết nối", true, v -> {
            String input = url.getText().toString().trim().replaceAll("/+$", "");
            if (!Validation.baseUrl(input)) { url.setError("Nhập HTTPS Base URL của mockapi.io, không kèm :endpoint."); return; }
            baseUrl = input; getPreferences(MODE_PRIVATE).edit().putString("baseUrl", input).apply(); api.close(); api = new ApiClient(input);
            categories.clear(); courses.clear(); query = ""; filterId = ""; load("courses");
        }));
        gap(content, 24); LinearLayout seed = card(content); seed.addView(text("Dữ liệu khởi tạo", 20, INK, true)); gap(seed, 8);
        seed.addView(text("Nạp 3 danh mục và 10 khóa học mẫu lên API. Các mục đã có cùng tên sẽ được giữ lại. Đây là thao tác ghi lên backend.", 14, MUTED, false));
        fullButton(seed, button("Nạp dữ liệu mẫu", false, v -> {
            if (baseUrl.isEmpty()) { toast("Hãy lưu Base URL trước."); return; }
            new AlertDialog.Builder(this).setTitle("Nạp dữ liệu mẫu lên MockAPI?")
                .setMessage("Thêm dữ liệu mẫu vào project đang kết nối, không xóa dữ liệu hiện có.")
                .setNegativeButton("Hủy", null).setPositiveButton("Nạp dữ liệu", (d, w) -> seedData()).show();
        }));
        LinearLayout about = card(content); about.addView(text("CourseHub · 1.0", 17, INK, true)); gap(about, 8);
        about.addView(text("Java · REST API · Glide · VideoView\nBài tập Mobile App — Quản lý khóa học", 13, MUTED, false));
    }
    private void seedData() {
        mutate(() -> {
            String raw;
            try (java.io.InputStream in = getAssets().open("sample-data.json")) { raw = readText(in); }
            JSONObject sample = new JSONObject(raw); Map<String, String> mapping = new HashMap<>();
            List<Category> freshCats = Models.categories(api.list("categories")); JSONArray cs = sample.getJSONArray("categories");
            for (int i = 0; i < cs.length(); i++) {
                JSONObject item = cs.getJSONObject(i); String id = null;
                for (Category c : freshCats) if (c.name.equals(item.getString("name"))) id = c.id;
                if (id == null) id = api.save("categories", null, new JSONObject().put("name", item.getString("name")).put("description", item.getString("description"))).getString("id");
                mapping.put(item.getString("key"), id);
            }
            List<Course> freshCourses = Models.courses(api.list("courses")); JSONArray data = sample.getJSONArray("courses");
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = new JSONObject(data.getJSONObject(i).toString()); boolean found = false;
                for (Course c : freshCourses) if (c.title.equals(item.getString("title"))) found = true;
                if (found) continue;
                item.put("categoryId", mapping.get(item.getString("categoryKey"))); item.remove("categoryKey"); api.save("courses", null, item);
            }
            return true;
        }, () -> { toast("Đã nạp dữ liệu mẫu lên API."); load("courses"); });
    }
    static String readText(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        return out.toString(java.nio.charset.StandardCharsets.UTF_8.name());
    }
    private void hideKeyboard() {
        View view = getCurrentFocus(); if (view != null) ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
    private abstract static class SimpleWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
    }
}
