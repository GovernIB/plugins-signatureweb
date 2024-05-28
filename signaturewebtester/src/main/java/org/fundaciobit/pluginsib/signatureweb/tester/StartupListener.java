package org.fundaciobit.pluginsib.signatureweb.tester;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Locale;
import java.util.logging.Logger;

@WebListener
public class StartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(StartupListener.class.getName());

    @Inject
    private PluginMapBean pluginMapBean;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("Aplicació iniciada");
        LOG.info("Plugins disponibles: " + pluginMapBean.getPluginNames());
        LOG.info("Platform Encoding: " + System.getProperty("file.encoding"));
        LOG.info("Default Locale: " + Locale.getDefault());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Finalitzant aplicació");
    }
}
