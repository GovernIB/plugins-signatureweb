package org.fundaciobit.plugins.signatureweb.tester;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@WebFilter(filterName = "Plugin Conversation Filter")
public class PluginConversationFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;
    
    private static final Logger LOG = Logger.getLogger(PluginConversationFilter.class.getName());

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        String pluginPath = request.getContextPath() + "/plugin/";

        if (requestURI.startsWith(pluginPath)) {
            int nextSlash = requestURI.indexOf("/", pluginPath.length() + 1);
            String cid = requestURI.substring(pluginPath.length(), nextSlash);
            LOG.info("CID: " + cid);

            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getParameter(String name) {
                    if ("cid".equals(name)) {
                        return cid;
                    } else {
                        return super.getParameter(name);
                    }
                }
            };
        }

        super.doFilter(request, response, chain);
    }
}
