package by.rayden.paracoder.win32native;

import com.sun.jna.WString;

import java.io.IOException;
import java.nio.file.Path;

public interface OsNative {
    String[] getUnicodeCommandLine();

    String[] getCommandLineToArgv(WString lpCmdLine);

    void deleteToTrash(Path... paths) throws IOException;
}
