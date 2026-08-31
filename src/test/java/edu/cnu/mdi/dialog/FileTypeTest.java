package edu.cnu.mdi.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FileTypeTest {

    @Test
    void normalizesExtensionsAndAppendsPreferredExtension() {
        FileType type = FileType.of("JSON data", ".JSON", "json", "jsn");
        assertEquals(java.util.List.of("json", "jsn"), type.extensions());
        Path accepted = Path.of("result.JSN");
        assertSame(accepted, type.ensureExtension(accepted));
        assertEquals(Path.of("result.json"), type.ensureExtension(Path.of("result")));
    }

    @Test
    void rejectsEmptyDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> FileType.of("blank"));
        assertThrows(IllegalArgumentException.class, () -> FileType.of(" ", "txt"));
        assertThrows(IllegalArgumentException.class, () -> FileType.of("Text", ""));
    }
}
