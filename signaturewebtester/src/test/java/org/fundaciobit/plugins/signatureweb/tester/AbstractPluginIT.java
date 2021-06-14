package org.fundaciobit.plugins.signatureweb.tester;

import org.junit.BeforeClass;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public abstract class AbstractPluginIT {

    protected static String endpoint;

    private static Properties properties;

    @BeforeClass
    public static void setup() throws IOException {
        properties = new Properties();
        try (var reader = new FileReader("test.properties")){
            properties.load(reader);
        }
        endpoint = properties.getProperty("endpoint");
    }

    protected String getConfig(String name) {
        return properties.getProperty(name);
    }
}
