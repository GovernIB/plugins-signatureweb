package org.fundaciobit.plugins.signatureweb.tester;

import org.fundaciobit.plugins.signatureweb.api.ISignatureWebPlugin;
import org.fundaciobit.pluginsib.core.utils.PluginsManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

@Named
@ApplicationScoped
public class PluginMapBean {

    private static final Logger LOG = Logger.getLogger(PluginMapBean.class.getName());

    private final Map<String, ISignatureWebPlugin> pluginMap = new HashMap<>();

    @PostConstruct
    protected void init() {
        String configDir = System.getProperty("org.fundaciobit.plugins.signatureweb.path");
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(configDir + "/plugin.properties")) {
            properties.load(inputStream);
        } catch (IOException ioException) {
            throw new RuntimeException("Error llegint plugin.properties", ioException);
        }

        String[] pluginNames = properties.getProperty("plugins.signatureweb").split(",");
        for (String pluginName : pluginNames) {
            String classProperty = "plugins.signatureweb." + pluginName + ".class";
            ISignatureWebPlugin plugin =
                    (ISignatureWebPlugin) PluginsManager.instancePluginByProperty(classProperty, "", properties);
            pluginMap.put(pluginName, plugin);
            LOG.info("Inicialitzat: " + pluginName + ", " + plugin.getName(Locale.getDefault()));
        }
    }

    public ISignatureWebPlugin getPlugin(String pluginName) {
        return pluginMap.get(pluginName);
    }

    public boolean containsPlugin(String pluginName) {
        return pluginMap.containsKey(pluginName);
    }

    public Set<String> getPluginNames() {
        return pluginMap.keySet();
    }
}
