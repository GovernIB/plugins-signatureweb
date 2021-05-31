package org.fundaciobit.plugins.signatureweb.tester;

import org.fundaciobit.plugins.signatureweb.api.ISignatureWebPlugin;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(value = "/plugin/*")
public class PluginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

        RequestTransactionInfo info = new RequestTransactionInfo(request);

        ISignatureWebPlugin plugin = (ISignatureWebPlugin) getServletContext().getAttribute(info.getSsid());
        if (plugin == null) {
            response.sendError(400, "SSID invàlid: " + info.getSsid());
            return;
        }

        boolean isPost = request.getMethod().equals("POST");
        if (isPost) {
            plugin.requestPOST(info.getBaseAbsoluta(), info.getBaseRelativa(),
                    info.getQuery(), info.getSsid(), info.getIndex(), request, response);
        } else {
            plugin.requestGET(info.getBaseAbsoluta(), info.getBaseRelativa(),
                    info.getQuery(), info.getSsid(), info.getIndex(), request, response);
        }
    }
}
