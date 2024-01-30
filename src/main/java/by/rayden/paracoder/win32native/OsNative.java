package by.rayden.paracoder.win32native;

import java.io.File;
import java.io.IOException;

public interface OsNative {
    String[] getUnicodeCommandLine();

    void deleteToTrash(File... files) throws IOException;
}
