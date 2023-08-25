package org.fundaciobit.pluginsib.signatureweb.firmanocriptografica;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.fundaciobit.plugins.signature.api.CommonInfoSignature;
import org.fundaciobit.plugins.signature.api.FileInfoSignature;
import org.fundaciobit.plugins.signature.api.StatusSignature;
import org.fundaciobit.plugins.signature.api.StatusSignaturesSet;

import org.fundaciobit.plugins.signatureweb.api.AbstractSignatureWebPlugin;

import org.fundaciobit.plugins.signatureweb.api.SignaturesSetWeb;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.module.SimpleModule;

import es.caib.evidenciesib.apiexterna.client.api.EvidenciesApi;
import es.caib.evidenciesib.apiexterna.client.model.EvidenciaFile;
import es.caib.evidenciesib.apiexterna.client.model.EvidenciaFileBase64;
import es.caib.evidenciesib.apiexterna.client.model.EvidenciaStartRequest;
import es.caib.evidenciesib.apiexterna.client.model.EvidenciaStartResponse;
import es.caib.evidenciesib.apiexterna.client.model.EvidenciaWs;
import es.caib.evidenciesib.apiexterna.client.services.ApiClient;
import es.caib.evidenciesib.apiexterna.client.services.ApiException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author anadal
 *
 */
public class FirmaNoCriptograficaSignatureWebPlugin extends AbstractSignatureWebPlugin {

    public static final String FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES = PLUGINSIB_SIGNATUREWEB_BASE_PROPERTY
            + "firmanocriptografica.";

    private static final Map<String, StatusFirmaNoCriptografica> statusBySignatureSetID = new HashMap<String, StatusFirmaNoCriptografica>();

    private static final Map<String, Map<String, Object>> parametersBySignatureSetID = new HashMap<String, Map<String, Object>>();

    static {
    }

    /**
     * 
     */
    public FirmaNoCriptograficaSignatureWebPlugin() {
        super();
    }

    /**
     * @param propertyKeyBase
     * @param properties
     */
    public FirmaNoCriptograficaSignatureWebPlugin(String propertyKeyBase, Properties properties) {
        super(propertyKeyBase, properties);
    }

    /**
     * @param propertyKeyBase
     */
    public FirmaNoCriptograficaSignatureWebPlugin(String propertyKeyBase) {
        super(propertyKeyBase);
    }

    @Override
    public String getName(Locale locale) {
        return getTraduccio("pluginname", locale);
    }

    @Override
    public String filter(HttpServletRequest request, SignaturesSetWeb signaturesSet, Map<String, Object> parameters) {

        // CRIDADA A  getApi().version() per comprovar comunicació.
        try {
            getApi().versio();
        } catch (Throwable e) {
            String msg = "Error desconegut intentant connectar amb el servidor d'EvidènciesIB: " + e.getMessage();
            log.error(msg, e);
            return msg;
        }

        if (signaturesSet.getFileInfoSignatureArray().length != 1) {
            // XYZ ZZZ 
            return "Només es pot fer una signatura a la vegada.";
        }

        String f = super.filter(request, signaturesSet, parameters);

        return f;

    }

    @Override
    public String signDocuments(HttpServletRequest request, String absolutePluginRequestPath,
            String relativePluginRequestPath, SignaturesSetWeb signaturesSet, Map<String, Object> parameters)
            throws Exception {

        addSignaturesSet(signaturesSet);
        final String signatureSetID = signaturesSet.getSignaturesSetID();

        log.info("signDocuments()::Parameters -> " + parameters.size());

        parametersBySignatureSetID.put(signatureSetID, parameters);

        final String returnUrl = absolutePluginRequestPath + "/" + CALLBACK_PAGE + "/{0}";

        final CommonInfoSignature common = signaturesSet.getCommonInfoSignature();

        final String language = common.getLanguageUI();

        EvidenciaStartRequest start = new EvidenciaStartRequest();

        start.setCallBackUrl(returnUrl);

        FileInfoSignature fis = signaturesSet.getFileInfoSignatureArray()[0];

        File f = fis.getFileToSign();

        byte[] docToSign = FileUtils.readFileToByteArray(f);

        EvidenciaFile ef = new EvidenciaFile();
        ef.setDescription(null);
        ef.setDocument(docToSign);
        ef.setEncryptedFileID(null);
        ef.setMime(fis.getMimeType());
        ef.setName(f.getName());
        ef.setSize((long) docToSign.length);

        start.setDocumentASignar(ef);
        start.setLanguageDocument(fis.getLanguageSign());
        start.setLanguageUI(language);
        start.setPersonaNif(common.getAdministrationID());

        // XYZ ZZZ ZZZ TODO
        start.setPersonaLlinatge1("Gonella");
        // XYZ ZZZ ZZZ TODO
        start.setPersonaLlinatge2("Rondalles");
        // XYZ ZZZ ZZZ TODO
        start.setPersonaNom("Pep");
        // XYZ ZZZ ZZZ TODO
        start.setPersonaEmail("pepgonella@fundaciobit.org");

        start.setRaoDeLaFirma(fis.getReason());
        start.setTitolEvidencia("PluginFirmaNoCripto_" + signatureSetID);

        EvidenciaStartResponse response = getApi().start(start);

        statusBySignatureSetID.put(signatureSetID, new StatusFirmaNoCriptografica(response.getEvidenciaID()));

        // XYZ ZZZ Falta TRY-CATCH !!!!

        // Mostrar pagina principal
        return response.getEvidenciaUrlRedirect().toString();
        //relativePluginRequestPath + "/" + PAGINA_PRINCIPAL_PAGE;

    }

