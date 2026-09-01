package by.rayden.paracoder.win32native;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@Primary
public class OsNativeWindowsFFM implements OsNative {

    private enum Shell32Error {
        ERROR_UNKNOWN(-1),
        ERROR_FILE_NOT_FOUND(2),
        ERROR_PATH_NOT_FOUND(3),
        ERROR_ACCESS_DENIED(5),
        ERROR_SHARING_VIOLATION(32);

        @Getter
        private final int code;

        private static final Map<Integer, Shell32Error> errors = Arrays
            .stream(Shell32Error.values())
            .collect(Collectors.toUnmodifiableMap(Shell32Error::getCode, Function.identity()));

        Shell32Error(int code) {
            this.code = code;
        }

        public static Shell32Error getErrorByCode(int code) {
            return errors.getOrDefault(code, ERROR_UNKNOWN);
        }
    }

    @Override
    public String[] getUnicodeCommandLine() {
        try {
            String cmdLine = Kernel32_FFM.INSTANCE.getCommandLineW();
            return getCommandLineToArgv(cmdLine);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing program arguments using FFM", e);
        }
    }

    /**
     * Parses a command line string and returns an array of Strings of the command
     * line arguments.
     *
     * @param cmdLine A string that contains the full command line. If this parameter is
     *                  an empty string the function returns the path to the current
     *                  executable file.
     * @return An array of strings, similar to {@code argv}.
     */
    @Override
    public String[] getCommandLineToArgv(String cmdLine) {
        return Shell32_FFM.INSTANCE.commandLineToArgvW(cmdLine).toArray(new String[0]);
    }

    /**
     * Deletes one or multiple files to recycle bin.
     * @param paths paths to files
     */
    @Override
    public void deleteToTrash(Path... paths) throws IOException {
        String[] absPaths = new String[paths.length];
        Arrays.setAll(absPaths, i -> paths[i].toAbsolutePath().toString());

        int errorCode = Shell32_FFM.INSTANCE.shFileOperation(absPaths);
        if (errorCode != 0) {
            throw new IOException("Error on deleting source file to the trash: "
                + Arrays.toString(absPaths)
                + ". ErrorCode=" + errorCode
                + ". " + Shell32Error.getErrorByCode(errorCode));
        }
    }

}
