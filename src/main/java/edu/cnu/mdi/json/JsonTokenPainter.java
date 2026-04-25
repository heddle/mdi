package edu.cnu.mdi.json;

import javax.swing.text.StyledDocument;

/**
 * Applies syntax-highlight color runs to a {@link StyledDocument} that already
 * contains raw JSON text.
 *
 * <h2>Approach</h2>
 * <p>
 * A single left-to-right scan of the raw JSON string identifies token
 * boundaries and calls {@link StyledDocument#setCharacterAttributes} for each
 * token span. The document text is <em>not</em> modified — only attributes are
 * changed — so this method is safe to call after a bulk
 * {@link StyledDocument#insertString} has already placed the text.
 * </p>
 *
 * <h2>Token categories</h2>
 * <ul>
 *   <li><b>Object keys</b> — any quoted string immediately followed (after
 *       optional whitespace) by a colon. Styled with
 *       {@link JsonRawPane#STYLE_KEY}.</li>
 *   <li><b>String values</b> — quoted strings that are <em>not</em> keys.
 *       Styled with {@link JsonRawPane#STYLE_STRING}.</li>
 *   <li><b>Numbers</b> — JSON number literals (integer, decimal, exponent).
 *       Styled with {@link JsonRawPane#STYLE_NUMBER}.</li>
 *   <li><b>Booleans</b> — the literals {@code true} and {@code false}.
 *       Styled with {@link JsonRawPane#STYLE_BOOLEAN}.</li>
 *   <li><b>Null</b> — the literal {@code null}. Styled with
 *       {@link JsonRawPane#STYLE_NULL}.</li>
 *   <li><b>Structural</b> — {@code { } [ ] , :}. Styled with
 *       {@link JsonRawPane#STYLE_STRUCTURAL}.</li>
 * </ul>
 *
 * <h2>Escape handling</h2>
 * <p>
 * Backslash escapes inside string literals are consumed correctly so that
 * {@code \"} does not terminate the string prematurely.
 * </p>
 *
 * <h2>Limitations</h2>
 * <p>
 * This is a display-only colorizer, not a validating parser. It assumes the
 * input is well-formed JSON (which Gson has already verified before this
 * method is called). Behaviour on malformed input is undefined but will not
 * throw — the worst case is a token receiving the wrong style.
 * </p>
 *
 * <h2>Performance</h2>
 * <p>
 * Each call to {@link StyledDocument#setCharacterAttributes} triggers a
 * document change event. For very large files (tens of thousands of tokens)
 * this can cause a brief repaint delay. The method runs on the EDT (called
 * from {@link JsonRawPane#showJson}), so the UI is unresponsive during
 * painting. For the file sizes typical of splot and map configuration files
 * this is imperceptible; if larger files become a concern, the painting can
 * be moved to a background thread using
 * {@link javax.swing.text.AbstractDocument#setCharacterAttributes} with
 * {@code replace = false} batched via a write lock.
 * </p>
 */
final class JsonTokenPainter {

