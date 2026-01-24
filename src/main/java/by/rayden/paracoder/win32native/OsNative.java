package by.rayden.paracoder.win32native;

import java.io.IOException;
import java.nio.file.Path;

public interface OsNative {
    String[] getUnicodeCommandLine();

    String[] getCommandLineToArgv(String cmdLine);

    void deleteToTrash(Path... paths) throws IOException;
}
