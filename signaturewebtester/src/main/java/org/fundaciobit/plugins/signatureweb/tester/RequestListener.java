package org.fundaciobit.plugins.signatureweb.tester;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

@WebListener
public class RequestListener implements ServletRequestListener {

    private static final Logger LOG = Logger.getLogger(RequestListener.class.getName());

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        LOG.info("Request: " + request.getRequestURI());
    }
}