    /** Static utility class — no instances. */
    private JsonTokenPainter() {}

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    /**
     * Walk {@code json} left-to-right, identify token spans, and apply the
     * corresponding {@link JsonRawPane} style to each span in {@code doc}.
     *
     * <p>The document must already contain {@code json} starting at offset 0.
     * This method only sets attributes; it does not insert or remove
     * characters.</p>
     *
     * @param doc  the styled document to annotate; must not be {@code null}
     * @param json the raw JSON string whose characters populate {@code doc}
     *             starting at offset 0; must not be {@code null}
     */
    static void paint(StyledDocument doc, String json) {
        int i   = 0;
        int len = json.length();

        while (i < len) {
            char c = json.charAt(i);

            // Skip whitespace — leave it with the base STYLE_PLAIN.
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Structural characters.
            if (c == '{' || c == '}' || c == '[' || c == ']'
                    || c == ',' || c == ':') {
                doc.setCharacterAttributes(i, 1, JsonRawPane.STYLE_STRUCTURAL, false);
                i++;
                continue;
            }

            // Quoted string — determine whether it is a key or a value.
            if (c == '"') {
                int end = scanString(json, i);        // index of closing quote + 1
                int afterWhitespace = skipWhitespace(json, end);
                boolean isKey = afterWhitespace < len
                        && json.charAt(afterWhitespace) == ':';
                doc.setCharacterAttributes(i, end - i,
                        isKey ? JsonRawPane.STYLE_KEY : JsonRawPane.STYLE_STRING,
                        false);
                i = end;
                continue;
            }

            // Number literal.
            if (c == '-' || Character.isDigit(c)) {
                int end = scanNumber(json, i);
                doc.setCharacterAttributes(i, end - i, JsonRawPane.STYLE_NUMBER, false);
                i = end;
                continue;
            }

            // Boolean / null literals — match by prefix.
            if (json.startsWith("true",  i)) {
                doc.setCharacterAttributes(i, 4, JsonRawPane.STYLE_BOOLEAN, false);
                i += 4;
                continue;
            }
            if (json.startsWith("false", i)) {
                doc.setCharacterAttributes(i, 5, JsonRawPane.STYLE_BOOLEAN, false);
                i += 5;
                continue;
            }
            if (json.startsWith("null",  i)) {
                doc.setCharacterAttributes(i, 4, JsonRawPane.STYLE_NULL, false);
                i += 4;
                continue;
            }

            // Anything else: advance one character and leave it plain.
            i++;
        }
    }

    // -------------------------------------------------------------------------
    // Private scanner helpers
    // -------------------------------------------------------------------------

    /**
     * Return the index just past the closing {@code "} of the string that
     * starts at {@code start} (which must be the opening {@code "}).
     *
     * <p>Backslash escapes are consumed so that {@code \"} does not
     * prematurely terminate the scan.</p>
     *
     * @param json  the source string
     * @param start index of the opening {@code "}
     * @return index of the first character after the closing {@code "},
     *         or {@code json.length()} if the string is unterminated
     */
    private static int scanString(String json, int start) {
        int i = start + 1;   // skip opening quote
        int len = json.length();
        while (i < len) {
            char c = json.charAt(i);
            if (c == '\\') {
                i += 2;      // skip escape sequence (e.g. \" \\ \n)
            } else if (c == '"') {
                return i + 1; // include closing quote
            } else {
                i++;
            }
        }
        return len; // unterminated — treat whole remainder as the string
    }

    /**
     * Return the index just past the last character of the JSON number that
     * starts at {@code start}.
     *
     * <p>Handles the full JSON number grammar: optional leading minus, integer
     * digits, optional decimal part, optional exponent ({@code e}/{@code E}
     * with optional sign).</p>
     *
     * @param json  the source string
     * @param start index of the first character of the number ({@code -} or a
     *              digit)
     * @return index of the first character after the number
     */
    private static int scanNumber(String json, int start) {
        int i   = start;
        int len = json.length();

        // Optional leading minus.
        if (i < len && json.charAt(i) == '-') {
            i++;
        }
        // Integer part.
        while (i < len && Character.isDigit(json.charAt(i))) {
            i++;
        }
        // Optional decimal part.
        if (i < len && json.charAt(i) == '.') {
            i++;
            while (i < len && Character.isDigit(json.charAt(i))) {
                i++;
            }
        }
        // Optional exponent.
        if (i < len && (json.charAt(i) == 'e' || json.charAt(i) == 'E')) {
            i++;
            if (i < len && (json.charAt(i) == '+' || json.charAt(i) == '-')) {
                i++;
            }
            while (i < len && Character.isDigit(json.charAt(i))) {
                i++;
            }
        }
        return i;
    }

    /**
     * Return the index of the first non-whitespace character at or after
     * {@code from}, or {@code json.length()} if only whitespace remains.
     *
     * @param json the source string
     * @param from the starting index
     * @return index of the next non-whitespace character
     */
    private static int skipWhitespace(String json, int from) {
        int i   = from;
        int len = json.length();
        while (i < len && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        return i;
    }
}