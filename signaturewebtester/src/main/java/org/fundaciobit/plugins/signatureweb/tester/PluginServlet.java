package org.fundaciobit.plugins.signatureweb.tester;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(value = "/plugin/*")
public class PluginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private SignTransaction signTransaction;

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) {

        signTransaction.request(request, response);

    }
}
