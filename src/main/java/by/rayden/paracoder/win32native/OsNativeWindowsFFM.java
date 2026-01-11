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
        String cmdLine = Kernel32_FFM.INSTANCE.getCommandLineW();
        return getCommandLineToArgv(cmdLine);
    }

    @Override
    public String[] getCommandLineToArgv(String cmdLine) {
        return Shell32_FFM.INSTANCE.commandLineToArgvW(cmdLine).toArray(new String[0]);
    }

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
