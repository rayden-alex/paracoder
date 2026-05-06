package by.rayden.paracoder.service;

import by.rayden.paracoder.win32native.OsNative;
import by.rayden.paracoder.win32native.OsNativeWindowsFFM;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ParameterizedClass
@FieldSource("osNativeProvider")
@ExtendWith(MockitoExtension.class)
class ProcessRunnerTest {
    static List<OsNative> osNativeProvider = List.of(new OsNativeWindowsFFM());

    static final Pattern SHOW_ARGS_REGEX = Pattern.compile("^argv\\[\\d+]: >(.*)<$", Pattern.MULTILINE);
    static final Charset PROCESS_CHARSET = StandardCharsets.UTF_8;

    static ProcessRunner processRunner;

    ProcessRunnerTest(OsNative osNative) {
        processRunner = new ProcessRunner(mock(RecoderThreadPool.class), osNative);
    }

    @Test
    void testShowArgs() throws Exception {
        List<String> commands = List.of(
            "src\\test\\resources\\ShowArgs.exe",
            "p1",
            "p2");

        ProcessResult res = execCapturedProcess(commands);

        assertThat(res.exitCode).isZero();
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

        assertThat(res.exitCode).isZero();
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

        assertThat(res.exitCode).isZero();
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

        assertThat(res.exitCode).isZero();
        assertThat(res.err).isEmpty();
        assertThat(res.out).isNotEmpty();
    }

    @Test
    void testCmdSleep() throws Exception {
        List<String> commands = List.of(
            "cmd.exe",
            "/c",
            "chcp 65001>nul && waitfor.exe /T 1 qqqqqqq");

        ProcessResult res = execCapturedProcess(commands);

        assertThat(res.exitCode).isNotZero();
        assertThat(res.out).isEmpty();
        assertThat(res.err).isEqualTo("ERROR: Timed out waiting for 'qqqqqqq'.\r\n");
    }

    @Test
    void testProcessFactoryWithoutPiping() throws Exception {
        Process process = processRunner.runProcessWithoutRedirect("src\\test\\resources\\ShowArgs.exe p1 p2");
        ProcessResult res = execCapturedProcess(process);

        assertThat(res.exitCode).isZero();
        assertThat(res.err).isEmpty();

        List<String> args = getArgs(res.out);
        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2");
    }

    @Test
    void testProcessFactoryWithPiping() throws Exception {
        Process process = processRunner
            .runProcessWithoutRedirect("src\\test\\resources\\ShowArgs.exe p1 \"p2 3\" | more.com /C");
        ProcessResult res = execCapturedProcess(process);

        assertThat(res.exitCode).isZero();
        assertThat(res.err).isEmpty();

        List<String> args = getArgs(res.out);
        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2 3");
    }

    @Test
    void testProcessFactoryWithPipingAndUnicodeParam() throws Exception {
        String unicodeFileName = "ბენდი sløwed L‘ÂME фыва 💃🕺🎼.flac";

        Process process = processRunner.runProcessWithoutRedirect("src\\test\\resources\\ShowArgs.exe p1 \""
            + unicodeFileName + "\" | more.com /C");
        ProcessResult res = execCapturedProcess(process);

        assertThat(res.exitCode).isZero();
        assertThat(res.err).isEmpty();

        List<String> args = getArgs(res.out);
        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo(unicodeFileName);
    }

    public static Stream<Arguments> provideArgsToTestSplitCommandByPipeChar() {
        // \uD83D\uDC7B --- ghost emoji (code point 4 bytes)
        // ✅ --- (code point 2 bytes, fits into char type)
        return Stream.of(
            Arguments.of("aaa|bbb", new String[]{"aaa", "bbb"}),
            Arguments.of("aaa|bbb|ccc", new String[]{"aaa", "bbb", "ccc"}),
            Arguments.of("aaa|\"bbb|ccc\"|ddd", new String[]{"aaa", "\"bbb|ccc\"", "ddd"}),
            Arguments.of("aaa\uD83D\uDC7B|✅\"bbb✅|\uD83D\uDC7Bccc\"✅|\uD83D\uDC7Bddd",
                new String[]{"aaa\uD83D\uDC7B", "✅\"bbb✅|\uD83D\uDC7Bccc\"✅", "\uD83D\uDC7Bddd"})
        );
    }

    @ParameterizedTest
    @MethodSource("provideArgsToTestSplitCommandByPipeChar")
    void testSplitCommandByPipeChar(String command, String[] expectedCommands) {
        List<String> commandsByProcess = processRunner.splitCommandByPipeChar(command);

        assertThat(commandsByProcess).containsExactly(expectedCommands);
    }

    @Test
    void testSplitCommandByPipeChar_withOddQuotes() {
        assertThatThrownBy(() -> processRunner.splitCommandByPipeChar("aaa|bbb\"bbb|ccc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Command template error: odd number of quotes");
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

    @SneakyThrows
    private String readToString(InputStream stream) {
        return new String(stream.readAllBytes(), PROCESS_CHARSET);
    }

    /**
     * Extracts program arguments from ShowArgs.exe output like this:
     * <pre> {@code
     * argc: 3
     * argv[0]: >src\test\resources\ShowArgs.exe<
     * argv[1]: >p1<
     * argv[2]: >p2<
     * }</pre>
     */
    private List<String> getArgs(String output) {
        return SHOW_ARGS_REGEX
            .matcher(output)
            .results()
            .map(matchResult -> matchResult.group(1))
            .toList();
    }
}
