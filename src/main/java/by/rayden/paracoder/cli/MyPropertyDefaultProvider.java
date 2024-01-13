package by.rayden.paracoder.cli;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;

import static picocli.CommandLine.IDefaultValueProvider;
import static picocli.CommandLine.Model;

class MyPropertyDefaultProvider implements IDefaultValueProvider {
    private Properties properties;

    @Override
    public String defaultValue(Model.ArgSpec argSpec) throws Exception {
        if (this.properties == null) {
            this.properties = new Properties();
            File file = new File(System.getProperty("user.home"), "defaults.properties");
            try (Reader reader = new FileReader(file)) {
                this.properties.load(reader);
            }
        }
        String key = argSpec.isOption() ? ((Model.OptionSpec) argSpec).longestName() : argSpec.paramLabel();
        return this.properties.getProperty(key);
    }

}