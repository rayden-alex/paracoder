package by.rayden.paracoder.service;

import by.rayden.paracoder.win32native.OsNative;
import by.rayden.paracoder.win32native.OsNativeWindowsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// TODO: Finish tests
@ExtendWith(MockitoExtension.class)
class ProcessRunnerTest {

    private static final Pattern SHOW_ARGS_REGEX = Pattern.compile("^argv\\[\\d+]: >(.*)<$", Pattern.MULTILINE);
    private static final Charset PROCESS_CHARSET = Charset.forName("cp866");

    @Test
    void testShowArgs() throws Exception {
        List<String> commands = List.of(
            "src\\test\\resources\\ShowArgs.exe",
            "p1",
            "p2");

        ProcessResult res = execCapturedProcess(commands);

        assertThat(res.err).isEmpty();
        assertThat(res.out).isNotEmpty();

        List<String> args = getArgs(res.out);

        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2");
    }

    @Test
    void testShowArgs2() throws Exception {
        String[] commands = {"cmd.exe", "/c", "src\\test\\resources\\ShowArgs.exe p1,pp \"p \\ 2\" p\\3"};

        ProcessResult res = execCapturedProcess(commands);

        assertThat(res.err).as("Command error stream:").isEmpty();
        assertThat(res.out).isNotEmpty();

        List<String> args = getArgs(res.out);

        assertThat(args).hasSize(4);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1,pp");
        assertThat(args.get(2)).isEqualTo("p \\ 2");
        assertThat(args.get(3)).isEqualTo("p\\3");
    }

    @Test
    void testShowArgs3() throws Exception {
        String[] commands = {"cmd.exe", "/d", "/c", "\"src\\test\\resources\\ShowArgs.exe\" \"p1\" p2"};

        ProcessResult res = execCapturedProcess(commands);

        assertThat(res.err).isEmpty();
        assertThat(res.out).isNotEmpty();

        List<String> args = getArgs(res.out);

        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2");
    }

    @Test
    void testCmdShowArgs() throws Exception {
        List<String> commands = List.of(
            "cmd.exe",
            "/c",
            "src\\test\\resources\\ShowArgs.exe p1 p2");

        ProcessResult res = execCapturedProcess(commands);

        assertThat(res.err).isEmpty();
        assertThat(res.out).isNotEmpty();
    }

    @Test
    void testCmdSleep() throws Exception {
        List<String> commands = List.of(
            "cmd.exe",
            "/c",
            "waitfor.exe /T 3 qqqqqqq");

        ProcessResult res = execCapturedProcess(commands);

        assertThat(res.err).isNotEmpty();
        assertThat(res.out).isEmpty();
    }

    @Test
    void testProcessFactoryWithoutPiping() throws Exception {
        RecoderThreadPool recoderThreadPool = mock(RecoderThreadPool.class);
        OsNative osNative = new OsNativeWindowsImpl();
        ProcessRunner processRunner = new ProcessRunner(recoderThreadPool, osNative);

        Process process = processRunner.runProcess("src\\test\\resources\\ShowArgs.exe p1 p2", false);
        ProcessResult res = execCapturedProcess(process);

        assertThat(res.err).isEmpty();

        List<String> args = getArgs(res.out);
        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2");
    }

    @Test
    void testProcessFactoryWithPiping() throws Exception {
        RecoderThreadPool recoderThreadPool = mock(RecoderThreadPool.class);
        OsNative osNative = new OsNativeWindowsImpl();
        ProcessRunner processRunner = new ProcessRunner(recoderThreadPool, osNative);

        Process process = processRunner.runProcess("src\\test\\resources\\ShowArgs.exe p1 \"p2 3\" | more.com /C",
            false);
        ProcessResult res = execCapturedProcess(process);

        assertThat(res.err).isEmpty();

        List<String> args = getArgs(res.out);
        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2 3");
    }

    @Test
    void testProcessFactoryWithPipingAndUnicodeParam() throws Exception {
        RecoderThreadPool recoderThreadPool = mock(RecoderThreadPool.class);
        OsNative osNative = new OsNativeWindowsImpl();
        ProcessRunner processRunner = new ProcessRunner(recoderThreadPool, osNative);

        Process process = processRunner.runProcess("src\\test\\resources\\ShowArgs.exe p1 ნიკოს | more.com /C",
            false);
        ProcessResult res = execCapturedProcess(process);

        assertThat(res.err).isEmpty();

        List<String> args = getArgs(res.out);
        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("ნიკოს");
    }


    private record ProcessResult(int exitCode, String out, String err) {

    }

    private ProcessResult execCapturedProcess(String... commands) throws Exception {
        return execCapturedProcess(Arrays.asList(commands));
    }

    private ProcessResult execCapturedProcess(List<String> commands) throws Exception {
        Process process = new ProcessBuilder().command(commands).start();
        return execCapturedProcess(process);
    }

    private ProcessResult execCapturedProcess(Process process) throws Exception {
        CompletableFuture<String> outStrFuture =
            CompletableFuture.supplyAsync(() -> readToString(process.getInputStream()));

        CompletableFuture<String> errStrFuture =
            CompletableFuture.supplyAsync(() -> readToString(process.getErrorStream()));

        int exitCode = process.waitFor();
        String outStr = outStrFuture.join();
        String errorStr = errStrFuture.join();

        return new ProcessResult(exitCode, outStr, errorStr);
    }

    private String readToString(InputStream stream) {
        try {
            return new String(stream.readAllBytes(), PROCESS_CHARSET);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts program arguments from output like this:
     * <pre> {@code
     * argc: 3
     * argv[0]: >src\test\resources\ShowArgs.exe<
     * argv[1]: >p1<
     * argv[2]: >p2<
     * }</pre>
     */
    private List<String> getArgs(String output) {
        return SHOW_ARGS_REGEX.matcher(output).results().map(matchResult -> matchResult.group(1)).toList();
    }
}
