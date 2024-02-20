package by.rayden.paracoder.cli;

import picocli.CommandLine.IVersionProvider;

import java.util.Properties;

/**
 * Demonstrates a {@link IVersionProvider} implementation that reads version information from a
 * {@code /version.txt} file that is expected to be in the root of the classpath.
 * <p>
 * The following gradle build snippet can be used to generate such a version.txt file and include it in the generated jar:
 * </p>
 * <pre>
 * def generatedResources = "$buildDir/generated-resources/main"
 * sourceSets {
 *     main {
 *         //register an output folder on the main SourceSet:
 *         output.dir(generatedResources, builtBy: 'generateVersionTxt')
 *         //it is now a part of the 'main' classpath and will be a part of the jar
 *     }
 * }
 *
 * //a task that generates the resources:
 * task generateVersionTxt {
 *     description 'Creates a version.txt file with build info that is added to the root of the jar'
 *     doLast {
 *         new File(generatedResources).mkdirs()
 *         def generated = new File(generatedResources, "version.txt")
 *         generated.text = """
 *         Version: $rootProject.version
 *         Buildtime: ${new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())}
 *         Application-name: $rootProject.name $project.name
 *         """
 *     }
 * }
 * </pre>
 *
 * See build.gradle "generateVersionTxt" task
 */
public class PropertiesVersionProvider implements IVersionProvider {
    public String[] getVersion() throws Exception {
        var properties = new Properties();
        // The file name must begin with a '/' to use an absolute path,
        // otherwise the relative path to current class will be used.
        try (var stream = getClass().getResourceAsStream("/version.txt")) {
            properties.load(stream);
        }
        return new String[]{
            STR."\{properties.getProperty("Application-name")} version @|bold,yellow \"\{properties.getProperty("Version")}\"|@",
            STR."Build-date: @|yellow \{properties.getProperty("Build-date")}|@",
            STR."Revision: @|yellow \{properties.getProperty("Revision")}|@",
            STR."Revision-date: @|yellow \{properties.getProperty("Revision-date")}|@"
        };
    }
}
