package by.rayden.paracoder.config;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;

/**
 * @see <a href="https://www.baeldung.com/spring-yaml-propertysource">Spring-yaml-propertysource</a>
 */
//public class YamlPropertySourceFactory implements PropertySourceFactory {
//
//    @Override
//    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource encodedResource)
//        throws IOException {
//        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
//        factory.setResources(encodedResource.getResource());
//
//        Properties properties = Objects.requireNonNull(factory.getObject());
//        String filename = encodedResource.getResource().getFilename();
//        if (filename == null) {
//            throw new IOException();
//        }
//        return new PropertiesPropertySource(filename, properties);
//    }
//}

public class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource encodedResource) throws IOException {
        PropertySourceLoader loader = new YamlPropertySourceLoader();

        String filename = encodedResource.getResource().getFilename();
        boolean isYamlFile = Arrays.stream(loader.getFileExtensions())
                                   .anyMatch(ext -> StringUtils.endsWithIgnoreCase(filename, ext));
        Assert.isTrue(isYamlFile, "The property source file must be a YAML file");

        // YAML can have a multiple documents in the one file. Only the first document will be used.
        return loader.load(filename, encodedResource.getResource()).getFirst();
    }
}
