package by.rayden.paracoder.cli.command;

import java.nio.file.Path;
import java.util.List;

public record ParaCoderParams(List<Path> inputPathList, boolean preserveFileTimestamp, boolean preserveDirTimestamp,
                              boolean recurse) {
}

