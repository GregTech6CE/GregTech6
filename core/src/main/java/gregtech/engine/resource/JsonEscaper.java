package gregtech.engine.resource;

/** Minimal JSON string escaping for resources emitted without a JSON library. */
public final class JsonEscaper {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private JsonEscaper() {}

    /** Returns {@code value} as a quoted JSON string literal. */
    public static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        appendQuoted(sb, value);
        return sb.toString();
    }

    /** Appends {@code value} to {@code sb} as a quoted JSON string literal. */
    public static void appendQuoted(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append("\\u00").append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
