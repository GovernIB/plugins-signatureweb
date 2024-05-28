package org.fundaciobit.pluginsib.signatureweb.tester;

import org.fundaciobit.pluginsib.core.utils.PluginsManager;
import org.fundaciobit.pluginsib.signatureweb.api.ISignatureWebPlugin;

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
        final String prop = "org.fundaciobit.pluginsib.signatureweb.path";
        String configDir = System.getProperty(prop);

        if (configDir == null) {
            throw new RuntimeException("No s'ha definit la Propietat de Sistema: '" + prop + "'");
        }

        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(configDir + "/plugin.properties")) {
            properties.load(inputStream);
        } catch (IOException ioException) {
            throw new RuntimeException("Error llegint plugin.properties", ioException);
        }

        String[] pluginNames = properties.getProperty("pluginsib.signatureweb").split(",");
        for (String pluginName : pluginNames) {
            String classProperty = "pluginsib.signatureweb." + pluginName + ".class";
            
            String className = properties.getProperty(classProperty);
            if (className == null) {
                throw new RuntimeException("No s'ha definit la propietat: " + classProperty);
            }
            
            ISignatureWebPlugin plugin;
            plugin = (ISignatureWebPlugin) PluginsManager.instancePluginByClassName(className, "", properties);
            
            if (plugin == null) {
                throw new RuntimeException("No s'ha pogut carregar el plugin amb classe: " + className);
            }
            
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
