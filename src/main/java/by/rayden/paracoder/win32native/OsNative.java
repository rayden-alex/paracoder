package by.rayden.paracoder.win32native;

import com.sun.jna.WString;

import java.io.File;
import java.io.IOException;

public interface OsNative {
    String[] getUnicodeCommandLine();

    String[] getCommandLineToArgv(WString lpCmdLine);

    void deleteToTrash(File... files) throws IOException;
}