    @Override
    public void closeSignaturesSet(HttpServletRequest request, String id) {

        super.closeSignaturesSet(request, id);

        statusBySignatureSetID.remove(id);

        parametersBySignatureSetID.remove(id);
    }

    @Override
    public void requestGET(String absolutePluginRequestPath, String relativePluginRequestPath, String relativePath,
            SignaturesSetWeb signaturesSet, int signatureIndex, HttpServletRequest request,
            HttpServletResponse response, Locale locale) {

        //final SignIDAndIndex sai = new SignIDAndIndex(signaturesSet, signatureIndex);
        //final String lang = locale.getLanguage();
        /*
        if (relativePath.startsWith(PAGINA_PRINCIPAL_PAGE)) {
        
            PrintWriter out = generateHeader(request, response, absolutePluginRequestPath,
                    relativePluginRequestPath, lang, sai, signaturesSet);
            paginaPrincipalGET(request, relativePluginRequestPath, relativePath, signaturesSet, out,
                    locale);
        
            generateFooter(out, sai, signaturesSet);
        }  else */ if (relativePath.startsWith(CALLBACK_PAGE)) {

            callBackDesDeEvidenciesIB(absolutePluginRequestPath, relativePluginRequestPath, request, response,
                    signaturesSet, locale);

        } else {
            super.requestGET(absolutePluginRequestPath, relativePluginRequestPath, relativePath, signaturesSet,
                    signatureIndex, request, response, locale);
        }

    }

    @Override
    public void requestPOST(String absolutePluginRequestPath, String relativePluginRequestPath, String relativePath,
            SignaturesSetWeb signaturesSet, int signatureIndex, HttpServletRequest request,
            HttpServletResponse response, Locale locale) {

        //        final SignIDAndIndex sai = new SignIDAndIndex(signaturesSet, signatureIndex);
        //        final String lang = locale.getLanguage();

        if (relativePath.startsWith(CALLBACK_PAGE)) {

            callBackDesDeEvidenciesIB(absolutePluginRequestPath, relativePluginRequestPath, request, response,
                    signaturesSet, locale);

        } else {

            super.requestPOST(absolutePluginRequestPath, relativePluginRequestPath, relativePath, signaturesSet,
                    signatureIndex, request, response, locale);

        }

    }

    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------
    // ------------------------------------ CALLBACK DES DE EVIDENCIES -------------------------------
    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------

    private static final String CALLBACK_PAGE = "callback";

    private void callBackDesDeEvidenciesIB(String absolutePluginRequestPath, String relativePluginRequestPath,
            HttpServletRequest request, HttpServletResponse response, SignaturesSetWeb signaturesSet, Locale locale) {

        final String signaturesSetID = signaturesSet.getSignaturesSetID();

        try {

            String language = signaturesSet.getCommonInfoSignature().getLanguageUI();

            Long evidenciaID = statusBySignatureSetID.get(signaturesSetID).getEvidenciaID();

            EvidenciesApi api = getApi();

            EvidenciaWs evi = api.get(evidenciaID, language);

            log.info(" CALL BACK => " + evi);

            log.info(" CALL BACK ESTAT => " + evi.getEstatCodiDescripcio());

            // XYZ ZZZ TODO CANVIAR PER CONSTANT
            if (evi.getEstatCodi() == 10) {

                EvidenciaFile efile = evi.getFitxerSignat();

                String encryptedFile = efile.getEncryptedFileID();

                EvidenciaFileBase64 file = api.getfilebase64(evidenciaID, encryptedFile, language);

                File f = File.createTempFile("PLUGIN_FIRMA_NO_CRIPTO_" + evidenciaID + "_", "_" + file.getName());

                FileOutputStream fos = new FileOutputStream(f);
                fos.write(Base64.decodeBase64(file.getDocumentBase64()));
                fos.flush();
                fos.close();

                FileInfoSignature[] fileInfoSignatureArray = signaturesSet.getFileInfoSignatureArray();

                FileInfoSignature fileInfoSignature = fileInfoSignatureArray[0];

                fileInfoSignature.getStatusSignature().setSignedData(f);

                fileInfoSignature.getStatusSignature().setStatus(StatusSignature.STATUS_FINAL_OK);

                signaturesSet.getStatusSignaturesSet().setStatus(StatusSignaturesSet.STATUS_FINAL_OK);

            } else {
                // Algun error
                StatusSignaturesSet ss = signaturesSet.getStatusSignaturesSet();
                ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);
                ss.setErrorMsg(evi.getEstatCodiDescripcio() + ": " + evi.getEstatError());
            }

            signaturesSet.getStatusSignaturesSet().setStatus(StatusSignaturesSet.STATUS_FINAL_OK);

        } catch (Throwable th) {
            // TODO Mirar certs tipus d'excepció

            log.error("Error Firmant: " + th.getMessage() + " [CLASS: " + th.getClass().getName() + "]", th);

            StatusSignaturesSet ss = signaturesSet.getStatusSignaturesSet();

            ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);

