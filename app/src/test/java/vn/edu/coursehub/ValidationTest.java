package vn.edu.coursehub;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationTest {
    @Test public void invalidFeesAreRejected() {
        assertTrue(Validation.nonNegative("0")); assertTrue(Validation.nonNegative("500000.5"));
        assertFalse(Validation.nonNegative("-1")); assertFalse(Validation.nonNegative("abc"));
        assertFalse(Validation.nonNegative("NaN")); assertFalse(Validation.nonNegative("Infinity"));
        assertFalse(Validation.positive("0")); assertTrue(Validation.positive("0.5"));
    }
    @Test public void onlyRealCalendarDatesAreAllowed() {
        assertTrue(Validation.date("2024-02-29")); assertFalse(Validation.date("2026-02-29"));
        assertFalse(Validation.date("2026-13-01")); assertFalse(Validation.date("20/10/2026"));
    }
    @Test public void searchHandlesVietnameseAccentsAndCase() {
        assertEquals("lap trinh android", Validation.searchKey("  Lập trình Android  "));
        assertEquals("do an", Validation.searchKey("Đồ án"));
    }
    @Test public void baseUrlMustBelongToMockApi() {
        assertTrue(Validation.baseUrl("https://abc.mockapi.io/api/v1"));
        assertFalse(Validation.baseUrl("https://mockapi.io.evil.test/api"));
        assertFalse(Validation.baseUrl("http://abc.mockapi.io"));
        assertFalse(Validation.baseUrl("https://abc.mockapi.io?foo=bar"));
        assertFalse(Validation.httpsUrl("https://name:password@abc.mockapi.io"));
        assertFalse(Validation.httpsUrl("javascript:alert(1)"));
    }
}
