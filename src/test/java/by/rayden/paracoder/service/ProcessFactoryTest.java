package by.rayden.paracoder.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: Finish tests
class ProcessFactoryTest {

    private static final Pattern SHOW_ARGS_REGEX = Pattern.compile("^argv\\[\\d+]: >(.*)<$", Pattern.MULTILINE);

    @Test
    void testShowArgs() throws IOException {
        List<String> commands = List.of(
            "src\\test\\resources\\ShowArgs.exe",
            "p1",
            "p2");

        ProcessResult res = execExternalProcess(commands);

        assertThat(res.err).isEmpty();
        assertThat(res.out).isNotEmpty();

        List<String> args = getArgs(res.out);

        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2");
    }

    @Test
    void testShowArgs2() throws IOException {
        String[] commands = {"cmd.exe", "/c", "src\\test\\resources\\ShowArgs.exe p1,pp \"p \\ 2\" p\\3"};

        ProcessResult res = execExternalProcess(commands);

        assertThat(res.err).as("Command error stream:").isEmpty();
        assertThat(res.out).isNotEmpty();

        List<String> args = getArgs(res.out);

        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2");
    }

    @Test
    void testShowArgs3() throws IOException {
        String[] commands = {"cmd.exe", "/d", "/c", "\"src\\test\\resources\\ShowArgs.exe\" \"p1\" p2"};

        ProcessResult res = execExternalProcess(commands);

        assertThat(res.err).isEmpty();
        assertThat(res.out).isNotEmpty();

        List<String> args = getArgs(res.out);

        assertThat(args).hasSize(3);
        assertThat(args.get(0)).endsWith("ShowArgs.exe");
        assertThat(args.get(1)).isEqualTo("p1");
        assertThat(args.get(2)).isEqualTo("p2");
    }

    @Test
    void testCmdShowArgs() throws IOException {
        List<String> commands = List.of(
            "cmd.exe",
            "/c",
            "src\\test\\resources\\ShowArgs.exe p1 p2");

        ProcessResult res = execExternalProcess(commands);

        assertThat(res.err).isEmpty();
        assertThat(res.out).isNotEmpty();
    }

    private record ProcessResult(String out, String err) {
    }

    private ProcessResult execExternalProcess(String... commands) throws IOException {
        Process process = new ProcessBuilder().command(commands).start();

        String outStr = readToString(process.getInputStream());
        String errorStr = readToString(process.getErrorStream());

        return new ProcessResult(outStr, errorStr);
    }

    private ProcessResult execExternalProcess(List<String> commands) throws IOException {
        Process process = new ProcessBuilder().command(commands).start();

        String outStr = readToString(process.getInputStream());
        String errorStr = readToString(process.getErrorStream());

        return new ProcessResult(outStr, errorStr);
    }

    private String readToString(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), Charset.forName("cp866"));
    }

    private List<String> getArgs(String output) {
        return SHOW_ARGS_REGEX.matcher(output).results().map(matchResult -> matchResult.group(1)).toList();
    }
}
