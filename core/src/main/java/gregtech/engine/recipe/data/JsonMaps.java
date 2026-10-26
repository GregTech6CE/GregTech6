package gregtech.engine.recipe.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The JSON subset used by machine-recipe documents. Objects keep source order. */
public final class JsonMaps {
    private JsonMaps() {}

    public static Map<String, Object> object(String json) {
        var parser = new Parser(json);
        var value = parser.value();
        parser.skip();
        if (!parser.done()) throw new IllegalArgumentException("Trailing JSON");
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("JSON root must be an object");
        return cast(map);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static final class Parser {
        private final String text;
        private int cursor;

        private Parser(String text) {
            if (text == null) throw new IllegalArgumentException("JSON is missing");
            this.text = text;
        }

        private boolean done() { return cursor >= text.length(); }

        private void skip() {
            while (!done()) {
                char c = text.charAt(cursor);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') return;
                cursor++;
            }
        }

        private char peek() {
            if (done()) throw new IllegalArgumentException("Unexpected end of JSON");
            return text.charAt(cursor);
        }

        private Object value() {
            skip();
            return switch (peek()) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Object literal(String word, Object value) {
            if (!text.startsWith(word, cursor)) throw new IllegalArgumentException("Expected " + word);
            cursor += word.length();
            return value;
        }

        private Map<String, Object> object() {
            cursor++;
            var map = new LinkedHashMap<String, Object>();
            skip();
            if (peek() == '}') { cursor++; return map; }
            while (true) {
                skip();
                if (peek() != '"') throw new IllegalArgumentException("Expected a JSON key");
                var key = string();
                skip();
                if (peek() != ':') throw new IllegalArgumentException("Expected ':'");
                cursor++;
                if (map.containsKey(key)) throw new IllegalArgumentException("Duplicate key " + key);
                map.put(key, value());
                skip();
                char mark = peek();
                cursor++;
                if (mark == '}') return map;
                if (mark != ',') throw new IllegalArgumentException("Expected ',' or '}'");
            }
        }

        private List<Object> array() {
            cursor++;
            var list = new ArrayList<Object>();
            skip();
            if (peek() == ']') { cursor++; return list; }
            while (true) {
                list.add(value());
                skip();
                char mark = peek();
                cursor++;
                if (mark == ']') return list;
                if (mark != ',') throw new IllegalArgumentException("Expected ',' or ']'");
            }
        }

        private String string() {
            cursor++;
            var out = new StringBuilder();
            while (!done()) {
                char c = text.charAt(cursor++);
                if (c == '"') return out.toString();
                if (c == '\\') {
                    if (done()) throw new IllegalArgumentException("Bad escape");
                    char escaped = text.charAt(cursor++);
                    switch (escaped) {
                        case '"', '\\', '/' -> out.append(escaped);
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (cursor + 4 > text.length()) throw new IllegalArgumentException("Bad unicode escape");
                            int code = Integer.parseInt(text.substring(cursor, cursor + 4), 16);
                            cursor += 4;
                            out.append((char) code);
                        }
                        default -> throw new IllegalArgumentException("Bad escape");
                    }
                } else if (c < 0x20) {
                    throw new IllegalArgumentException("Raw control character in string");
                } else {
                    out.append(c);
                }
            }
            throw new IllegalArgumentException("Unclosed string");
        }

        private Number number() {
            int start = cursor;
            if (peek() == '-') cursor++;
            digits();
            boolean real = false;
            if (!done() && text.charAt(cursor) == '.') {
                real = true;
                cursor++;
                digits();
            }
            if (!done() && (text.charAt(cursor) == 'e' || text.charAt(cursor) == 'E')) {
                real = true;
                cursor++;
                if (!done() && (text.charAt(cursor) == '+' || text.charAt(cursor) == '-')) cursor++;
                digits();
            }
            var token = text.substring(start, cursor);
            if (real) return Double.valueOf(token);
            return Long.valueOf(token);
        }

        private void digits() {
            int start = cursor;
            while (!done() && text.charAt(cursor) >= '0' && text.charAt(cursor) <= '9') cursor++;
            if (cursor == start) throw new IllegalArgumentException("Expected a number");
        }
    }
}
