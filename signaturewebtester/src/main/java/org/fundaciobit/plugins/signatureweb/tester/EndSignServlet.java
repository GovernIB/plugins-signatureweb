package org.fundaciobit.plugins.signatureweb.tester;

import org.fundaciobit.plugins.signature.api.StatusSignature;
import org.fundaciobit.plugins.signatureweb.api.SignaturesSetWeb;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/endSign")
public class EndSignServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private SignTransaction signTransaction;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {
            SignaturesSetWeb signaturesSetWeb = signTransaction.end(request);

            PrintWriter writer = response.getWriter();
            writer.println("Transacció acabada");
            writer.println(signaturesSetWeb.getStatusSignaturesSet().getStatus()
                    + " " + signaturesSetWeb.getStatusSignaturesSet().getErrorMsg());

            StatusSignature statusSignature = signaturesSetWeb.getFileInfoSignatureArray()[0].getStatusSignature();
            if (statusSignature.getSignedData() != null) {
                writer.println(statusSignature.getSignedData().getPath());
                writer.println(statusSignature.getSignedData().length());
            }


        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
