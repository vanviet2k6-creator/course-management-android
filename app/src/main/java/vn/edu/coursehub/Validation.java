package vn.edu.coursehub;

import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Pure Java rules, shared by forms and unit tests. */
public final class Validation {
    private Validation() {}
    public static boolean httpsUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                && uri.getUserInfo() == null && uri.getHost().contains(".");
        } catch (Exception e) { return false; }
    }
    public static boolean baseUrl(String value) {
        if (!httpsUrl(value)) return false;
        URI uri = URI.create(value.trim());
        return uri.getQuery() == null && uri.getFragment() == null
            && !value.contains(":endpoint")
            && uri.getHost().toLowerCase(Locale.ROOT).endsWith(".mockapi.io");
    }
    public static boolean nonNegative(String value) {
        try { double n = Double.parseDouble(value.trim()); return Double.isFinite(n) && n >= 0; }
        catch (Exception e) { return false; }
    }
    public static boolean positive(String value) {
        return nonNegative(value) && Double.parseDouble(value.trim()) > 0;
    }
    public static boolean date(String value) {
        try { return LocalDate.parse(value).toString().equals(value); }
        catch (DateTimeParseException e) { return false; }
    }
    public static String searchKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
