package com.sleepycat.berkeley.spring.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CursorTest {
    @TempDir Path home;

    @Test
    void demoRunsAgainstAnIsolatedDatabase() throws Exception {
        Cursor.main(new String[] { home.toString() });
    }

    @Test
    void writesSearchesUpdatesAndDeletesThroughCursor() throws Exception {
        Cursor cursor = new Cursor();
        ReflectionTestUtils.setField(cursor, "envDir", home.toString());
        cursor.configEnvironment();
        cursor.createDatabase();
        try {
            cursor.addDataByCursor("a", "first");
            cursor.addDataByCursor("b", "second");
            cursor.getAllByCursor();
            cursor.searchByCursor("a", "first");
            cursor.searchByCursor("missing", "missing");
            cursor.searchMultipleByCursor("a");
            cursor.changeCursor("a");
            cursor.searchByCursor("a", "www.baike.com");
            cursor.deleteDataByCursor("a");
            assertDoesNotThrow(() -> cursor.searchByCursor("a", "www.baike.com"));
        } finally {
            cursor.closeDatabase();
        }
    }
}
