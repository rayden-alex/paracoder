package by.rayden.paracoder.win32native;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
public class OsNativeWindowsImpl implements OsNative {

    @Override
    public String[] getUnicodeCommandLine() {
        try {
            WString lpCmdLine = Kernel32.INSTANCE.GetCommandLineW();
            return getCommandLineToArgv(lpCmdLine);
        } catch (Throwable t) {
            throw new RuntimeException("Error parsing program arguments using JNA", t);
        }
    }

    /**
     * Parses a command line string and returns an array of Strings of the command
     * line arguments.
     *
     * @param lpCmdLine A string that contains the full command line. If this parameter is
     *                  an empty string the function returns the path to the current
     *                  executable file.
     * @return An array of strings, similar to {@code argv}.
     */
    @Override
    public String[] getCommandLineToArgv(WString lpCmdLine) {
        IntByReference nArgs = new IntByReference();
        Pointer strArr = Shell32.INSTANCE.CommandLineToArgvW(lpCmdLine, nArgs);
        if (strArr != null) {
            try {
                return strArr.getWideStringArray(0, nArgs.getValue());
            } finally {
                Kernel32.INSTANCE.LocalFree(strArr);
            }
        }
        throw new RuntimeException("Error at JNA call Shell32.CommandLineToArgvW");
    }

    @Override
    public void deleteToTrash(File... files) throws IOException {
        Shell32.SHFILEOPSTRUCT fileOp = new Shell32.SHFILEOPSTRUCT();
        fileOp.wFunc = Shell32.FO_DELETE;
        fileOp.fFlags = Shell32.FOF_ALLOWUNDO | Shell32.FOF_NO_UI;

        String[] paths = new String[files.length];
        Arrays.setAll(paths, i -> files[i].getAbsolutePath());
        fileOp.pFrom = fileOp.encodePaths(paths);

        int ret = Shell32.INSTANCE.SHFileOperation(fileOp);
        if (ret != 0) {
            throw new IOException("Error on deleting source file to the trash: " + fileOp.pFrom + ". ErrorCode=" + ret);
        }
        if (fileOp.fAnyOperationsAborted) {
            throw new IOException("Move to trash aborted");
        }
    }
}
