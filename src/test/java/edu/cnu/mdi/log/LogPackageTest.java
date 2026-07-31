package edu.cnu.mdi.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogPackageTest {

    @TempDir
    Path tempDir;

    @Test
    void duplicateListenerRegistrationStillDispatchesOnce() {
        List<String> messages = new ArrayList<>();
        ILogListener listener = new ILogListener() {
            @Override public void info(String message) { messages.add(message); }
            @Override public void config(String message) { }
            @Override public void warning(String message) { }
            @Override public void error(String message) { }
            @Override public void exception(String message) { }
        };
        Log log = Log.getInstance();
        try {
            log.addLogListener(listener);
            log.addLogListener(listener);
            log.info("once");
            assertEquals(List.of("once"), messages);
        } finally {
            log.removeLogListener(listener);
        }
    }

    @Test
    void fileLoggerWritesAndRotates() throws Exception {
        Path path = tempDir.resolve("application.log");
        try (FileLogger logger = new FileLogger(path, 1)) {
            logger.info("hello");
        }

        assertTrue(Files.exists(path));
        assertTrue(Files.exists(tempDir.resolve("application.log.1")));
        assertTrue(Files.readString(path).contains("hello"));
    }
}
