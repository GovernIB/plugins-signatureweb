package org.fundaciobit.plugins.signatureweb.firmanocriptografica;

import org.fundaciobit.plugins.signature.api.StatusSignature;
import org.fundaciobit.plugins.signature.api.StatusSignaturesSet;
import org.fundaciobit.plugins.signatureserver.api.ISignatureServerPlugin;

import org.fundaciobit.plugins.signatureweb.api.AbstractSignatureWebPlugin;

import org.fundaciobit.plugins.signatureweb.api.SignaturesSetWeb;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.EvidenciaClaveAutenticacion;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.EvidenciaDniNomLlinatges;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.EvidenciaIP;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.EvidenciaIban;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.EvidenciaPadro;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.EvidenciaTelefonMobil;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.EvidenciaUsuariCAIB;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.EvidenciaXarxesSocials;
import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.IEvidencia;
import org.fundaciobit.pluginsib.core.utils.PluginsManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

    private static final String SERVER_PLUGIN_CLASS = FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES
            + "serverplugin.class";

    private static final String SERVER_PLUGIN_PROPERTIES = FIRMANOCRIPTOGRAFICA_BASE_PROPERTIES
            + "serverplugin.properties.";

    private ISignatureServerPlugin signatureServerPlugin = null;

    private static final Map<String, StatusFirmaNoCriptografica> statusBySignatureSetID = new HashMap<String, StatusFirmaNoCriptografica>();

    private static final Map<String, Map<String, Object>> parametersBySignatureSetID = new HashMap<String, Map<String, Object>>();

    private static final List<IEvidencia> EVIDENCESLIST = new ArrayList<IEvidencia>();

    private static final Map<String, IEvidencia> EVIDENCESBYCODE = new HashMap<String, IEvidencia>();

    static {

        EVIDENCESLIST.add(new EvidenciaClaveAutenticacion());
        EVIDENCESLIST.add(new EvidenciaDniNomLlinatges());
        EVIDENCESLIST.add(new EvidenciaIP());
        EVIDENCESLIST.add(new EvidenciaPadro());
        EVIDENCESLIST.add(new EvidenciaTelefonMobil());
        EVIDENCESLIST.add(new EvidenciaUsuariCAIB());
        EVIDENCESLIST.add(new EvidenciaXarxesSocials());
        EVIDENCESLIST.add(new EvidenciaIban());
        
        

        for (IEvidencia iEvidencia : EVIDENCESLIST) {
            EVIDENCESBYCODE.put(iEvidencia.getCodi(), iEvidencia);
        }

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

    private synchronized ISignatureServerPlugin getSignatureServerPlugin() throws Exception {

        if (signatureServerPlugin == null) {

            String pluginClass = getPropertyRequired(SERVER_PLUGIN_CLASS);

            ISignatureServerPlugin plugin = (ISignatureServerPlugin) PluginsManager
                    .instancePluginByClassName(pluginClass, SERVER_PLUGIN_PROPERTIES,
                            this.getPluginProperties());

            this.signatureServerPlugin = plugin;

        }

        return this.signatureServerPlugin;

    };

    @Override
    public String getName(Locale locale) {
        return getTraduccio("pluginname", locale);
    }

    @Override
    public String filter(HttpServletRequest request, SignaturesSetWeb signaturesSet,
            Map<String, Object> parameters) {

        ISignatureServerPlugin plugin;

        try {

            plugin = getSignatureServerPlugin();

        } catch (Exception e) {
            String msg = "Error instanciant plugin de firma en Servidor: " + e.getMessage();
            log.error(msg, e);
            return msg;
        }

        String f = plugin.filter(signaturesSet, parameters);

        if (f == null) {
            f = super.filter(request, signaturesSet, parameters);
        }

        return f;

    }

    @Override
    public String signDocuments(HttpServletRequest request, String absolutePluginRequestPath,
            String relativePluginRequestPath, SignaturesSetWeb signaturesSet,
            Map<String, Object> parameters) throws Exception {

        addSignaturesSet(signaturesSet);
        final String signatureSetID = signaturesSet.getSignaturesSetID();

        statusBySignatureSetID.put(signatureSetID, new StatusFirmaNoCriptografica(EVIDENCESLIST));

        parametersBySignatureSetID.put(signatureSetID, parameters);

        // CommonInfoSignature commonInfoSignature =
        // signaturesSet.getCommonInfoSignature();

        // String username = commonInfoSignature.getUsername();

        // Mostrar pagina principal
        return relativePluginRequestPath + "/" + PAGINA_PRINCIPAL_PAGE;

    }

    @Override
    public void closeSignaturesSet(HttpServletRequest request, String id) {

        super.closeSignaturesSet(request, id);

        statusBySignatureSetID.remove(id);

        parametersBySignatureSetID.remove(id);
    }

    @Override
    public void requestGET(String absolutePluginRequestPath, String relativePluginRequestPath,
            String relativePath, SignaturesSetWeb signaturesSet, int signatureIndex,
            HttpServletRequest request, HttpServletResponse response, Locale locale) {

        final SignIDAndIndex sai = new SignIDAndIndex(signaturesSet, signatureIndex);
        final String lang = locale.getLanguage();

        if (relativePath.startsWith(PAGINA_PRINCIPAL_PAGE)) {

            PrintWriter out = generateHeader(request, response, absolutePluginRequestPath,
                    relativePluginRequestPath, lang, sai, signaturesSet);
            paginaPrincipalGET(request, relativePluginRequestPath, relativePath, signaturesSet, out,
                    locale);

            generateFooter(out, sai, signaturesSet);
        } else if (relativePath.startsWith(EVIDENCIA_DADES_DNI_PAGE)) {

            PrintWriter out = generateHeader(request, response, absolutePluginRequestPath,
                    relativePluginRequestPath, lang, sai, signaturesSet);
            evidenciaDadesDniPage(request, response, relativePluginRequestPath, relativePath,
                    signaturesSet, out, locale, true);
            generateFooter(out, sai, signaturesSet);

        } else if (relativePath.startsWith(EVIDENCIA_TELEFON_MOBIL)) {

            PrintWriter out = generateHeader(request, response, absolutePluginRequestPath,
                    relativePluginRequestPath, lang, sai, signaturesSet);
            evidenciaTelefonMobil(request, response, relativePluginRequestPath, relativePath,
                    signaturesSet, out, locale, true);
            generateFooter(out, sai, signaturesSet);

        } else if (relativePath.startsWith(FIRMAR_PAGE)) {

            firmarPOST(absolutePluginRequestPath, relativePluginRequestPath, request, response,
                    signaturesSet, locale);

        } else {
            super.requestGET(absolutePluginRequestPath, relativePluginRequestPath, relativePath,
                    signaturesSet, signatureIndex, request, response, locale);
        }

    }

    @Override
    public void requestPOST(String absolutePluginRequestPath, String relativePluginRequestPath,
            String relativePath, SignaturesSetWeb signaturesSet, int signatureIndex,
            HttpServletRequest request, HttpServletResponse response, Locale locale) {

        final SignIDAndIndex sai = new SignIDAndIndex(signaturesSet, signatureIndex);
        final String lang = locale.getLanguage();

        if (relativePath.startsWith(FIRMAR_PAGE)) {

            firmarPOST(absolutePluginRequestPath, relativePluginRequestPath, request, response,
                    signaturesSet, locale);

        } else if (relativePath.startsWith(EVIDENCIA_DADES_DNI_PAGE)) {

            PrintWriter out = generateHeader(request, response, absolutePluginRequestPath,
                    relativePluginRequestPath, lang, sai, signaturesSet);
            evidenciaDadesDniPage(request, response, relativePluginRequestPath, relativePath,
                    signaturesSet, out, locale, false);
            generateFooter(out, sai, signaturesSet);

        } else if (relativePath.startsWith(EVIDENCIA_TELEFON_MOBIL)) {

            PrintWriter out = generateHeader(request, response, absolutePluginRequestPath,
                    relativePluginRequestPath, lang, sai, signaturesSet);
            evidenciaTelefonMobil(request, response, relativePluginRequestPath, relativePath,
                    signaturesSet, out, locale, false);
            generateFooter(out, sai, signaturesSet);

        } else {

            super.requestPOST(absolutePluginRequestPath, relativePluginRequestPath, relativePath,
                    signaturesSet, signatureIndex, request, response, locale);

        }

    }

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ------------------ P A G I N A - P R I N C I P A L -------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

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
            out.println("<td><b>" + e.getTitol() /* getTraduccio("fitxerp12", locale) */ + "</b>"
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

        /*
         * out.println("<tr>"); out.println("<td>" + getTraduccio("contrasenyap12",
         * locale)+ ":</td>"); out.
         * println("<td><input type=\"password\" style=\"display: none;\" /><input type=\"password\" name=\""
         * + SERVER_PLUGIN_CLASS + "\" value=\"\" /></td>"); out.println("</tr>");
         * out.println("<tr>"); out.println("<td>" + getTraduccio("pin", locale) +
         * "&nbsp; <span title=\"" + getTraduccio("pin.desc", locale) +
         * "\">[ &iopf; ]</span>:</td>");
         * out.println("<td><input type=\"password\"  name=\"" + FIELD_PIN +
         * "\" value=\"\" /></td>"); out.println("</tr>");
         */
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

        /**
         * if (query.endsWith("canreturn")) { String redirect = pluginRequestPath + "/"
         * + SELECT_CERTIFICATE_PAGE; out.println("&nbsp;<button type=\"button\"
         * class=\"btn\" onclick=\"location.href='" + redirect + "'\">");
         * out.println(getTraduccio("tornar", locale)); out.println("</button>"); }
         * 
         * 
         * out.println("</form>");
         */
    }

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ------------------ EVIDENCIA DNI-NON-LLINATGE-DATA_CADUCITAT ---------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    private static final String EVIDENCIA_DADES_DNI_PAGE = "evidencia"
            + EvidenciaDniNomLlinatges.CODI;

    private void evidenciaDadesDniPage(HttpServletRequest request, HttpServletResponse response,
            String relativePluginRequestPath, String query, SignaturesSetWeb signaturesSet,
            PrintWriter out, Locale locale, boolean isGet) {

        if (isGet) {
            // TODO XXX XYZ
            out.println("<h3>Evidencies utilitzant dades del DNI</h3>");
            // TODO XXX XYZ
            out.println(
                    "<h5>Ens ha de proporcionar certes dades del seu DNI. Les ha d'escriure TAL I COM APAREIXEN al Document d'Identitat.</h5>");

            out.println("<form action=\"" + relativePluginRequestPath + "/"
                    + EVIDENCIA_DADES_DNI_PAGE + "\" method=\"post\" >"); // enctype=\"multipart/form-data\"

            out.println("<table class=\"table table-striped\">");
            out.println("<thead>\r\n" + "    <tr>\r\n" + "      <th scope=\"col\">Camp</th>\r\n"
                    + "      <th scope=\"col\">Valor</th>\r\n" + "    </tr>\r\n" + "  </thead>\r\n"
                    + "  <tbody>");

            String[][] camps = { { "DNI", "dni" }, { "Nom:", "nom" },
                    { "Llinatge 1:", "llinatge1" }, { "Llinatge 2:", "llinatge2" },
                    { "Data Caducitat:", "datacaducitat" } };

            for (String[] camp : camps) {
                out.println("<tr>");
                out.println("<td>" + camp[0] + "</td>"); // XYZ ZZZ
                out.println("<td><input type=\"text\" id=\"" + camp[1] + "\" name=\"" + camp[1]
                        + "\" /></td>");
                out.println("</tr>");
            }

            /*
             * out.println("<tr>"); out.println("<td>" + getTraduccio("contrasenyap12",
             * locale)+ ":</td>"); out.
             * println("<td><input type=\"password\" style=\"display: none;\" /><input type=\"password\" name=\""
             * + SERVER_PLUGIN_CLASS + "\" value=\"\" /></td>"); out.println("</tr>");
             * out.println("<tr>"); out.println("<td>" + getTraduccio("pin", locale) +
             * "&nbsp; <span title=\"" + getTraduccio("pin.desc", locale) +
             * "\">[ &iopf; ]</span>:</td>");
             * out.println("<td><input type=\"password\"  name=\"" + FIELD_PIN +
             * "\" value=\"\" /></td>"); out.println("</tr>");
             */
            out.println("</tbody>");
            out.println("</table>");
            out.println("<br />");

            out.println("&nbsp;&nbsp;");
            out.println("<button class=\"btn btn-primary\" type=\"submit\">" + "Acceptar"
                    + "</button>"); // XYZ ZZZ

            {
                out.println("&nbsp;&nbsp;");
                String path = relativePluginRequestPath + "/" + PAGINA_PRINCIPAL_PAGE;
                String label = getTraduccio("tornar", locale);
                String htmlCode = generateButton(path, label, "btn-warning");
                out.println(htmlCode);
            }
out.println("</form>");
            /**
             * if (query.endsWith("canreturn")) { String redirect = pluginRequestPath + "/"
             * + SELECT_CERTIFICATE_PAGE; out.println("&nbsp;<button type=\"button\"
             * class=\"btn\" onclick=\"location.href='" + redirect + "'\">");
             * out.println(getTraduccio("tornar", locale)); out.println("</button>"); }
             * 
             * 
             * 
             */
        } else {
            // POST

            // TODO XYZ XYZ Falta validar valors passats
            
            log.info("\n PASSA PEER POST DE DNI ... \n");

            String id = signaturesSet.getSignaturesSetID();

            StatusFirmaNoCriptografica sfn = statusBySignatureSetID.get(id);

            if (sfn != null) {
                Map<String, Integer> mappunts = sfn.getEvidenciesPuntsMap();
              mappunts.put(EvidenciaDniNomLlinatges.CODI,
                    new EvidenciaDniNomLlinatges().getPunts());
              
              
              
            }

            try {
                response.sendRedirect(relativePluginRequestPath + "/" + PAGINA_PRINCIPAL_PAGE);
            } catch (IOException e) { 
                e.printStackTrace(); // TODO 
                super.errorPage(id, e, relativePluginRequestPath, relativePluginRequestPath, request, response, signaturesSet, locale);
            }

        }
    }
    
    
    
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ------------------ EVIDENCIA DNI-NON-LLINATGE-DATA_CADUCITAT ---------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    private static final String EVIDENCIA_TELEFON_MOBIL = "evidencia"
            + EvidenciaTelefonMobil.CODI;

    private void evidenciaTelefonMobil(HttpServletRequest request, HttpServletResponse response,
            String relativePluginRequestPath, String query, SignaturesSetWeb signaturesSet,
            PrintWriter out, Locale locale, boolean isGet) {

        if (isGet) {
            // TODO XXX XYZ
            out.println("<h3>Evidencies utilitzant el telèfon Mòbil</h3>");
            // TODO XXX XYZ
            out.println(
                    "<h5>Enviarem un codi al seu mòbil per validar el seu numero de telefon</h5>");

            out.println("<form action=\"" + relativePluginRequestPath + "/"
                    + EVIDENCIA_TELEFON_MOBIL + "\" method=\"post\" >"); // enctype=\"multipart/form-data\"

            out.println("<table class=\"table table-striped\">");
            out.println("<thead>\r\n" + "    <tr>\r\n" + "      <th scope=\"col\">Camp</th>\r\n"
                    + "      <th scope=\"col\">Valor</th>\r\n" + "    </tr>\r\n" + "  </thead>\r\n"
                    + "  <tbody>");

            String[][] camps = { { "Telefon Mòbil", "telefon" }, { "Codi Rebut:", "codi" }
                     };

            for (String[] camp : camps) {
                out.println("<tr>");
                out.println("<td>" + camp[0] + "</td>"); // XYZ ZZZ
                out.print("<td><input type=\"text\" id=\"" + camp[1] + "\" name=\"" + camp[1] + "\" /></td>");
                
                if ("telefon".equals(camp[1])) {
                    out.println("<td><button class=\"btn btn-info\" type=\"button\">" + "Enviar Codi" +  "</button></td>"); // XYZ ZZZ    
                } else {
                    out.println("<td>&nbsp;</td>");
                }
                
                
                out.println("</tr>");
               
            }

            /*
             * out.println("<tr>"); out.println("<td>" + getTraduccio("contrasenyap12",
             * locale)+ ":</td>"); out.
             * println("<td><input type=\"password\" style=\"display: none;\" /><input type=\"password\" name=\""
             * + SERVER_PLUGIN_CLASS + "\" value=\"\" /></td>"); out.println("</tr>");
             * out.println("<tr>"); out.println("<td>" + getTraduccio("pin", locale) +
             * "&nbsp; <span title=\"" + getTraduccio("pin.desc", locale) +
             * "\">[ &iopf; ]</span>:</td>");
             * out.println("<td><input type=\"password\"  name=\"" + FIELD_PIN +
             * "\" value=\"\" /></td>"); out.println("</tr>");
             */
            out.println("</tbody>");
            out.println("</table>");
            out.println("<br />");

            out.println("&nbsp;&nbsp;");
            out.println("<button class=\"btn btn-primary\" type=\"submit\">" + "Acceptar"
                    + "</button>"); // XYZ ZZZ

            {
                out.println("&nbsp;&nbsp;");
                String path = relativePluginRequestPath + "/" + PAGINA_PRINCIPAL_PAGE;
                String label = getTraduccio("tornar", locale);
                String htmlCode = generateButton(path, label, "btn-warning");
                out.println(htmlCode);
            }
out.println("</form>");
            /**
             * if (query.endsWith("canreturn")) { String redirect = pluginRequestPath + "/"
             * + SELECT_CERTIFICATE_PAGE; out.println("&nbsp;<button type=\"button\"
             * class=\"btn\" onclick=\"location.href='" + redirect + "'\">");
             * out.println(getTraduccio("tornar", locale)); out.println("</button>"); }
             * 
             * 
             * 
             */
        } else {
            // POST

            // TODO XYZ XYZ Falta validar valors passats
            
            log.info("\n PASSA PER POST DE TELEFON MOBIL ... \n");

            String id = signaturesSet.getSignaturesSetID();

            StatusFirmaNoCriptografica sfn = statusBySignatureSetID.get(id);

            if (sfn != null) {
                Map<String, Integer> mappunts = sfn.getEvidenciesPuntsMap();
              mappunts.put(EvidenciaTelefonMobil.CODI,
                    new EvidenciaTelefonMobil().getPunts());
            }

            try {
                response.sendRedirect(relativePluginRequestPath + "/" + PAGINA_PRINCIPAL_PAGE);
            } catch (IOException e) { 
                e.printStackTrace(); // TODO 
                super.errorPage(id, e, relativePluginRequestPath, relativePluginRequestPath, request, response, signaturesSet, locale);
            }

        }
    }
    
    

    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------
    // ------------------------------------ FIRMAR -------------------------------
    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------

    private static final String FIRMAR_PAGE = "firmar";

    private void firmarPOST(String absolutePluginRequestPath, String relativePluginRequestPath,
            HttpServletRequest request, HttpServletResponse response,
            SignaturesSetWeb signaturesSet, Locale locale) {

        final String signaturesSetID = signaturesSet.getSignaturesSetID();

        try {

            ISignatureServerPlugin pluginServer = getSignatureServerPlugin();

            pluginServer.signDocuments(signaturesSet, signaturesSetID,
                    parametersBySignatureSetID.get(signaturesSetID));

            signaturesSet.getStatusSignaturesSet().setStatus(StatusSignaturesSet.STATUS_FINAL_OK);

        } catch (Throwable th) {
            // TODO Mirar certs tipus d'excepció

            log.error("Error Firmant: " + th.getMessage() + " [CLASS: " + th.getClass().getName()
                    + "]", th);

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

        /*
         * FileInfoSignature[] fileInfoArray =
         * signaturesSet.getFileInfoSignatureArray();
         * 
         * for (int i = 0; i < fileInfoArray.length; i++) { final CommonInfoSignature
         * commonInfoSignature = signaturesSet.getCommonInfoSignature();
         * 
         * String pin = request.getParameter(FIELD_PIN); String cert =
         * request.getParameter("cert");
         * 
         * File certPath = new File(getUserNamePath(commonInfoSignature.getUsername()),
         * cert);
         * 
         * if (!certPath.exists()) { saveMessageError(signaturesSetID, "El path " +
         * certPath.getAbsolutePath() + " no existeix "); sendRedirect(response,
         * relativePluginRequestPath + "/" + SELECT_CERTIFICATE_PAGE); return; }
         * 
         * Properties prop; try { prop = readPropertiesFromCertPath(certPath);
         * 
         * } catch (Exception ex) { String msg = getTraduccio("error.pinerroni",
         * locale); saveMessageError(signaturesSetID, msg); log.warn(msg + ": " +
         * ex.getMessage(), ex); sendRedirect(response, relativePluginRequestPath + "/"
         * + SELECT_CERTIFICATE_PAGE); return; }
         * 
         * String p12PasswordEncriptedB64 =
         * prop.getProperty(PROPERTY_P12PASSWORD_ENCRIPTED);
         * 
         * String p12Password; try { p12Password = EncrypterDecrypter.decrypt(ALGORITHM,
         * fillPwd(pin, 16).getBytes(), p12PasswordEncriptedB64); } catch (Exception ex)
         * { String msg = getTraduccio("error.pinerroni", locale);
         * saveMessageError(signaturesSetID, msg); log.warn(msg + ": " +
         * ex.getMessage(), ex); sendRedirect(response, relativePluginRequestPath + "/"
         * + SELECT_CERTIFICATE_PAGE); return; }
         * 
         * // Check PAIR PublicCertificatePrivateKeyPair pair; File p12file = new
         * File(certPath, FILENAME_P12); try { FileInputStream fis = new
         * FileInputStream(p12file); pair = CertificateUtils.readPKCS12(fis,
         * p12Password); fis.close(); } catch (Exception e) { // TODO traduir missatge
         * String msg = "Error llegint fitxer P12 (" + p12file.getAbsolutePath() + ")."
         * + " Consulti amb l'Administrador. Error: " + e.getMessage();
         * 
         * saveMessageError(signaturesSetID, msg); log.warn(msg + ": " + e.getMessage(),
         * e); sendRedirect(response, relativePluginRequestPath + "/" +
         * SELECT_CERTIFICATE_PAGE); return; }
         * 
         * 
         * 
         * int pos = relativePluginRequestPath.lastIndexOf("-1");
         * 
         * String baseSignaturesSet = relativePluginRequestPath.substring(0, pos - 1);
         * 
         * // int errors = 0; X509Certificate certificate = pair.getPublicCertificate();
         * PrivateKey privateKey = pair.getPrivateKey();
         * 
         * signaturesSet.getStatusSignaturesSet().setStatus(StatusSignaturesSet.
         * STATUS_IN_PROGRESS);
         * 
         * long start;
         * 
         * 
         * 
         * start = System.currentTimeMillis(); FileInfoSignature fileInfo =
         * fileInfoArray[i];
         * 
         * try {
         * 
         * String timeStampUrl = null; if (fileInfo.getTimeStampGenerator() != null) {
         * String callbackhost = getHostAndContextPath(absolutePluginRequestPath,
         * relativePluginRequestPath); timeStampUrl = callbackhost + baseSignaturesSet +
         * "/" + i + "/" + TIMESTAMP_PAGE; }
         * 
         * MiniAppletSignInfo info; info =
         * MiniAppletUtils.convertLocalSignature(commonInfoSignature, fileInfo,
         * timeStampUrl, certificate);
         * 
         * if (FileInfoSignature.SIGN_TYPE_PADES.equals(fileInfo.getSignType()) ||
         * FileInfoSignature.SIGN_TYPE_XADES.equals(fileInfo.getSignType()) ||
         * FileInfoSignature.SIGN_TYPE_CADES.equals(fileInfo.getSignType())) {
         * 
         * // FIRMA PADES o Xades StatusSignature ss = fileInfo.getStatusSignature();
         * 
         * ss.setStatus(StatusSignature.STATUS_IN_PROGRESS);
         * 
         * final String algorithm = info.getSignAlgorithm(); byte[] signedData;
         * 
         * if (FileInfoSignature.SIGN_TYPE_PADES.equals(fileInfo.getSignType())) {
         * 
         * AbstractTriFaseSigner cloudSign = new MiniAppletInServerPAdESSigner(
         * privateKey);
         * 
         * signedData = cloudSign.fullSign(info.getDataToSign(), algorithm, new
         * Certificate[] { info.getCertificate() }, info.getProperties()); } else if
         * (FileInfoSignature.SIGN_TYPE_CADES.equals(fileInfo.getSignType())) {
         * log.debug("Passa per CAdESSigner");
         * 
         * MiniAppletInServerCAdESSigner cadesSigner = new
         * MiniAppletInServerCAdESSigner();
         * 
         * // StringWriter sw = new StringWriter(); // info.getProperties().store(sw,
         * "PropertiesDemo"); // log.info("CADES PROPERTIES:\n" + sw.toString() );
         * signedData = cadesSigner.sign(info.getDataToSign(), algorithm, privateKey,
         * new Certificate[] { info.getCertificate() }, info.getProperties());
         * 
         * } else {
         * 
         * log.debug("Passa per XAdESSigner"); MiniAppletInServerXAdESSigner xadesSigner
         * = new MiniAppletInServerXAdESSigner();
         * 
         * signedData = xadesSigner.sign(info.getDataToSign(), algorithm, privateKey,
         * new Certificate[] { info.getCertificate() }, info.getProperties());
         * 
         * }
         * 
         * File firmat = File.createTempFile("MAISSigWebPlugin", "signedfile");
         * 
         * FileOutputStream fos = new FileOutputStream(firmat); fos.write(signedData);
         * fos.flush(); fos.close();
         * 
         * ss.setSignedData(firmat); ss.setStatus(StatusSignature.STATUS_FINAL_OK);
         * 
         * } else { // TODO Falta CADes,
         * 
         * // "Tipus de Firma amb ID {0} no està suportat pel plugin `{1}` String msg =
         * getTraduccio("tipusfirma.desconegut", locale, fileInfo.getSignType(),
         * this.getName(locale));
         * 
         * StatusSignature ss = fileInfo.getStatusSignature(); ss.setErrorMsg(msg);
         * ss.setErrorException(null); ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);
         * }
         * 
         * 
         * 
         * } catch (Throwable th) { // TODO Mirar certs tipus d'excepció
         * 
         * log.error("Error Firmant: " + th.getMessage() + " [CLASS: " +
         * th.getClass().getName() + "]", th);
         * 
         * StatusSignature ss = getStatusSignature(signaturesSetID, i);
         * 
         * ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);
         * 
         * ss.setErrorException(th);
         * 
         * String msg; Throwable cause = th.getCause(); if (cause != null && th
         * instanceof java.lang.reflect.InvocationTargetException) { msg =
         * cause.getMessage(); } else { msg = th.getMessage(); }
         * 
         * ss.setErrorMsg(getTraduccio("error.firmantdocument", locale) +
         * fileInfo.getName() + " (" + i + "):" + msg); } } if (log.isDebugEnabled()) {
         * log.debug("Firma amb ID " + fileInfo.getSignID() + " finalitzada en " +
         * (System.currentTimeMillis() - start) + "ms "); }
         * 
         */

    }

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ---------------------- U T I L I T A T S H T M L -------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    protected String generateButton(String path, String label, String tipus) {
        String htmlCode = "<button class=\"btn " + tipus
                + "\" type=\"button\"  onclick=\"location.href='" + path + "'\" >" + label
                + "</button>";
        return htmlCode;
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
            return getSignatureServerPlugin().acceptExternalRubricGenerator();
        } catch (Exception e) {
            log.error("acceptExternalRubricGenerator: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean acceptExternalSecureVerificationCodeStamper() {
        try {
            return getSignatureServerPlugin().acceptExternalSecureVerificationCodeStamper();
        } catch (Exception e) {
            log.error("acceptExternalSecureVerificationCodeStamper: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean acceptExternalTimeStampGenerator(String signType) {
        try {
            return getSignatureServerPlugin().acceptExternalTimeStampGenerator(signType);
        } catch (Exception e) {
            log.error("acceptExternalTimeStampGenerator: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> getSupportedBarCodeTypes() {
        try {
            return getSignatureServerPlugin().getSupportedBarCodeTypes();
        } catch (Exception e) {
            log.error("getSupportedBarCodeTypes: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String[] getSupportedSignatureAlgorithms(String signType) {
        try {
            return getSignatureServerPlugin().getSupportedSignatureAlgorithms(signType);
        } catch (Exception e) {
            log.error("getSupportedSignatureAlgorithms: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String[] getSupportedSignatureTypes() {
        try {
            return getSignatureServerPlugin().getSupportedSignatureTypes();
        } catch (Exception e) {
            log.error("getSupportedSignatureTypes: " + e.getMessage(), e);
            return null;
        }

    }

    @Override
    public boolean providesRubricGenerator() {
        try {
            return getSignatureServerPlugin().providesRubricGenerator();
        } catch (Exception e) {
            log.error("providesRubricGenerator: " + e.getMessage(), e);
            return false;
        }

    }

    @Override
    public boolean providesSecureVerificationCodeStamper() {
        try {
            return getSignatureServerPlugin().providesSecureVerificationCodeStamper();
        } catch (Exception e) {
            log.error("providesSecureVerificationCodeStamper: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean providesTimeStampGenerator(String signType) {
        try {
            return getSignatureServerPlugin().providesTimeStampGenerator(signType);
        } catch (Exception e) {
            log.error("providesTimeStampGenerator: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void resetAndClean(HttpServletRequest request) throws Exception {

        try {
            getSignatureServerPlugin().resetAndClean();

            statusBySignatureSetID.clear();

            parametersBySignatureSetID.clear();

        } catch (Exception e) {
            log.error("resetAndClean: " + e.getMessage(), e);
        }
    }

}
