package by.rayden.paracoder.cli;

import by.rayden.paracoder.win32native.OsNative;
import by.rayden.paracoder.win32native.OsNativeWindowsImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnicodeCommandLineTest {
    private static UnicodeCommandLine unicodeCommandLine;

    @BeforeAll
    static void setUp() {
        OsNative osNative = new OsNativeWindowsImpl();
        unicodeCommandLine = new UnicodeCommandLine(osNative);
    }

    @Test
    void whenGetJarFileNameFromJarPathThenReturnJarFileName() {
        String jarFileName = unicodeCommandLine
            .getJarFileName("d:\\java\\prj\\paracoder\\build\\libs\\ParaCoder-1.0.2.jar");

        assertThat(jarFileName).isEqualTo("ParaCoder-1.0.2.jar");
    }

    @Test
    void whenGetJarFileNameFromNonJarThenReturnEmptyString() {
        String jarFileName = unicodeCommandLine.getJarFileName("ghjgjgg");

        assertThat(jarFileName).isEmpty();
    }

    @Test
    void whenGetJarFileNameFromEmptyThenReturnEmptyString() {
        String jarFileName = unicodeCommandLine.getJarFileName("");

        assertThat(jarFileName).isEmpty();
    }

    @Test
    void getCodeSourcePath() throws URISyntaxException {
        String codeSourcePath = unicodeCommandLine.getCodeSourcePath();

        // When test is running then CodeSourcePath is a path to main app class not to a JAR file.
        // Like "/D:/java/prj/paracoder/build/classes/java/main/"
        assertThat(codeSourcePath).endsWith("/build/classes/java/main/");
    }

    @Test
    void whenRunFromJarThenGetArgumentsOnlyReturnValidArguments() {
        String[] commandLine = new String[]{
            "C:\\Users\\rayden\\.jdks\\corretto-21.0.1\\bin\\java.exe",
            "-agentlib:jdwp=transport=dt_socket",
            "address=127.0.0.1:4852",
            "server=n",
            "--enable-preview",
            "-Dfile.encoding=UTF-8",
            "-javaagent:C:\\Users\\rayden\\AppData\\Local\\JetBrains\\IntelliJIdea2023" +
                ".3\\captureAgent\\debugger-agent.jar",
            "-classpath",
            "C:\\Program Files\\JetBrains\\IntelliJ IDEA 2023.3.3\\lib\\idea_rt.jar",
            "-jar",
            "d:\\java\\prj\\paracoder\\build\\libs\\ParaCoder-1.0.2.jar",
            "-h",
            "--version"
        };
        String jarFileName = "ParaCoder-1.0.2.jar";
        List<String> argumentsOnly = unicodeCommandLine.getArgumentsOnly(commandLine);

        assertThat(argumentsOnly).containsExactly("-h", "--version");
    }

    @Test
    void whenRunFromClassThenGetArgumentsOnlyReturnValidArguments() {
        String[] commandLine = new String[]{
            "C:\\Users\\rayden\\.jdks\\corretto-21.0.1\\bin\\java.exe",
            "-agentlib:jdwp=transport=dt_socket",
            "address=127.0.0.1:4852",
            "server=n",
            "--enable-preview",
            "-Dfile.encoding=UTF-8",
            "-cp",
            "D:\\java\\prj\\paracoder\\build\\classes\\java\\main;D:\\java\\prj\\paracoder\\build\\resources\\main;",
            "by.rayden.paracoder.ParallelCoderApplication",
            "-h",
            "--version"
        };
        String jarFileName = "ParaCoder-1.0.2.jar";
        List<String> argumentsOnly = unicodeCommandLine.getArgumentsOnly(commandLine);

        assertThat(argumentsOnly).containsExactly("-h", "--version");
    }
}
