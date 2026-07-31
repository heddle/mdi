package edu.cnu.mdi.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class JsonPackageTest {

    @Test
    void nodeDataEnforcesItsDocumentedInvariants() {
        assertThrows(NullPointerException.class,
                () -> new JsonNodeData(null, "", JsonNodeData.Kind.ROOT));
        assertThrows(NullPointerException.class,
                () -> new JsonNodeData("key", null, JsonNodeData.Kind.STRING));
        assertThrows(NullPointerException.class,
                () -> new JsonNodeData("key", "value", null));
    }

    @Test
    void rawSearchIsLocaleIndependent() throws Exception {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            AtomicInteger total = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> {
                JsonRawPane pane = new JsonRawPane();
                pane.showJson("{\"TITLE\": \"VALUE\"}");
                pane.highlight("title", (current, count) -> total.set(count));
            });
            assertEquals(1, total.get());
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void treeAcceptsAndFiltersNestedJson() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JsonTreePane pane = new JsonTreePane();
            pane.setRoot(JsonParser.parseString("{\"a\":[1,{\"b\":true}]}"), "test.json");
            pane.search("B");
            pane.search("");
            assertThrows(NullPointerException.class, () -> pane.setRoot(null, "test.json"));
        });
    }
}
