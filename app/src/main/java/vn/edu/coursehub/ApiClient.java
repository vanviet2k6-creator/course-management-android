package vn.edu.coursehub;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** All HTTP and JSON work runs off the UI thread. Never supplies local fallback records. */
public final class ApiClient {
    private final String baseUrl;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    public interface Task<T> { T run() throws Exception; }
    public interface Callback<T> { void success(T result); void error(String message); }
    public ApiClient(String baseUrl) { this.baseUrl = baseUrl.replaceAll("/+$", ""); }
    public <T> void async(Task<T> task, Callback<T> callback) {
        if (closed) return;
        executor.execute(() -> {
            try { T result = task.run(); main.post(() -> { if (!closed) callback.success(result); }); }
            catch (Exception e) {
                String message = e instanceof java.net.SocketTimeoutException ? "Kết nối quá thời gian. Vui lòng thử lại."
                    : e instanceof java.net.UnknownHostException ? "Không tìm thấy máy chủ. Kiểm tra Internet và Base URL."
                    : e.getMessage() == null ? "Không thể kết nối tới API." : e.getMessage();
                main.post(() -> { if (!closed) callback.error(message); });
            }
        });
    }
    public JSONArray list(String resource) throws Exception { return new JSONArray(request("GET", "/" + resource, null)); }
    public JSONObject detail(String resource, String id) throws Exception {
        return new JSONObject(request("GET", "/" + resource + "/" + Uri.encode(id), null));
    }
    public JSONObject save(String resource, String id, JSONObject body) throws Exception {
        return new JSONObject(request(id == null ? "POST" : "PUT", "/" + resource + (id == null ? "" : "/" + Uri.encode(id)), body));
    }
    public void delete(String resource, String id) throws Exception { request("DELETE", "/" + resource + "/" + Uri.encode(id), null); }
    public JSONObject saveCourse(String id, JSONObject body) throws Exception {
        String categoryId = body.getString("categoryId");
        boolean exists = false;
        for (Models.Category category : Models.categories(list("categories"))) if (category.id.equals(categoryId)) exists = true;
        if (!exists) throw new IOException("Danh mục đã bị xóa hoặc không tồn tại. Hãy tải lại dữ liệu để chọn danh mục khác.");
        return save("courses", id, body);
    }
    public void deleteCategory(String id) throws Exception {
        // MockAPI has no foreign keys: always check the current backend first.
        for (Models.Course course : Models.courses(list("courses"))) {
            if (course.categoryId.equals(id)) throw new IOException("Không thể xóa: danh mục đang có khóa học. Hãy chuyển hoặc xóa các khóa học trước.");
        }
        delete("categories", id);
    }
    private String request(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(20000);
        c.setRequestProperty("Accept", "application/json");
        try {
            if (body != null) {
                c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = c.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            }
            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            String text = "";
            if (stream != null) try (InputStream in = stream) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                text = out.toString(StandardCharsets.UTF_8.name());
            }
            if (code < 200 || code >= 300) {
                if (code == 404) throw new IOException("Không tìm thấy dữ liệu (404). Kiểm tra resource hoặc tải lại danh sách.");
                throw new IOException("API trả lỗi HTTP " + code + ". Vui lòng thử lại.");
            }
            return text;
        } finally { c.disconnect(); }
    }
    public void close() { closed = true; executor.shutdownNow(); }
}
