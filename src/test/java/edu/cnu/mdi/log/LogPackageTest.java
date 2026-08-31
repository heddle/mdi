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
            log.clearHistoryForTesting();
            log.addLogListener(listener);
            log.addLogListener(listener);
            log.info("once");
            assertEquals(List.of("once"), messages);
        } finally {
            log.removeLogListener(listener);
        }
    }

    @Test
    void newListenerReceivesBoundedHistoryInOrderAndAtOriginalLevels() {
        Log log = Log.getInstance();
        log.clearHistoryForTesting();
        for (int index = 0; index <= Log.HISTORY_CAPACITY; index++) {
            log.info("info-" + index);
        }
        log.warning("warning");

        List<String> messages = new ArrayList<>();
        ILogListener listener = new ILogListener() {
            @Override public void info(String message) { messages.add("I:" + message); }
            @Override public void warning(String message) { messages.add("W:" + message); }
        };
        try {
            log.addLogListener(listener);
            assertEquals(Log.HISTORY_CAPACITY, messages.size());
            assertEquals("I:info-2", messages.get(0));
            assertEquals("W:warning", messages.get(messages.size() - 1));
        } finally {
            log.removeLogListener(listener);
            log.clearHistoryForTesting();
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
