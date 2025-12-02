package edu.cnu.mdi.graphics.text;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A snippet of text with its own font and relative offset, used for
 * LaTeX-like compound strings. Parsed via backslash control codes.
 *
 * Controls (at start of a token after '\'):
 *   n  : newline
 *   d  : reset to base font
 *   b  : turn on bold
 *   B  : turn off bold
 *   i  : turn on italic
 *   I  : turn off italic
 *   p  : plain (no bold/italic)
 *   _  : subscript (smaller, shifted down)
 *   ^  : superscript (smaller, shifted up)
 *   +  : bigger font
 *   -  : smaller font
 *   s  : small horizontal space
 *   S  : big horizontal space
 */
public class Snippet {

    // control characters
    private static final char NEWLINE   = 'n';
    private static final char BASE      = 'd';
    private static final char BOLD      = 'b';
    private static final char ITALIC    = 'i';
    private static final char UNBOLD    = 'B';
    private static final char UNITALIC  = 'I';
    private static final char PLAIN     = 'p';
    private static final char SUB       = '_';
    private static final char SUP       = '^';
    private static final char BIGGER    = '+';
    private static final char SMALLER   = '-';
    private static final char SMALLSPACE= 's';
    private static final char BIGSPACE  = 'S';

    // relative offsets from the main string’s baseline
    private final int deltaX;
    private final int deltaY;

    // font and text for this snippet
    private final Font font;
    private final String text;

    public Snippet(int deltaX, int deltaY, Font font, String text) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.font = font;
        this.text = text;
    }

    /**
     * Draw this snippet as part of a rotated compound string.
     *
     * @param g            Graphics context
     * @param xo           x pixel coordinate of main baseline
     * @param yo           y pixel coordinate of main baseline
     * @param angleDegrees rotation angle in degrees
     */
    public void drawSnippet(Graphics g, int xo, int yo, double angleDegrees) {
        TextPainter.drawRotatedText((Graphics2D) g, text, font,
                xo, yo, deltaX, deltaY, angleDegrees);
    }

    public int getDeltaX() {
        return deltaX;
    }

    public int getDeltaY() {
        return deltaY;
    }

    public Font getFont() {
        return font;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("text: ").append(text).append('\n');
        sb.append("delX: ").append(deltaX).append('\n');
        sb.append("delY: ").append(deltaY).append('\n');
        sb.append("font: ").append(font);
        return sb.toString();
    }

    /**
     * Compute the size of this snippet.
     */
    public Dimension size(Component c) {
        FontMetrics fm = c.getFontMetrics(font);
        int w = fm.stringWidth(text);
        return new Dimension(w, fm.getHeight());
    }

    /**
     * Parse a compound string into snippets.
     *
     * @param baseFont  baseline font
     * @param cstr      compound string with backslash controls
     * @param component for FontMetrics
     * @return list of Snippets in drawing order
     */
    public static ArrayList<Snippet> getSnippets(Font baseFont, String cstr, Component component) {

        // port your old UnicodeSupport here
        cstr = UnicodeSupport.specialCharReplace(cstr);

        if (baseFont == null || cstr == null || component == null) {
            return null;
        }

        String[] tokens = tokens(cstr, "\\");
        int len = tokens.length;

        boolean slashSet = false;
        Font currentFont = cloneFont(baseFont);
        ArrayList<Snippet> snippets = new ArrayList<>(tokens.length);

        int delX = 0;
        int delY = 0;
        int extra = 0; // for sub/sup vertical offsets

        for (int index = 0; index < len; index++) {
            String s = tokens[index];

            // token is just "\"?
            if (!slashSet && isDelimiter(s)) {
                slashSet = true;
            } else {
                if (slashSet && s.length() > 0) {
                    char firstChar = s.charAt(0);

                    if (NEWLINE == firstChar) {
                        s = s.substring(1);
                        delX = 0;
                        delY += component.getFontMetrics(currentFont).getHeight();
                    }

                    if (BASE == firstChar) {
                        s = s.substring(1);
                        currentFont = cloneFont(baseFont);
                        extra = 0;
                    } else if (BOLD == firstChar) {
                        s = s.substring(1);
                        currentFont = bold(currentFont);
                    } else if (UNBOLD == firstChar) {
                        s = s.substring(1);
                        currentFont = unbold(currentFont);
                    } else if (ITALIC == firstChar) {
                        s = s.substring(1);
                        currentFont = italic(currentFont);
                    } else if (UNITALIC == firstChar) {
                        s = s.substring(1);
                        currentFont = unitalic(currentFont);
                    } else if (PLAIN == firstChar) {
                        s = s.substring(1);
                        currentFont = plain(currentFont);
                    } else if (SMALLSPACE == firstChar) {
                        s = s.substring(1);
                        delX += 2;
                    } else if (BIGSPACE == firstChar) {
                        s = s.substring(1);
                        delX += 8;
                    } else if (SUB == firstChar) {
                        int vs = currentFont.getSize() / 4;
                        int del = currentFont.getSize() / 5;
                        extra += vs;
                        s = s.substring(1);
                        currentFont = smaller(currentFont, del);
                    } else if (SUP == firstChar) {
                        int vs = currentFont.getSize() / 4;
                        int del = currentFont.getSize() / 5;
                        extra -= vs;
                        s = s.substring(1);
                        currentFont = smaller(currentFont, del);
                    } else if (BIGGER == firstChar) {
                        int del = currentFont.getSize() / 4;
                        s = s.substring(1);
                        currentFont = bigger(currentFont, del);
                    } else if (SMALLER == firstChar) {
                        int del = currentFont.getSize() / 4;
                        s = s.substring(1);
                        currentFont = smaller(currentFont, del);
                    }
                }

                // create snippet with current font and offsets
                Snippet snippet = new Snippet(delX, delY + extra, currentFont, s);
                snippets.add(snippet);
                Dimension d = snippet.size(component);
                delX += d.width;

                slashSet = false;
            }
        }

        return snippets;
    }

    // --- font helpers (replace old Fonts.commonFont) -------------------------

    private static Font cloneFont(Font font) {
        return new Font(font.getName(), font.getStyle(), font.getSize());
    }

    private static Font smaller(Font font, int del) {
        int newSize = Math.max(4, font.getSize() - del);
        return font.deriveFont(font.getStyle(), newSize);
    }

    private static Font bigger(Font font, int del) {
        int newSize = font.getSize() + del;
        return font.deriveFont(font.getStyle(), newSize);
    }

    private static Font plain(Font font) {
        return font.deriveFont(Font.PLAIN, font.getSize());
    }

    private static Font bold(Font font) {
        int style = font.getStyle() | Font.BOLD;
        return font.deriveFont(style, font.getSize());
    }

    private static Font italic(Font font) {
        int style = font.getStyle() | Font.ITALIC;
        return font.deriveFont(style, font.getSize());
    }

    private static Font unbold(Font font) {
        int style = font.getStyle() & ~Font.BOLD;
        return font.deriveFont(style, font.getSize());
    }

    private static Font unitalic(Font font) {
        int style = font.getStyle() & ~Font.ITALIC;
        return font.deriveFont(style, font.getSize());
    }

    // --- tokenization helpers ------------------------------------------------

    private static boolean isDelimiter(String s) {
        return s != null && s.length() == 1 && s.charAt(0) == '\\';
    }

    private static String[] tokens(String str, String delimiter) {
        StringTokenizer t = new StringTokenizer(str, delimiter, true);
        int num = t.countTokens();
        String[] lines = new String[num];
        for (int i = 0; i < num; i++) {
            lines[i] = t.nextToken();
        }
        return lines;
    }
}
