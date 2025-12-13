package edu.cnu.mdi.graphics.style;

public enum LineStyle {
    SOLID("Solid"),
    DASH("Dash"),
    DOT_DASH("Dot Dash"),
    DOT("Dot"),
    DOUBLE_DASH("Double Dash"),
    LONG_DASH("Long Dash"),
    LONG_DOT_DASH("Long Dot Dash");

    private final String displayName;

    LineStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
