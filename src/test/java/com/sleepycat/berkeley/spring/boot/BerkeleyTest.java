package com.sleepycat.berkeley.spring.boot;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BerkeleyTest {
    @TempDir Path home;
    private Environment environment;
    private Database database;
    private BerkeleyTemplate template;

    @BeforeEach
    void openDatabase() {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setConfigParam("je.freeDisk", "0");
        envConfig.setTransactional(true);
        environment = new Environment(home.toFile(), envConfig);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);
        database = environment.openDatabase(null, "items", dbConfig);
        template = new BerkeleyTemplate();
        template.setDatabase(database);
        ReflectionTestUtils.setField(template, "myDbEnvironment", environment);
    }

    @AfterEach
    void closeDatabase() {
        if (database != null) database.close();
        if (environment != null) environment.close();
    }

    @Test
    void writesReadsListsAndDeletesTransactionalRecords() throws Exception {
        assertSame(database, template.getDatabase());
        template.writeToDatabase(" key ", " first ", false);
        template.writeToDatabase("key", "ignored", false);
        assertEquals("first", template.readFromDatabase("key"));
        template.writeToDatabase("key", "second", true);
        assertEquals("second", template.readFromDatabase("key"));
        template.writeToDatabase("other", "value", false);
        assertEquals(Arrays.asList("key", "other"), template.getAllFromDatabase());
        template.deleteFromDatabase("other");
        assertEquals("", template.readFromDatabase("other"));
    }

    @Test
    void storesPrimitiveValues() {
        template.writePrimitiveDatabase("primitive", "stored");
        template.readPrimitiveDatabase("primitive");
        assertNotEquals("", template.readFromDatabase("primitive"));
    }


    @Test
    void clearsAllRecordsWithoutReplacingSharedHandle() {
        template.writeToDatabase("keep", "before", true);
        template.writeToDatabase("second", "before", true);
        template.otherMethod();
        assertSame(database, template.getDatabase());
        assertEquals("", template.readFromDatabase("keep"));
        assertEquals("", template.readFromDatabase("second"));
        template.writeToDatabase("after", "value", true);
        assertEquals("value", template.readFromDatabase("after"));
    }
    @Test
    void failedClearLeavesTheSharedHandleUntouched() {
        Database failing = mock(Database.class);
        when(failing.getDatabaseName()).thenReturn("items");
        when(failing.getEnvironment()).thenReturn(environment);
        when(failing.openCursor(any(com.sleepycat.je.Transaction.class), isNull())).thenThrow(new IllegalStateException("cursor unavailable"));
        template.setDatabase(failing);
        assertThrows(IllegalStateException.class, () -> template.otherMethod());
        assertSame(failing, template.getDatabase());
        template.setDatabase(database);
        template.writeToDatabase("after-failure", "valid", true);
        assertEquals("valid", template.readFromDatabase("after-failure"));
    }
}