            ss.setErrorException(th);

            String msg;
            Throwable cause = th.getCause();
            if (cause != null && th instanceof java.lang.reflect.InvocationTargetException) {
                msg = cause.getMessage();
            } else {
                msg = th.getMessage();
            }

            ss.setErrorMsg(getTraduccio("error.firmantdocument", locale) + ":" + msg);
        }

        final String url;
        url = signaturesSet.getUrlFinal();

        sendRedirect(response, url);

    }

    protected File guardaFitxer(EvidenciesApi api, final String language, Long evidenciaID, EvidenciaFile efile,
            String fileType) throws ApiException, FileNotFoundException, IOException {

        if (efile == null) {

            return null;
        }

        String encryptedFile = efile.getEncryptedFileID();

        EvidenciaFile file = api.getfile(evidenciaID, encryptedFile, language);

        File f = new File("EVI_" + evidenciaID + "_" + fileType + "_" + file.getName());

        FileOutputStream fos = new FileOutputStream(f);
        fos.write(file.getDocument());
        fos.flush();
        fos.close();

        System.out.println("Gardat Fitxer " + fileType + " a " + f.getName());
        return f;
    }

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ------------------ P A G I N A - P R I N C I P A L -------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    /*
    private static final String PAGINA_PRINCIPAL_PAGE = "paginaprincipal";
    
    private void paginaPrincipalGET(HttpServletRequest request, String relativePluginRequestPath,
            String query, SignaturesSetWeb signaturesSet, PrintWriter out, Locale locale) {
    
        // TODO XXX XYZ
        out.println("<h3>Necessitam confirmar que es vosté ...</h3>");
        // TODO XXX XYZ
        out.println(
                "<h5>Ens ha de proporcionar evidències de la seva persona fins a aconseguir 100 punts.<br/>"
                        + "Serà en aquest moment en que podrà signar ..." + "</h5>");
        out.println("<table class=\"table table-striped\">");
        out.println("<thead>\r\n" + "    <tr>\r\n" + "      <th scope=\"col\">Evidència</th>\r\n"
                + "      <th scope=\"col\">Punts<br/>Disponibles</th>\r\n"
                + "      <th scope=\"col\">Punts<br/>Aconseguits</th>\r\n" + "    </tr>\r\n"
                + "  </thead>\r\n" + "  <tbody>");
    
        StatusFirmaNoCriptografica status = statusBySignatureSetID
                .get(signaturesSet.getSignaturesSetID());
    
        Map<String, Integer> mappunts = status.getEvidenciesPuntsMap();
    
        int totalpunts = 0;
    
        for (String codi : mappunts.keySet()) {
    
            IEvidencia e = EVIDENCESBYCODE.get(codi);
    
            out.println("<tr>");
            out.println("<td><b>" + e.getTitol() + "</b>"
                    + "<br/><small>" + e.getSubtitol() + "</small>" + "</td>"); // XYZ ZZZ
    
            out.println("<td>" + e.getPunts() + "</td>");
    
            out.println("<td>");
            Integer punts = mappunts.get(e.getCodi());
            if (punts == null) {
                String path = relativePluginRequestPath + "/evidencia" + codi;
                String label = "Aconseguir punts"; // getTraduccio("cancel", locale); XYZ ZZZ
                String htmlCode = generateButton(path, label, "btn-success");
                out.println(htmlCode);
    
            } else {
                out.println("<b>" + punts + "</b>");
                totalpunts = totalpunts + punts;
            }
    
            out.println("</td>");
    
            out.println("</tr>");
    
        }
    
        out.println("<tr>");
        out.println("<td colspan=\"2\"><i>Total Punts: </i></td>"); // XYZ ZZZ
        out.println("<td>" + totalpunts + "</td>");
        out.println("</tr>");
    
    
        out.println("</tbody>");
        out.println("</table>");
        out.println("<br />");
    
        out.println("&nbsp;&nbsp;");
    
        if (totalpunts >= 100) {
            String path = relativePluginRequestPath + "/" + FIRMAR_PAGE;
            String label = getTraduccio("firmadocument", locale);
            String htmlCode = generateButton(path, label, "btn-primary");
            out.println(htmlCode);
            out.println("&nbsp;&nbsp;");
        }
    
        {
            String path = relativePluginRequestPath + "/" + CANCEL_PAGE;
            String label = getTraduccio("cancel", locale);
            String htmlCode = generateButton(path, label, "btn-warning");
            out.println(htmlCode);
        }
    
    
    }
    
    */

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ---------------------- U T I L I T A T S H T M L -------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // TODO
    //    protected String generateButton(String path, String label, String tipus) {
    //        String htmlCode = "<button class=\"btn " + tipus + "\" type=\"button\"  onclick=\"location.href='" + path
    //                + "'\" >" + label + "</button>";
    //        return htmlCode;
    //    }

    @Override
    public String getResourceBundleName() {
        return "firmanocriptografica";
    }

    @Override
    protected String getSimpleName() {
        return "Firma No Criptogràfica";
    }

    @Override
    public int getActiveTransactions() throws Exception {
        return internalGetActiveTransactions();
    }

    @Override
    public boolean acceptExternalRubricGenerator() {
        try {
            return false;
        } catch (Exception e) {
            log.error("acceptExternalRubricGenerator: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean acceptExternalSecureVerificationCodeStamper() {
        try {
            return false;
        } catch (Exception e) {
            log.error("acceptExternalSecureVerificationCodeStamper: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean acceptExternalTimeStampGenerator(String signType) {
        try {
            return false;
        } catch (Exception e) {
            log.error("acceptExternalTimeStampGenerator: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> getSupportedBarCodeTypes() {
        try {
            return null;
        } catch (Exception e) {
            log.error("getSupportedBarCodeTypes: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String[] getSupportedSignatureAlgorithms(String signType) {
        try {
            return new String[] { FileInfoSignature.SIGN_ALGORITHM_SHA1, FileInfoSignature.SIGN_ALGORITHM_SHA256,
                    FileInfoSignature.SIGN_ALGORITHM_SHA384, FileInfoSignature.SIGN_ALGORITHM_SHA512 };
        } catch (Exception e) {
            log.error("getSupportedSignatureAlgorithms: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String[] getSupportedSignatureTypes() {
        try {
            return new String[] { FileInfoSignature.SIGN_TYPE_PADES };
        } catch (Exception e) {
            log.error("getSupportedSignatureTypes: " + e.getMessage(), e);
            return null;
        }

    }

    @Override
    public boolean providesRubricGenerator() {
        try {
            return false;
        } catch (Exception e) {
            log.error("providesRubricGenerator: " + e.getMessage(), e);
            return false;
        }

    }

    @Override
    public boolean providesSecureVerificationCodeStamper() {
        try {
            return false;
        } catch (Exception e) {
            log.error("providesSecureVerificationCodeStamper: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean providesTimeStampGenerator(String signType) {
        try {
            return false;
        } catch (Exception e) {
            log.error("providesTimeStampGenerator: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void resetAndClean(HttpServletRequest request) throws Exception {

        try {

            statusBySignatureSetID.clear();

            parametersBySignatureSetID.clear();

        } catch (Exception e) {
            log.error("resetAndClean: " + e.getMessage(), e);
        }
    }



    protected EvidenciesApi getApi() throws Exception {

        ApiClient apiclient = new ApiClient();

        SimpleModule modul = new SimpleModule();
        
        System.out.println("\n\nFIRMA NO CRIPTO REGISTANT MyByteArraySerializer !!!!\n\n");
        modul.addDeserializer(byte[].class, new MyByteArraySerializer());
        apiclient.getJSON().getContext(null).registerModule(modul);
        apiclient.getJSON().getContext(byte[].class).registerModule(modul);

        apiclient.setBasePath(getPropertyRequired(FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.host"));
        apiclient.setUsername(getPropertyRequired(FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.username"));
        apiclient.setPassword(getPropertyRequired(FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.password"));

        apiclient.setDebugging(true);

        EvidenciesApi api = new EvidenciesApi(apiclient);

        return api;
    }

    public static class MyByteArraySerializer extends com.fasterxml.jackson.databind.JsonDeserializer<byte[]> {
        @Override
        public byte[] deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

            System.out.println("\n\nFIRMA NO CRIPTO MyByteArraySerializer ==> PASSA !!!!\n\n");

            String str = p.getText();

            return Base64.decodeBase64(str);

        }
    }

}
