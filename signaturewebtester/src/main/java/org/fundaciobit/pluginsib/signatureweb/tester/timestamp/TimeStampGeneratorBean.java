package org.fundaciobit.pluginsib.signatureweb.tester.timestamp;

import org.fundaciobit.plugins.signature.api.ITimeStampGenerator;
import org.fundaciobit.plugins.timestamp.api.ITimeStampPlugin;
import org.fundaciobit.pluginsib.core.utils.PluginsManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

@Named
@ApplicationScoped
public class TimeStampGeneratorBean {

    private static final Logger LOG = Logger.getLogger(TimeStampGeneratorBean.class.getName());

    private ITimeStampGenerator timeStampGenerator;

    @PostConstruct
    protected void init() {
        String configDir = System.getProperty("org.fundaciobit.pluginsib.signatureweb.path");
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(configDir + "/timestamp.properties")) {
            properties.load(inputStream);
        } catch (IOException ioException) {
            throw new RuntimeException("Error llegint timestamp.properties", ioException);
        }

        String classProperty = "plugins.timestamp.class";
        ITimeStampPlugin plugin = (ITimeStampPlugin) PluginsManager.instancePluginByProperty(classProperty, "", properties);
        LOG.info("Inicialitzat plugin timestamp " + plugin.getClass().getSimpleName());
        timeStampGenerator = new TimeStampPluginGenerator(plugin);
    }

    @Produces
    public ITimeStampGenerator getTimeStampGenerator() {
        return timeStampGenerator;
    }
}
