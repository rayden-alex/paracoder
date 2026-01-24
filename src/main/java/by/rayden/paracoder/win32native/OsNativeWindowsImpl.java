package by.rayden.paracoder.win32native;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
@Component
public class OsNativeWindowsImpl implements OsNative {

    @Override
    public String[] getUnicodeCommandLine() {
        try {
            WString lpCmdLine = Kernel32.INSTANCE.GetCommandLineW();
            return getCommandLineToArgv(lpCmdLine.toString());
        } catch (Throwable t) {
            throw new RuntimeException("Error parsing program arguments using JNA", t);
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
        IntByReference nArgs = new IntByReference();
        Pointer strArr = Shell32.INSTANCE.CommandLineToArgvW(new WString(cmdLine), nArgs);
        if (strArr == null) {
            throw new RuntimeException("Error at JNA call Shell32.CommandLineToArgvW");
        }

        try {
            return strArr.getWideStringArray(0, nArgs.getValue());
        } finally {
            Kernel32.INSTANCE.LocalFree(strArr);
        }
    }

    @Override
    public void deleteToTrash(Path... paths) throws IOException {
        Shell32.SHFILEOPSTRUCT fileOp = new Shell32.SHFILEOPSTRUCT();
        fileOp.wFunc = Shell32.FO_DELETE;
        fileOp.fFlags = Shell32.FOF_ALLOWUNDO | Shell32.FOF_NO_UI;

        String[] absPaths = new String[paths.length];
        Arrays.setAll(absPaths, i -> paths[i].toAbsolutePath().toString());
        fileOp.pFrom = fileOp.encodePaths(absPaths);

        int ret = Shell32.INSTANCE.SHFileOperation(fileOp);
        if (ret != 0) {
            throw new IOException("Error on deleting source file to the trash: " + fileOp.pFrom + ". ErrorCode=" + ret);
        }
        if (fileOp.fAnyOperationsAborted) {
            throw new IOException("Move to trash aborted");
        }
    }
}
