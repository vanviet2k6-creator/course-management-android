package vn.edu.coursehub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Model objects mirror the two MockAPI resources. */
public final class Models {
    private Models() {}
    public static final class Category {
        public final String id, name, description;
        public Category(JSONObject j) {
            id = j.optString("id"); name = j.optString("name"); description = j.optString("description");
        }
        @Override public String toString() { return name; }
    }
    public static final class Course {
        public final String id, title, description, categoryId, instructor, thumbnail, trailerUrl, createdAt;
        public final double price, duration;
        public Course(JSONObject j) {
            id = j.optString("id"); title = j.optString("title"); description = j.optString("description");
            categoryId = j.optString("categoryId"); instructor = j.optString("instructor");
            thumbnail = j.optString("thumbnail"); trailerUrl = j.optString("trailerUrl");
            createdAt = j.optString("createdAt"); price = j.optDouble("price", 0); duration = j.optDouble("duration", 0);
        }
        public JSONObject toJson() throws JSONException {
            return new JSONObject().put("title", title).put("description", description).put("categoryId", categoryId)
                .put("instructor", instructor).put("price", price).put("duration", duration)
                .put("thumbnail", thumbnail).put("trailerUrl", trailerUrl).put("createdAt", createdAt);
        }
    }
    public static List<Category> categories(JSONArray a) throws JSONException {
        List<Category> list = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) list.add(new Category(a.getJSONObject(i)));
        return list;
    }
    public static List<Course> courses(JSONArray a) throws JSONException {
        List<Course> list = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) list.add(new Course(a.getJSONObject(i)));
        return list;
    }
}
