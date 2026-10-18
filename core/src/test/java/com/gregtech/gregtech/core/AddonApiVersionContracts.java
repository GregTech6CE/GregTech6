package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.GregTechAddons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** mod_version minor matches API_VERSION, and forRemoval entries are not due. */
public final class AddonApiVersionContracts {
    private static final Pattern MINOR = Pattern.compile("0\\.(0|[1-9]\\d*)");
    private static final Pattern MOD_VERSION = Pattern.compile("0\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)");
    private static final Pattern SINCE = Pattern.compile("since\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern FOR_REMOVAL_TRUE = Pattern.compile("forRemoval\\s*=\\s*true\\b");
    private static final Pattern FOR_REMOVAL_FALSE = Pattern.compile("forRemoval\\s*=\\s*false\\b");
    private static final Pattern ID = Pattern.compile(
            "[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*(?:#(?:<init>|<clinit>|[A-Za-z_][A-Za-z0-9_]*))?");
    private static final Set<String> MODIFIERS = Set.of(
            "public", "protected", "private", "static", "final", "abstract", "default",
            "synchronized", "native", "strictfp", "transient", "volatile", "sealed");
    private static final String EN_HEADING = "## Deprecation and removal";
    private static final String EN_HEADER = "| Entry | Deprecated | Replacement | Remove at |";
    private static final String ZH_HEADING = "## 弃用与移除";
    private static final String ZH_HEADER = "| 入口 | 弃用版本 | 替代 | 计划移除 |";
    private static int assertions;

    private AddonApiVersionContracts() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 4) throw new IllegalArgumentException(
                "expected gradle.properties, source roots, English API doc, Chinese API doc");
        selfCheck();
        var properties = Path.of(args[0]);
        var english = Path.of(args[args.length - 2]);
        var chinese = Path.of(args[args.length - 1]);
        int minor = requireApiMinor(Files.readString(properties), GregTechAddons.API_VERSION);
        var entries = new ArrayList<Entry>();
        for (int i = 1; i < args.length - 2; i++) entries.addAll(scanTree(Path.of(args[i])));
        var en = parseTable(english.toString(), Files.readString(english), EN_HEADING, EN_HEADER);
        var zh = parseTable(chinese.toString(), Files.readString(chinese), ZH_HEADING, ZH_HEADER);
        audit(minor, entries, en, zh);
        System.out.println("addon API version contracts passed: " + assertions
                + " assertions, " + entries.size() + " scheduled removals (Java 17; no game dependencies)");
    }

    private static void selfCheck() {
        var kept = scan("kept.java", String.join("\n",
                "package sample;",
                "/** class */",
                "@Deprecated",
                "class Plain {",
                "    /** @deprecated Use {@code replacement}. Removal is 0.5. */",
                "    public @Deprecated(since = \"0.3\", forRemoval = true)",
                "    @AddonApiRemoval(id = \"sample.Plain#old\", removeAt = \"0.5\")",
                "    void old() {}",
                "    String example = \"\"\"",
                "        @Deprecated(since = \"0.1\", forRemoval = true)",
                "        \"\"\";",
                "    String quoted = \"@Deprecated(since = \\\"0.1\\\", forRemoval = true)\";",
                "    // @Deprecated(since = \"0.1\", forRemoval = true)",
                "    /* @Deprecated(since = \"0.1\", forRemoval = true) */",
                "    @Deprecated(forRemoval = false)",
                "    void stay() {}",
                "}"));
        check(kept.size() == 1 && kept.get(0).id().equals("sample.Plain#old")
                && kept.get(0).since().equals("0.3") && kept.get(0).removeAt().equals("0.5"),
                "one scheduled removal survives comments, strings and text blocks");
        audit(3, kept, table("sample.Plain#old", "0.3", "{@code replacement}", "0.5"),
                table("sample.Plain#old", "0.3", "替代", "0.5"));
        expectFail("missing removal annotation", "missing AddonApiRemoval", () -> scan("missing.java", """
                /** @deprecated Use {@code next}. Removal is 0.5. */
                @Deprecated(since = "0.3", forRemoval = true)
                void old() {}
                """));
        expectFail("removal without forRemoval", "AddonApiRemoval without forRemoval", () -> scan("extra.java", """
                /** @deprecated Use {@code next}. Removal is 0.5. */
                @AddonApiRemoval(id = "sample.Api#old", removeAt = "0.5")
                void old() {}
                """));
        expectFail("forRemoval without since", "forRemoval without since", () -> scan("nosince.java", """
                /** @deprecated Use {@code next}. Removal is 0.5. */
                @Deprecated(forRemoval = true)
                @AddonApiRemoval(id = "sample.Api#old", removeAt = "0.5")
                void old() {}
                """));
        expectFail("javadoc tag", "javadoc missing @deprecated", () -> scan("nodoc.java", """
                /** Use {@code next}. Removal is 0.5. */
                @Deprecated(since = "0.3", forRemoval = true)
                @AddonApiRemoval(id = "sample.Api#old", removeAt = "0.5")
                void old() {}
                """));
        expectFail("javadoc replacement", "javadoc missing replacement", () -> scan("nolink.java", """
                /** @deprecated Replacement stays. Removal is 0.5. */
                @Deprecated(since = "0.3", forRemoval = true)
                @AddonApiRemoval(id = "sample.Api#old", removeAt = "0.5")
                void old() {}
                """));
        expectFail("javadoc removal minor", "javadoc missing removal version", () -> scan("noplan.java", """
                /** @deprecated Use {@code next}. */
                @Deprecated(since = "0.3", forRemoval = true)
                @AddonApiRemoval(id = "sample.Api#old", removeAt = "0.5")
                void old() {}
                """));
        var early = scheduled("0.3", "0.4");
        expectFail("one minor retained", "removal earlier than two minors",
                () -> audit(1, early, table("sample.Api#old", "0.3", "{@code next}", "0.4"),
                        table("sample.Api#old", "0.3", "替代", "0.4")));
        var due = scheduled("0.1", "0.3");
        expectFail("due entry", "expired removal",
                () -> audit(3, due, table("sample.Api#old", "0.1", "{@code next}", "0.3"),
                        table("sample.Api#old", "0.1", "替代", "0.3")));
        expectFail("english row", "doc table mismatch",
                () -> audit(3, kept, List.of(), table("sample.Plain#old", "0.3", "替代", "0.5")));
        expectFail("table drift", "doc tables disagree", () -> requireSameTables(
                table("sample.Plain#old", "0.3", "{@code replacement}", "0.5"),
                table("sample.Plain#old", "0.3", "替代", "0.6")));
        check(requireApiMinor("mod_version=0.3.1\n", 3) == 3, "patch release keeps the same minor");
        expectFail("minor drift", "mod_version", () -> requireApiMinor("mod_version=0.2.0\n", 3));
        expectFail("missing patch", "mod_version", () -> requireApiMinor("mod_version=0.3\n", 3));
        expectFail("major bump", "mod_version", () -> requireApiMinor("mod_version=1.3.0\n", 3));
        expectFail("leading zero", "mod_version", () -> requireApiMinor("mod_version=0.03.0\n", 3));
        check(requireApiMinor("# comment\nmod_version=0.3.0\n", 3) == 3, "commented properties still parse");
    }

    private static List<Entry> scheduled(String since, String removeAt) {
        return scan("scheduled.java", """
                /** @deprecated Use {@code next}. Removal is %s. */
                @Deprecated(since = "%s", forRemoval = true)
                @AddonApiRemoval(id = "sample.Api#old", removeAt = "%s")
                void old() {}
                """.formatted(removeAt, since, removeAt));
    }

    private static List<Row> table(String id, String since, String replacement, String removeAt) {
        return List.of(new Row(id, since, replacement, removeAt));
    }

    private static void expectFail(String label, String part, Runnable action) {
        try {
            action.run();
        } catch (AssertionError error) {
            check(error.getMessage() != null && error.getMessage().contains(part),
                    label + ": expected [" + part + "] but was [" + error.getMessage() + "]");
            return;
        }
        check(false, label + ": expected failure");
    }

    private static int requireApiMinor(String properties, int apiVersion) {
        String version = null;
        for (String raw : properties.split("\r?\n", -1)) {
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') continue;
            int split = line.indexOf('=');
            if (split < 0) continue;
            if (!line.substring(0, split).trim().equals("mod_version")) continue;
            if (version != null) fail("mod_version is declared twice");
            version = line.substring(split + 1).trim();
        }
        if (version == null) fail("mod_version is missing");
        if (!MOD_VERSION.matcher(version).matches())
            fail("mod_version must be 0.MINOR.PATCH, found " + version);
        int minor = Integer.parseInt(version.split("\\.")[1]);
        check(minor == apiVersion, "mod_version minor " + minor + " != API_VERSION " + apiVersion);
        return minor;
    }

    private static List<Entry> scanTree(Path root) throws IOException {
        var entries = new ArrayList<Entry>();
        try (var walk = Files.walk(root)) {
            var files = walk.filter(path -> path.getFileName().toString().endsWith(".java")).sorted().toList();
            for (var file : files) entries.addAll(scan(file.toString(), Files.readString(file)));
        }
        return entries;
    }

    private static List<Entry> scan(String file, String text) {
        var entries = new ArrayList<Entry>();
        var lexer = new Lexer(file, text);
        String pendingDoc = null;
        String clusterDoc = null;
        var cluster = new ArrayList<Annotation>();
        while (true) {
            var token = lexer.next();
            if (token == null) break;
            switch (token.kind) {
                case JAVADOC -> {
                    if (!cluster.isEmpty()) {
                        entries.addAll(finish(file, clusterDoc, cluster));
                        cluster.clear();
                    }
                    pendingDoc = token.text;
                }
                case ANNOTATION -> {
                    if (cluster.isEmpty()) clusterDoc = pendingDoc;
                    cluster.add(new Annotation(token.text, token.args));
                }
                case MODIFIER -> { }
                case CODE -> {
                    if (!cluster.isEmpty()) {
                        entries.addAll(finish(file, clusterDoc, cluster));
                        cluster.clear();
                        clusterDoc = null;
                    }
                    pendingDoc = null;
                }
            }
        }
        if (!cluster.isEmpty()) entries.addAll(finish(file, clusterDoc, cluster));
        return entries;
    }

    private static List<Entry> finish(String file, String javadoc, List<Annotation> cluster) {
        Annotation deprecated = null;
        Annotation removal = null;
        for (var annotation : cluster) {
            String simple = annotation.simpleName();
            if (simple.equals("Deprecated")) {
                if (deprecated != null) fail(file + ": two @Deprecated on one entry");
                deprecated = annotation;
            } else if (simple.equals("AddonApiRemoval")) {
                if (removal != null) fail(file + ": two @AddonApiRemoval on one entry");
                removal = annotation;
            }
        }
        boolean forRemoval = deprecated != null && forRemoval(file, deprecated.args);
        if (!forRemoval && removal == null) return List.of();
        if (!forRemoval) fail(file + ": AddonApiRemoval without forRemoval");
        if (removal == null) fail(file + ": missing AddonApiRemoval");
        String since = attribute(deprecated.args, SINCE);
        if (since == null) fail(file + ": forRemoval without since");
        int sinceMinor = minor(file, "since", since);
        String removeAt = attribute(removal.args, Pattern.compile("removeAt\\s*=\\s*\"([^\"]*)\""));
        if (removeAt == null) fail(file + ": AddonApiRemoval missing removeAt");
        int removeMinor = minor(file, "removeAt", removeAt);
        String id = attribute(removal.args, Pattern.compile("id\\s*=\\s*\"([^\"]*)\""));
        if (id == null || !ID.matcher(id).matches()) fail(file + ": AddonApiRemoval id " + id);
        if (javadoc == null || !javadoc.contains("@deprecated"))
            fail(file + ": javadoc missing @deprecated for " + id);
        if (!javadoc.contains("{@link") && !javadoc.contains("{@code"))
            fail(file + ": javadoc missing replacement for " + id);
        if (!javadoc.contains(removeAt))
            fail(file + ": javadoc missing removal version for " + id);
        return List.of(new Entry(id, since, sinceMinor, removeAt, removeMinor));
    }

    private static boolean forRemoval(String file, String args) {
        boolean yes = FOR_REMOVAL_TRUE.matcher(args).find();
        boolean no = FOR_REMOVAL_FALSE.matcher(args).find();
        if (yes && no) fail(file + ": forRemoval is both true and false");
        if (args.contains("forRemoval") && !yes && !no) fail(file + ": forRemoval is not true or false");
        return yes;
    }

    private static String attribute(String args, Pattern pattern) {
        Matcher matcher = pattern.matcher(args);
        String value = null;
        while (matcher.find()) {
            if (value != null) return null;
            value = matcher.group(1);
        }
        return value;
    }

    private static int minor(String file, String field, String value) {
        if (!MINOR.matcher(value).matches()) fail(file + ": " + field + " must be 0.MINOR, found " + value);
        return Integer.parseInt(value.substring(2));
    }

    private static void audit(int currentMinor, List<Entry> entries, List<Row> english, List<Row> chinese) {
        var seen = new HashMap<String, Entry>();
        for (var entry : entries) {
            if (seen.put(entry.id(), entry) != null) fail("duplicate removal id " + entry.id());
            check(entry.removeMinor() >= entry.sinceMinor() + 2,
                    "removal earlier than two minors: " + entry.id()
                            + " since " + entry.since() + " removeAt " + entry.removeAt());
            check(entry.removeMinor() > currentMinor,
                    "expired removal: " + entry.id() + " removeAt " + entry.removeAt()
                            + " current 0." + currentMinor);
        }
        matchTable("ADDON_API.md", entries, english);
        matchTable("ADDON_API.zh-CN.md", entries, chinese);
        requireSameTables(english, chinese);
    }

    private static void matchTable(String doc, List<Entry> entries, List<Row> rows) {
        var byId = new HashMap<String, Row>();
        for (var row : rows) {
            if (byId.put(row.id(), row) != null) fail("doc table mismatch: " + doc + " repeats " + row.id());
        }
        check(byId.size() == entries.size(),
                "doc table mismatch: " + doc + " has " + rows.size() + " rows, source has " + entries.size());
        for (var entry : entries) {
            var row = byId.get(entry.id());
            check(row != null && row.since().equals(entry.since()) && row.removeAt().equals(entry.removeAt()),
                    "doc table mismatch: " + doc + " " + entry.id());
        }
    }

    private static void requireSameTables(List<Row> english, List<Row> chinese) {
        var byId = new HashMap<String, Row>();
        for (var row : chinese) byId.put(row.id(), row);
        check(english.size() == chinese.size(), "doc tables disagree");
        for (var row : english) {
            var other = byId.get(row.id());
            check(other != null && other.since().equals(row.since()) && other.removeAt().equals(row.removeAt()),
                    "doc tables disagree: " + row.id());
        }
    }

    private static List<Row> parseTable(String file, String text, String heading, String header) {
        var lines = text.split("\r?\n", -1);
        int section = -1;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().equals(heading)) continue;
            if (section >= 0) fail(file + ": duplicate " + heading);
            section = i;
        }
        if (section < 0) fail(file + ": missing " + heading);
        int headerAt = -1;
        for (int i = section + 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("## ")) break;
            if (line.equals(header)) {
                headerAt = i;
                break;
            }
        }
        if (headerAt < 0) fail(file + ": missing deprecation table");
        if (headerAt + 1 >= lines.length
                || !lines[headerAt + 1].trim().matches("\\|\\s*-+\\s*\\|\\s*-+\\s*\\|\\s*-+\\s*\\|\\s*-+\\s*\\|"))
            fail(file + ": deprecation table separator");
        var rows = new ArrayList<Row>();
        for (int i = headerAt + 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) break;
            if (!line.startsWith("|")) fail(file + ": deprecation table row");
            String[] cells = line.split("\\|", -1);
            if (cells.length != 6) fail(file + ": deprecation table row " + line);
            String id = cells[1].trim();
            String since = cells[2].trim();
            String replacement = cells[3].trim();
            String removeAt = cells[4].trim();
            if (replacement.isEmpty() || replacement.equals("-") || replacement.equals("—"))
                fail(file + ": empty replacement for " + id);
            rows.add(new Row(id, since, replacement, removeAt));
        }
        return rows;
    }

    private static void check(boolean passed, String message) {
        assertions++;
        if (!passed) throw new AssertionError(message);
    }

    private static void fail(String message) {
        check(false, message);
    }

    private record Entry(String id, String since, int sinceMinor, String removeAt, int removeMinor) {}
    private record Row(String id, String since, String replacement, String removeAt) {}
    private record Annotation(String name, String args) {
        String simpleName() {
            int dot = name.lastIndexOf('.');
            return dot < 0 ? name : name.substring(dot + 1);
        }
    }

    private static final class Lexer {
        private final String file;
        private final String text;
        private int i;

        private Lexer(String file, String text) {
            this.file = file;
            this.text = text;
        }

        private Token next() {
            int n = text.length();
            while (i < n) {
                char c = text.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                    i++;
                    continue;
                }
                if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                    while (i < n && text.charAt(i) != '\n') i++;
                    continue;
                }
                if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                    String javadoc = readBlock();
                    if (javadoc != null) return new Token(Token.Kind.JAVADOC, javadoc, "");
                    continue;
                }
                if (c == '"') {
                    readString();
                    return new Token(Token.Kind.CODE, "", "");
                }
                if (c == '\'') {
                    readChar();
                    return new Token(Token.Kind.CODE, "", "");
                }
                if (c == '@') {
                    Token annotation = readAnnotation();
                    if (annotation != null) return annotation;
                    return new Token(Token.Kind.CODE, "", "");
                }
                if (isIdentStart(c)) {
                    int start = i;
                    i++;
                    while (i < n && isIdentPart(text.charAt(i))) i++;
                    String word = text.substring(start, i);
                    if (MODIFIERS.contains(word)) return new Token(Token.Kind.MODIFIER, word, "");
                    return new Token(Token.Kind.CODE, word, "");
                }
                i++;
                return new Token(Token.Kind.CODE, "", "");
            }
            return null;
        }

        private Token readAnnotation() {
            int start = i + 1;
            if (start >= text.length() || !isIdentStart(text.charAt(start))) {
                i++;
                return null;
            }
            int end = readDottedName(start);
            String name = text.substring(start, end);
            if (name.equals("interface")) {
                i = end;
                return new Token(Token.Kind.CODE, name, "");
            }
            i = end;
            while (i < text.length()) {
                char c = text.charAt(i);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f') break;
                i++;
            }
            String args = "";
            if (i < text.length() && text.charAt(i) == '(') args = readArgs();
            return new Token(Token.Kind.ANNOTATION, name, args);
        }

        private int readDottedName(int at) {
            int n = text.length();
            at++;
            while (at < n && isIdentPart(text.charAt(at))) at++;
            while (at < n && text.charAt(at) == '.' && at + 1 < n && isIdentStart(text.charAt(at + 1))) {
                at += 2;
                while (at < n && isIdentPart(text.charAt(at))) at++;
            }
            return at;
        }

        private String readArgs() {
            i++;
            int start = i;
            int depth = 1;
            while (i < text.length() && depth > 0) {
                char c = text.charAt(i);
                if (c == '"') {
                    readString();
                    continue;
                }
                if (c == '\'') {
                    readChar();
                    continue;
                }
                if (c == '(') depth++;
                else if (c == ')') depth--;
                i++;
            }
            if (depth != 0) fail(file + ": unclosed annotation");
            return text.substring(start, i - 1);
        }

        private String readBlock() {
            boolean javadoc = i + 2 < text.length() && text.charAt(i + 2) == '*'
                    && !(i + 3 < text.length() && text.charAt(i + 3) == '/');
            int start = i;
            i += 2;
            while (i + 1 < text.length() && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) i++;
            if (i + 1 >= text.length()) fail(file + ": unclosed comment");
            i += 2;
            return javadoc ? text.substring(start, i) : null;
        }

        private void readString() {
            if (i + 2 < text.length() && text.charAt(i + 1) == '"' && text.charAt(i + 2) == '"') {
                i += 3;
                while (i + 2 < text.length() && !(text.charAt(i) == '"' && text.charAt(i + 1) == '"'
                        && text.charAt(i + 2) == '"')) {
                    if (text.charAt(i) == '\\') i += 2;
                    else i++;
                }
                if (i + 2 >= text.length()) fail(file + ": unclosed text block");
                i += 3;
                return;
            }
            i++;
            while (i < text.length()) {
                char c = text.charAt(i);
                if (c == '\\') {
                    i += 2;
                    continue;
                }
                if (c == '"') {
                    i++;
                    return;
                }
                if (c == '\n') fail(file + ": unclosed string");
                i++;
            }
            fail(file + ": unclosed string");
        }

        private void readChar() {
            i++;
            if (i < text.length() && text.charAt(i) == '\\') i += 2;
            else i++;
            if (i >= text.length() || text.charAt(i) != '\'') fail(file + ": unclosed character");
            i++;
        }

        private static boolean isIdentStart(char c) {
            return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_' || c == '$';
        }

        private static boolean isIdentPart(char c) {
            return isIdentStart(c) || c >= '0' && c <= '9';
        }
    }

    private static final class Token {
        private enum Kind { JAVADOC, ANNOTATION, MODIFIER, CODE }
        private final Kind kind;
        private final String text;
        private final String args;

        private Token(Kind kind, String text, String args) {
            this.kind = kind;
            this.text = text;
            this.args = args;
        }
    }
}
