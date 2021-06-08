package org.fundaciobit.plugins.signatureweb.tester;

import org.fundaciobit.plugins.signatureweb.api.ISignatureWebPlugin;
import org.fundaciobit.plugins.signatureweb.api.SignaturesSetWeb;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/signResult")
public class SignResultServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String ssid = request.getParameter("ssid");
        if (ssid == null || ssid.isEmpty()) {
            response.sendError(400, "No s'ha rebut pàmetre ssid");
            return;
        }

        ISignatureWebPlugin plugin = (ISignatureWebPlugin) getServletContext().getAttribute(ssid);
        if (plugin == null) {
            response.sendError(400, "SSID invàlid: " + ssid);
            return;
        }


        SignaturesSetWeb signaturesSetWeb = plugin.getSignaturesSet(ssid);
        request.setAttribute("signatureSet", signaturesSetWeb);

        getServletContext().getRequestDispatcher("/result.jsp").forward(request, response);
    }
}
