package org.fundaciobit.pluginsib.signatureweb.tester;

import org.fundaciobit.plugins.signature.api.FileInfoSignature;
import org.fundaciobit.plugins.signature.api.StatusSignature;
import org.fundaciobit.plugins.signature.api.StatusSignaturesSet;
import org.fundaciobit.pluginsib.core.utils.FileUtils;
import org.fundaciobit.pluginsib.signatureweb.api.ISignatureWebPlugin;
import org.fundaciobit.pluginsib.signatureweb.api.SignaturesSetWeb;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@WebServlet("/endSign")
public class EndSignServlet extends HttpServlet {

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
        getServletContext().removeAttribute(ssid);

        try {
            SignaturesSetWeb signaturesSetWeb = plugin.getSignaturesSet(ssid);
            plugin.closeSignaturesSet(request, ssid);

            checkStatus(signaturesSetWeb.getStatusSignaturesSet());

            FileInfoSignature fileInfoSignature = signaturesSetWeb.getFileInfoSignatureArray()[0];
            StatusSignature statusSignature = fileInfoSignature.getStatusSignature();
            checkStatus(statusSignature);

            File signedData = statusSignature.getSignedData();
            response.setContentLengthLong(signedData.length());
            response.setContentType(signResultContentType(fileInfoSignature.getMimeType()));
            String fileName = "result" + System.currentTimeMillis() + signResultExtension(fileInfoSignature.getMimeType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            try (InputStream inputStream = new FileInputStream(signedData)) {
                FileUtils.copy(inputStream, response.getOutputStream());
            } finally {
                if (!signedData.delete()) {
                    signedData.deleteOnExit();
                }
            }

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void checkStatus(StatusSignaturesSet statusSignaturesSet) throws ServletException {
        if (statusSignaturesSet.getStatus() != StatusSignaturesSet.STATUS_FINAL_OK) {
            throw new ServletException(statusSignaturesSet.getErrorMsg());
        }
    }

    private String signResultContentType(String contentType) {
        switch (contentType) {
            case "application/pdf":
            case "application/xml":
            case "text/xml":
                return contentType;
            default:
                return "application/octet-stream";
        }
    }

    private String signResultExtension(String contentType) {
        switch (contentType) {
            case "application/pdf":
                return ".pdf";
            case "application/xml":
            case "text/xml":
                return ".xsig";
            default:
                return ".csig";
        }
    }

}
