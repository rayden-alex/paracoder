package by.rayden.paracoder.win32native;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
@Component
@Primary
public class OsNativeWindowsFFM implements OsNative {

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

        int ret = Shell32_FFM.INSTANCE.shFileOperation(absPaths);
        if (ret != 0) {
            throw new IOException("Error on deleting source file to the trash: " + Arrays.toString(absPaths) + ". ErrorCode=" + ret);
        }
    }

}
