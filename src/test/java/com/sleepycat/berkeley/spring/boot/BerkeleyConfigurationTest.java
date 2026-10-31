package com.sleepycat.berkeley.spring.boot;

import com.sleepycat.berkeley.spring.boot.hooks.BerkeleyShutdownHook;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BerkeleyConfigurationTest {
    @TempDir Path home;

    @Test
    void configuresEnvironmentAndOpensCatalogAndDatabase() {
        BerkeleyProperties properties = new BerkeleyProperties();
        properties.setAllowCreate(true);
        properties.setTransactional(true);
        properties.setCatalogDatabaseName("catalog");
        properties.setDatabaseName("items");
        BerkeleyAutoConfiguration configuration = new BerkeleyAutoConfiguration();
        ReflectionTestUtils.setField(configuration, "properties", properties);
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        configuration.setResourceLoader(resourceLoader);
        assertSame(resourceLoader, ReflectionTestUtils.getField(configuration, "resourceLoader"));
        EnvironmentConfig settings = configuration.configEnvironment();
        assertTrue(settings.getAllowCreate());
        assertTrue(settings.getTransactional());
        assertFalse(settings.getReadOnly());
        assertNotNull(configuration.repConfigProxy());

        Environment environment = new Environment(home.toFile(), settings);
        Database catalogDatabase = configuration.catalogDatabase(environment);
        StoredClassCatalog catalog = configuration.catalog();
        Database database = configuration.berkeleyDatabase(environment);
        assertEquals("catalog", catalogDatabase.getDatabaseName());
        assertEquals("items", database.getDatabaseName());
        new BerkeleyShutdownHook(database, catalog, environment).run();
    }
}
