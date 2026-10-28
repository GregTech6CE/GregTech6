package gregtech.util;

/** Adds one empty line between TOML settings while retaining comments and user values. */
public final class ConfigSpacing {
    private ConfigSpacing() {}

    public static String format(String text) {
        // Multiline strings may contain apparent keys/comments; leave their contents untouched.
        if (text.contains("\"\"\"") || text.contains("'''")) return text;
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        StringBuilder out = new StringBuilder();
        String previous = "";
        for (String line : text.split("\\R", -1)) {
            String trimmed = line.stripLeading();
            boolean startsEntry =
                    trimmed.startsWith("#") || trimmed.startsWith("[") || trimmed.matches("[A-Za-z0-9_.-]+\\s*=.*");
            if (startsEntry && !previous.isBlank() && !previous.stripLeading().startsWith("#")) out.append(newline);
            out.append(line).append(newline);
            previous = line;
        }
        // split(-1) preserves the trailing empty element; do not grow the file on every reload.
        if (text.endsWith("\n")) out.setLength(out.length() - newline.length());
        return out.toString();
    }
}
