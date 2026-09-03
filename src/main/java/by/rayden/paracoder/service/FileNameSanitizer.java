package by.rayden.paracoder.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collector;

/// Replace forbidden characters in the file name with Unicode equivalents,<p/>
/// while preserve existing Unicode codePoints unchanged.
@Component
@Slf4j
public class FileNameSanitizer {
    /// Map to replace invalid characters in file name to Unicode HomoGlyphs.<p/>
    /// It contain a Unicode codePoints (char casted to integer)
    private static final Map<Integer, Integer> SANITIZE_FILENAME_MAP = Map.of(
        (int) '\t', (int) ' ',
        (int) '"', (int) '″',
        (int) '/', (int) '╱',
        (int) '\\', (int) '⧹',
        (int) '|', (int) '￨',
        (int) '?', (int) '？',
        (int) ':', (int) '∶',
        (int) '*', (int) '∗',
        (int) '<', (int) '˂',
        (int) '>', (int) '˃'
    );

    public String sanitize(String name) {
        int capacity = name.length();

        var toStringByStringBuilder = Collector.of(
            () -> new StringBuilder(capacity),
            StringBuilder::appendCodePoint,
            StringBuilder::append,
            StringBuilder::toString);

        return name.codePoints()
                   .mapToObj(codePoint -> SANITIZE_FILENAME_MAP.getOrDefault(codePoint, codePoint))
                   .collect(toStringByStringBuilder);
    }

}
