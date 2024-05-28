package org.fundaciobit.pluginsib.signatureweb.firmanocriptografica;

import org.apache.commons.io.FileUtils;
import org.fundaciobit.plugins.signature.api.CommonInfoSignature;
import org.fundaciobit.plugins.signature.api.FileInfoSignature;
import org.fundaciobit.plugins.signature.api.PropertyInfo;
import org.fundaciobit.plugins.signature.api.StatusSignature;
import org.fundaciobit.plugins.signature.api.StatusSignaturesSet;
import org.fundaciobit.pluginsib.signatureweb.api.AbstractSignatureWebPlugin;
import org.fundaciobit.pluginsib.signatureweb.api.SignaturesSetWeb;

import es.caib.evidenciesib.api.externa.client.evidencies.v1.api.EvidenciesApi;
import es.caib.evidenciesib.api.externa.client.evidencies.v1.model.ConstantsWs;
import es.caib.evidenciesib.api.externa.client.evidencies.v1.model.EvidenciaFile;
import es.caib.evidenciesib.api.externa.client.evidencies.v1.model.EvidenciaFileBase64;
import es.caib.evidenciesib.api.externa.client.evidencies.v1.model.EvidenciaStartRequest;
import es.caib.evidenciesib.api.externa.client.evidencies.v1.model.EvidenciaStartResponse;
import es.caib.evidenciesib.api.externa.client.evidencies.v1.model.EvidenciaWs;
import es.caib.evidenciesib.api.externa.client.evidencies.v1.services.ApiClient;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
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

    public static final ConstantsWs C = new ConstantsWs();

    public static final String FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES = PLUGINSIB_SIGNATUREWEB_BASE_PROPERTY
            + "firmanocriptografica.";

    private static final Map<String, StatusFirmaNoCriptografica> statusBySignatureSetID = new HashMap<String, StatusFirmaNoCriptografica>();

    private static final Map<String, Map<String, Object>> parametersBySignatureSetID = new HashMap<String, Map<String, Object>>();

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

        final CommonInfoSignature common = signaturesSet.getCommonInfoSignature();

        try {
            addSignaturesSet(signaturesSet);
            final String signatureSetID = signaturesSet.getSignaturesSetID();

            log.info("signDocuments()::Parameters -> " + parameters.size());

            parametersBySignatureSetID.put(signatureSetID, parameters);

            final String returnUrl = absolutePluginRequestPath + "/" + CALLBACK_PAGE + "/{0}";

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
            start.setPersonaUsername(common.getUsername());

            start.setPersonaLlinatge1(null);
            start.setPersonaLlinatge2(null);
            start.setPersonaNom(null);
            start.setPersonaEmail(null);
            start.setPersonaMobil(null);

            start.setRaoDeLaFirma(fis.getReason());
            start.setTitolEvidencia("PluginFirmaNoCripto_" + signatureSetID);

            EvidenciaStartResponse response = getApi().start(start);

            statusBySignatureSetID.put(signatureSetID, new StatusFirmaNoCriptografica(response.getEvidenciaID(),
                    response.getEvidenciaUrlRedirect().toString()));

            // Mostrar pagina principal
            return relativePluginRequestPath + "/" + PAGINA_PRINCIPAL_PAGE;

        } catch (Throwable th) {

            log.error("Error Establint inici de comunicació amb EvidènciesIB: " + th.getMessage() + " [CLASS: "
                    + th.getClass().getName() + "]", th);

            StatusSignaturesSet ss = signaturesSet.getStatusSignaturesSet();

            ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);

            ss.setErrorException(th);

            String msg;
            Throwable cause = th.getCause();
            if (cause == null) {
                msg = th.getMessage();
            } else {
                if (th instanceof java.lang.reflect.InvocationTargetException) {
                    msg = cause.getMessage();
                } else {
                    msg = th.getMessage() + "(" + cause.getMessage() + ")";
                }
            }

            ss.setErrorMsg(getTraduccio("error.firmantdocument", new Locale(common.getLanguageUI()), msg));

            final String url = signaturesSet.getUrlFinal();

            return url;
        }

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

        final SignIDAndIndex sai = new SignIDAndIndex(signaturesSet, signatureIndex);

        if (relativePath.startsWith(PAGINA_PRINCIPAL_PAGE)) {

            paginaPrincipalGET(absolutePluginRequestPath, relativePluginRequestPath, request, response, relativePath,
                    signaturesSet, locale, sai);

        } else if (relativePath.startsWith(CALLBACK_PAGE)) {

            callBackDesDeEvidenciesIB(absolutePluginRequestPath, relativePluginRequestPath, request, response,
                    signaturesSet, locale, sai);

        } else {
            super.requestGET(absolutePluginRequestPath, relativePluginRequestPath, relativePath, signaturesSet,
                    signatureIndex, request, response, locale);
        }

    }

    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------
    // ---------------------- CALLBACK DES DE EVIDENCIES -------------------------
    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------

    private static final String CALLBACK_PAGE = "callback";

    private void callBackDesDeEvidenciesIB(String absolutePluginRequestPath, String relativePluginRequestPath,
            HttpServletRequest request, HttpServletResponse response, SignaturesSetWeb signaturesSet, Locale locale,
            final SignIDAndIndex sai) {

        final String signaturesSetID = signaturesSet.getSignaturesSetID();

        try {

            String language = signaturesSet.getCommonInfoSignature().getLanguageUI();

            Long evidenciaID = statusBySignatureSetID.get(signaturesSetID).getEvidenciaID();

            EvidenciesApi api = getApi();

            EvidenciaWs evi = api.get(evidenciaID, language);

            log.info(" CALL BACK => " + evi);

            log.info(" CALL BACK ESTAT => " + evi.getEstatCodiDescripcio());

            if (evi.getEstatCodi() == C.getEVIDENCIAESTATCODISIGNAT()) {

                EvidenciaFile efile = evi.getFitxerSignat();

                String encryptedFile = efile.getEncryptedFileID();

                EvidenciaFileBase64 file = api.getfilebase64(evidenciaID, encryptedFile, language);

                File f = File.createTempFile("PLUGIN_FIRMA_NO_CRIPTO_" + evidenciaID + "_", "_" + file.getName());

                FileOutputStream fos = new FileOutputStream(f);
                fos.write(Base64.getDecoder().decode(file.getDocumentBase64()));
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

            ss.setErrorMsg(getTraduccio("error.firmantdocument", locale, msg));

        }

        // Estam en la finestra nova 

        // (1) Hem de carregar la pagina de final al iframe
        // (2) Hem de tancar aquesta finestra

        PrintWriter out = generateHeader(request, response, absolutePluginRequestPath, relativePluginRequestPath,
                locale.getLanguage(), sai, signaturesSet);

        final String url;
        url = signaturesSet.getUrlFinal();

        out.println("<script type=\"text/javascript\">" + "\n");
        out.println("    window.opener.location.href='" + url + "';\n");
        out.println("    setTimeout(() => { window.close(); }, 1000);" + "\n");
        out.println("</script>" + "\n");
        out.println("<center>" + "\n");
        out.println("<img onClick='window.close();' src=\"" + relativePluginRequestPath + "/" + WEBRESOURCE
                + "/img/ajax-loader2.gif\" />" + "\n");
        out.println("</center>\n");

        generateFooter(out, sai, signaturesSet);

    }

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ------------------ P A G I N A - P R I N C I P A L -------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    private static final String PAGINA_PRINCIPAL_PAGE = "paginaprincipal";

    private void paginaPrincipalGET(String absolutePluginRequestPath, String relativePluginRequestPath,
            HttpServletRequest request, HttpServletResponse response, String query, SignaturesSetWeb signaturesSet,
            Locale locale, final SignIDAndIndex sai) {

        final String signaturesSetID = signaturesSet.getSignaturesSetID();

        StatusFirmaNoCriptografica sfnc = statusBySignatureSetID.get(signaturesSetID);

        PrintWriter out = generateHeader(request, response, absolutePluginRequestPath, relativePluginRequestPath,
                locale.getLanguage(), sai, signaturesSet);
        
        
        final String cancelURL = relativePluginRequestPath + "/" + CANCEL_PAGE;
        

        out.println("<script type=\"text/javascript\">" + "\n");
        out.println("    let windowObjectReference = null;");
        out.println("\n");
        out.println("    reintentar();");
        out.println("\n");
        out.println("    function reintentar() {");
        out.println("      windowObjectReference = window.open('" + sfnc.getUrlEvidencies() + "', '_blank');");
        out.println("    }");
        out.println("\n");
        out.println("    function cancelEvidencia() {");
        out.println("        if (windowObjectReference === null || windowObjectReference.closed) {");
        out.println("           // No fer res;");
        out.println("        } else {");
        out.println("           windowObjectReference.close();");
        out.println("        }");
        out.println("        document.location.href = '" + cancelURL + "';");
        out.println("    }");
        out.println("\n");
        out.println("</script>" + "\n");
        out.println("<center>" + "\n");
        out.println("<h4> " + getTraduccio("esperar", locale) + " </h4><br/>" + "\n");
        out.println("<img src=\"" + relativePluginRequestPath + "/" + WEBRESOURCE + "/img/ajax-loader2.gif\" />" + "\n");
        out.println("<br/><br/><input id=\"cancel\" name=\"cancel\" class=\"btn btn-warning btn-large\"\r\n"
                + "                             onclick=\"cancelEvidencia();\"\r\n"
                + "                             value=\"" + getTraduccio("cancel", locale) + "\" />");
        
        out.println("<br/><br/><h4> " + getTraduccio("pipellabloquejada", locale) + " </h4>" + "\n");
        out.println("<br/><button class=\"btn btn-succes btn-large\" onclick=\"reintentar();\">");
        out.println("       &#8634; " + getTraduccio("reintentar", locale) + "</button>");
        out.println("</center>");

        out.flush();

        generateFooter(out, sai, signaturesSet);

    }

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

    /**
     * Indicam que no volem que es validi el NIF de l'usuari que ha de signar amb el NIF que apareix a la firma digital.
     */
    @Override
    public boolean administrationIdCanBeValidated() {
        return false;
    }

    @Override
    public List<PropertyInfo> getAvailableProperties(String propertyKeyBase) {

        if (propertyKeyBase == null) {
            propertyKeyBase = super.getPropertyKeyBase();
        }

        List<PropertyInfo> list = new ArrayList<PropertyInfo>();

        {
            PropertyInfo pi = new PropertyInfo();
            pi.setKey(propertyKeyBase + FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.host");
            pi.setExamples(new String[] {});
            list.add(pi);
        }
        {
            PropertyInfo pi = new PropertyInfo();
            pi.setKey(propertyKeyBase + FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.host");
            pi.setExamples(new String[] {});
            list.add(pi);
        }
        {
            PropertyInfo pi = new PropertyInfo();
            pi.setKey(propertyKeyBase + FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.username");
            pi.setExamples(new String[] {});
            list.add(pi);
        }
        {
            PropertyInfo pi = new PropertyInfo();
            pi.setKey(propertyKeyBase + FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.password");
            pi.setExamples(new String[] { "true", "false" });
            list.add(pi);
        }

        return list;

    }

    private EvidenciesApi getApi() throws Exception {

        ApiClient apiclient = new ApiClient();

        apiclient.setBasePath(getPropertyRequired(FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.host"));
        apiclient.setUsername(getPropertyRequired(FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.username"));
        apiclient.setPassword(getPropertyRequired(FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.password"));

        apiclient.setDebugging("true".equals(getProperty(FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES + "evidencies.debug")));

        EvidenciesApi api = new EvidenciesApi(apiclient);

        return api;
    }

}
