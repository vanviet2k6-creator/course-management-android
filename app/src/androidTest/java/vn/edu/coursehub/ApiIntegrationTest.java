package vn.edu.coursehub;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ApiIntegrationTest {
    @Test public void crudAndForeignKeyRules() throws Exception {
        String base = InstrumentationRegistry.getArguments().getString("baseUrl", BuildConfig.DEFAULT_BASE_URL);
        ApiClient api = new ApiClient(base);
        String categoryId = null, courseId = null;
        String unique = "Kiểm thử " + System.currentTimeMillis();
        try {
            JSONObject cat = api.save("categories", null, new JSONObject().put("name", unique).put("description", "Temporary integration-test record"));
            categoryId = cat.getString("id");
            api.save("categories", categoryId, new JSONObject().put("name", unique + " updated").put("description", "Updated"));
            assertEquals(unique + " updated", api.detail("categories", categoryId).getString("name"));
            JSONObject payload = new JSONObject().put("title", unique).put("description", "Test course")
                .put("categoryId", "missing-category-test").put("instructor", "Test instructor").put("price", 0).put("duration", 1)
                .put("thumbnail", "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=300")
                .put("trailerUrl", "https://media.w3.org/2010/05/sintel/trailer.mp4").put("createdAt", "2026-10-20");
            try { api.saveCourse(null, payload); fail("Missing category must be rejected"); }
            catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("không tồn tại")); }
            payload.put("categoryId", categoryId);
            courseId = api.saveCourse(null, payload).getString("id");
            assertEquals(unique, api.detail("courses", courseId).getString("title"));
            payload.put("title", unique + " edited").put("price", 123456);
            api.saveCourse(courseId, payload);
            JSONObject updated = api.detail("courses", courseId);
            assertEquals(unique + " edited", updated.getString("title")); assertEquals(123456, updated.getDouble("price"), 0.01);
            assertEquals(categoryId, updated.getString("categoryId"));
            try { api.deleteCategory(categoryId); fail("Referenced category must not be deleted"); }
            catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("đang có khóa học")); }
            assertEquals(categoryId, api.detail("categories", categoryId).getString("id"));
            api.delete("courses", courseId); courseId = null;
            api.deleteCategory(categoryId); categoryId = null;
            assertTrue(api.list("categories").length() >= 3); assertTrue(api.list("courses").length() >= 10);
        } finally {
            // Clean up only records created by this test, even if an assertion fails.
            if (courseId != null) api.delete("courses", courseId);
            if (categoryId != null) api.deleteCategory(categoryId);
            api.close();
        }
    }
}
