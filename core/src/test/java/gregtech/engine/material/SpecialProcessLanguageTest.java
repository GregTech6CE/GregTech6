package gregtech.engine.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SpecialProcessLanguageTest {
    private static final Pattern PROCESS_KEYS = Pattern.compile("\"(gt6ce\\.special_process\\.[^\"]+)\"\\s*:");

    @Test
    void specialProcessLanguagesHaveTheSameKeys() throws Exception {
        var english = keys("en_us.json");
        var chinese = keys("zh_cn.json");
        assertEquals(english, chinese);
        for (var method : gregtech.content.material.SpecialProcessMethods.methods())
            assertTrue(english.contains(method.displayNameKey()), method.displayNameKey());
    }

    private static Set<String> keys(String language) throws Exception {
        var text =
                Files.readString(Path.of("src/main/resources/assets/gt6ce/lang").resolve(language));
        var keys = new HashSet<String>();
        var matcher = PROCESS_KEYS.matcher(text);
        while (matcher.find()) keys.add(matcher.group(1));
        return keys;
    }
}
