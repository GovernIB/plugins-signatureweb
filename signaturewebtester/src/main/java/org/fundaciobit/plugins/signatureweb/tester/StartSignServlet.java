package org.fundaciobit.plugins.signatureweb.tester;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.fundaciobit.plugins.signature.api.CommonInfoSignature;
import org.fundaciobit.plugins.signature.api.FileInfoSignature;
import org.fundaciobit.plugins.signature.api.ITimeStampGenerator;
import org.fundaciobit.plugins.signatureweb.api.ISignatureWebPlugin;
import org.fundaciobit.plugins.signatureweb.api.SignaturesSetWeb;
import org.fundaciobit.pluginsib.core.utils.FileUtils;

@WebServlet("/startSign")
@MultipartConfig
public class StartSignServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private PluginMapBean pluginMapBean;

    @Inject
    private ITimeStampGenerator timeStampGenerator;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Part fitxer = request.getPart("fitxer");
        if (fitxer == null || fitxer.getSize() == 0) {
            response.sendError(400, "No s'ha rebut el paràmetre fitxer");
            return;
        }

        String pluginName = request.getParameter("pluginName");
        if (pluginName == null || !pluginMapBean.containsPlugin(pluginName)) {
            response.sendError(400, "Nom de plugin invàlid: " + pluginName);
            return;
        }

        String nif = request.getParameter("nif");
        if (nif == null || nif.isEmpty()) {
            response.sendError(400, "NIF invàlid: " + nif);
            return;
        }

        Path tempFile = Files.createTempFile("sign", "temp");
        try (OutputStream os = new FileOutputStream(tempFile.toFile());
             InputStream is = fitxer.getInputStream()) {
            FileUtils.copy(is, os);
        }

        boolean timestamp = Boolean.parseBoolean(request.getParameter("timestamp"));

        FileInfoSignature fileInfoSignature = getFileInfoSignature(tempFile,
                fitxer.getSubmittedFileName(),
                fitxer.getContentType(),
                timestamp);

        String signaturesSetID = UUID.randomUUID().toString();
        String urlFinal = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() +
                request.getContextPath() + "/signResult?ssid=" + signaturesSetID;

        CommonInfoSignature commonInfoSignature = new CommonInfoSignature("ca", "", null, nif);

        SignaturesSetWeb signaturesSetWeb = new SignaturesSetWeb(
                signaturesSetID,
                Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)),
                commonInfoSignature,
                new FileInfoSignature[]{fileInfoSignature},
                urlFinal);

        try {
            ISignatureWebPlugin plugin = pluginMapBean.getPlugin(pluginName);

            String filterError = plugin.filter(request, signaturesSetWeb, Collections.emptyMap());
            if (filterError != null) {
                log("Filter error: " + filterError);
                response.sendError(500, filterError);
                return;
            }

            getServletContext().setAttribute(signaturesSetID, plugin);
            RequestTransactionInfo info = new RequestTransactionInfo(signaturesSetID, -1, "/plugin", request);
            String returnUrl = plugin.signDocuments(request,
                    info.getBaseAbsoluta(), info.getBaseRelativa(),
                    signaturesSetWeb, Collections.emptyMap());
            response.sendRedirect(returnUrl);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private FileInfoSignature getFileInfoSignature(Path tempFile, String fileName, String contentType, boolean timestamp) {
        return new FileInfoSignature("1", tempFile.toFile(), null,
                contentType, fileName, "Petició", null, null, 1, "ca",
                FileInfoSignature.SIGN_OPERATION_SIGN, signType(contentType), FileInfoSignature.SIGN_ALGORITHM_SHA256,
                FileInfoSignature.SIGN_MODE_ATTACHED_ENVELOPED, FileInfoSignature.SIGNATURESTABLELOCATION_WITHOUT, null, null,
                null, false, timestamp ? timeStampGenerator : null, null,
                null, null, null, null, null);
    }

    private String signType(String contentType) {
        switch (contentType) {
            case "application/pdf":
                return FileInfoSignature.SIGN_TYPE_PADES;
            case "application/xml":
            case "text/xml":
                return FileInfoSignature.SIGN_TYPE_XADES;
            default:
                return FileInfoSignature.SIGN_TYPE_CADES;
        }
    }
}
