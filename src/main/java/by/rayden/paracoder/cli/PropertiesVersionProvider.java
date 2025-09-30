package by.rayden.paracoder.cli;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;
import picocli.CommandLine.IVersionProvider;

/**
 * See build.gradle "springBoot.buildInfo" and "gitProperties" tasks.
 */
@Component
public class PropertiesVersionProvider implements IVersionProvider {
    private final BuildProperties buildInfo;
    private final GitProperties gitInfo;

    public PropertiesVersionProvider(BuildProperties buildInfo, GitProperties gitInfo) {
        this.buildInfo = buildInfo;
        this.gitInfo = gitInfo;
    }

    public String[] getVersion() {
        return new String[]{
            this.buildInfo.getName() + " @|bold,yellow v" + this.buildInfo.getVersion() + "|@",
            "Build-Date: @|yellow " + this.buildInfo.getTime() + "|@",
            "Build-Revision: @|yellow " + this.gitInfo.getShortCommitId() + "|@",
            "Revision-Date: @|yellow " + this.gitInfo.getCommitTime() + "|@"
        };
    }
}
