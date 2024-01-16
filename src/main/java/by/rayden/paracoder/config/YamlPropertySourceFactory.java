package by.rayden.paracoder.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource encodedResource)
        throws IOException {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(encodedResource.getResource());

        Properties properties = Objects.requireNonNull(factory.getObject());
        String filename = encodedResource.getResource().getFilename();
        if (filename == null) {
            throw new IOException();
        }
        return new PropertiesPropertySource(filename, properties);
    }
}

//public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {
//    @Override
//    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws
//    IOException {
//        if (resource == null){
//            return super.createPropertySource(name, resource);
//        }
//        List<PropertySource<?>> source = new YamlPropertySourceLoader().load(resource.getResource().getFilename(),
//        resource.getResource());
//        return source.getLast();
//    }
//}
