package edu.cnu.mdi.mapping.loader;

import java.io.IOException;
import java.io.Reader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Package-private Gson helpers shared by {@link GeoJsonCountryLoader} and
 * {@link GeoJsonCityLoader}.
 *
 * <p>Gson's tree model throws on a missing member or a type mismatch, whereas
 * the loaders are deliberately tolerant and skip malformed features. These
 * helpers provide the null-tolerant navigation the loaders need: every method
 * accepts {@code null} and returns {@code null} (or an empty array) rather
 * than throwing when the requested content is absent or of the wrong type.</p>
 */
final class GeoJsonSupport {

    private static final JsonArray EMPTY = new JsonArray();

    private GeoJsonSupport() {}

    /**
     * Parses a complete JSON document and returns its root object.
     *
     * @param reader source of JSON text
     * @return the root object, or {@code null} if the document is empty or its
     *         root is not a JSON object
     * @throws IOException if the text is not well-formed JSON
     */
    static JsonObject parseRoot(Reader reader) throws IOException {
        try {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setStrictness(Strictness.STRICT);
            JsonElement root = JsonParser.parseReader(jsonReader);
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("Unexpected content after the end of the JSON document");
            }
            return root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("Malformed JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the named member of an object.
     *
     * @param node  a JSON element, possibly {@code null}
     * @param field member name
     * @return the member, or {@code null} if {@code node} is not an object or
     *         has no such member
     */
    static JsonElement get(JsonElement node, String field) {
        return (node != null && node.isJsonObject()) ? node.getAsJsonObject().get(field) : null;
    }

    /**
     * Returns the array itself, or an empty array if {@code node} is not a
     * JSON array. Lets callers iterate without a separate type check.
     *
     * @param node a JSON element, possibly {@code null}
     * @return {@code node} as an array, or an empty array
     */
    static JsonArray array(JsonElement node) {
        return (node != null && node.isJsonArray()) ? node.getAsJsonArray() : EMPTY;
    }

    /**
     * Returns the string form of a JSON primitive (string, number or boolean).
     *
     * @param node a JSON element, possibly {@code null}
     * @return the primitive's text, or {@code null} for anything else
     *         (missing, JSON {@code null}, object or array)
     */
    static String text(JsonElement node) {
        return (node != null && node.isJsonPrimitive()) ? node.getAsString() : null;
    }

    /**
     * Tests whether an element is a JSON number (not a numeric string).
     *
     * @param node a JSON element, possibly {@code null}
     * @return {@code true} if {@code node} is a JSON number
     */
    static boolean isNumber(JsonElement node) {
        return node != null && node.isJsonPrimitive() && node.getAsJsonPrimitive().isNumber();
    }
}
