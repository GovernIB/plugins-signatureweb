package org.fundaciobit.pluginsib.signatureweb.fire;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fundaciobit.pluginsib.core.v3.utils.FileUtils;
import org.fundaciobit.pluginsib.signature.api.CommonInfoSignature;
import org.fundaciobit.pluginsib.signature.api.FileInfoSignature;
import org.fundaciobit.pluginsib.signature.api.StatusSignature;
import org.fundaciobit.pluginsib.signature.api.StatusSignaturesSet;
import org.fundaciobit.pluginsib.signatureserver.miniappletutils.MIMEInputStream;
import org.fundaciobit.pluginsib.signatureserver.miniappletutils.MiniAppletSignInfo;
import org.fundaciobit.pluginsib.signatureserver.miniappletutils.MiniAppletUtils;
import org.fundaciobit.pluginsib.signatureserver.miniappletutils.SMIMEInputStream;
import org.fundaciobit.pluginsib.signatureweb.api.AbstractSignatureWebPlugin;
import org.fundaciobit.pluginsib.signatureweb.api.SignaturesSetWeb;
import org.fundaciobit.pluginsib.signatureweb.miniappletutils.AbstractMiniAppletSignaturePlugin;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.signers.tsp.pkcs7.CMSTimestamper;
import es.gob.afirma.signers.tsp.pkcs7.TsaParams;
import es.gob.clavefirma.client.HttpForbiddenException;
import es.gob.clavefirma.client.HttpNetworkException;

import es.gob.clavefirma.client.signprocess.HttpSignProcessConstants;
import es.gob.fire.client.Base64;
import es.gob.fire.client.BatchResult;
import es.gob.fire.client.CreateBatchResult;
import es.gob.fire.client.FireClient;
import es.gob.fire.client.SignBatchResult;
import es.gob.fire.client.SignOperationResult;
import es.gob.fire.client.TransactionResult;

/**
 * 
 * @author anadal
 * @author areus
 */
public class FIReSignatureWebPlugin extends AbstractMiniAppletSignaturePlugin {

    public static final String FIRE_BASE_PROPERTIES = SIGNATUREWEB_BASE_PROPERTY + "fire.";

    // Firma
    public static final String OPERATION_SIGN = "sign";

    // CoFirma
    public static final String OPERATION_COSIGN = "cosign";

    // ContraFirma
    public static final String OPERATION_COUNTERSIGN = "countersign";

    private static final String PROPERTY_MAPPING_USERS_PATH = FIRE_BASE_PROPERTIES + "mappingusers";

    private static final String PROPERTY_USERS_PATTERN = FIRE_BASE_PROPERTIES + "userspattern";

    private static final String PROPERTY_CALLBACK_HOST = FIRE_BASE_PROPERTIES + "callbackhost";

    protected Map<String, FIReSignaturesSet> transactions = new ConcurrentHashMap<String, FIReSignaturesSet>();

    protected Map<String, String> generateCertificateTransactions = new ConcurrentHashMap<String, String>();

    /**
     * S'utilitza per guardar les propietats que s'utilitza per la firma SMIME o
     * CADES quan aquesta demana Segellat de Temps. S'ha de fer a posteriori dins
     * del mètode storeDocument() ja que CADES trifase no suporta nativament
     * Segellat de Temps.
     */
    protected final Map<String, Properties[]> timeStampCache = new ConcurrentHashMap<String, Properties[]>();

    public FIReSignatureWebPlugin() {
        super();
    }

    public FIReSignatureWebPlugin(String propertyKeyBase, Properties properties) {
        super(propertyKeyBase, properties);
    }

    public FIReSignatureWebPlugin(String propertyKeyBase) {
        super(propertyKeyBase);
    }

    protected int cacheMaxEntries() {
        return Integer.parseInt(getProperty(FIRE_BASE_PROPERTIES + "cacheMaxEntries", "1000"));
    }

    protected int cacheMaxTimeToLive() {
        return Integer.parseInt(getProperty(FIRE_BASE_PROPERTIES + "cacheMaxTimeToLive", "900"));
    }

    protected boolean isDebug() {
        return log.isDebugEnabled() || "true".equalsIgnoreCase(getProperty(FIRE_BASE_PROPERTIES + "debug"));
    }

    protected String getAppID() throws Exception {
        return getPropertyRequired(FIRE_BASE_PROPERTIES + "appid");
    }

    protected String getCertOrigin() {
        return getProperty(FIRE_BASE_PROPERTIES + "certOrigin");
    }

    protected String getAppName() {
        return getProperty(FIRE_BASE_PROPERTIES + "appName");
    }

    @Override
    public String getName(Locale locale) {
        return getTraduccio("pluginname", locale);
    }

    /**
     * Indica quan l'origen dels certificats no és exclusivament de Clavefirma, és a dir,
     * que és mode mixt o local, per tant l'usuari pot signar amb un certificat local.
     */
    private boolean isModeMixtOrLocal() {
        String origin = getCertOrigin();
        return (origin == null || origin.isEmpty() || "local".equals(origin));
    }

    @Override
    public String filter(HttpServletRequest request, SignaturesSetWeb signaturesSet, Map<String, Object> parameters) {

        // FIRe és incompatible amb ClaveFirma
        try {

            Class.forName("es.gob.clavefirma.client.ConfigManager");
            // XYZ ZZZ TODO Traduir
            log.warn("\n\n\n" + " ------------------------------------------------------------------------\n"
                    + " |  El Plugin de Cl@veFirma i el Plugin de FIRe son incompatibles !!!!  |\n"
                    + " |  S'ha d'eliminar el jar d'un d'aquest dos plugins de lib.            |\n"
                    + " ------------------------------------------------------------------------\n" + "\n\n\n");

            return "El Plugin de Cl@veFirma i el Plugin de FIRe son incompatibles."
                    + "S'ha d'eliminar el jar d'un d'aquest dos plugins de lib.";
        } catch (ClassNotFoundException cnfe) {
            // OK
        } catch (Exception e) {
            // XYZ ZZZ TODO Traduir
            String msg = "No puc descobrir si esta instal·lat el plugin de ClaveFirma."
                    + " Continuarem. Error llançat: " + e.getMessage();
            log.warn(msg, e);

            return msg;

        }

        if (isModeMixtOrLocal()) {
            // Si origin es local o no definit, llavors hem de controlar que no hi
            // hagi estampació de Taules ja que no seria posible

            FileInfoSignature[] signatures = signaturesSet.getFileInfoSignatureArray();

            for (int i = 0; i < signatures.length; i++) {
                if (signatures[i].getSignaturesTableLocation() != FileInfoSignature.SIGNATURESTABLELOCATION_WITHOUT) {
                    // XYZ ZZZ TODO Traduir
                    String msg = "La signatura de la posició " + i
                            + " requereix taula de firmes però només es posible si la propietat "
                            + getPropertyName(FIRE_BASE_PROPERTIES + "certOrigin") + " està en mode mixt o local";
                    if (isDebug()) {
                        log.warn(msg);
                    }
                    return msg;
                }
            }

        }

        return super.filter(request, signaturesSet, parameters);
    }

    @Override
    public void closeSignaturesSet(HttpServletRequest request, String id) {
        timeStampCache.remove(id);
        transactions.remove(id);
        generateCertificateTransactions.remove(id);
        super.closeSignaturesSet(request, id);
    }

    @Override
    public void requestGET(String absolutePluginRequestPath, String relativePluginRequestPath, String relativePath,
            SignaturesSetWeb signaturesSet, int signatureIndex, HttpServletRequest request,
            HttpServletResponse response, Locale locale) {
        if (relativePath.startsWith(FIRMAR_PRE_PAGE)) {
            firmarPre(absolutePluginRequestPath, relativePluginRequestPath, relativePath, request, response,
                    signaturesSet, locale);
        } else if (relativePath.startsWith(FIRMAR_POST_PAGE)) {
            firmarPostOk(request, response, signaturesSet, signatureIndex, locale);

        } else if (relativePath.startsWith(SIGN_ERROR_PAGE)) {
            signErrorPage(absolutePluginRequestPath, relativePluginRequestPath, request, response, signaturesSet,
                    signatureIndex, relativePath, locale);
        } else if (relativePath.startsWith(CLOSE_FIRE_PAGE)) {
            closeFirePage(response, locale);
        } else {
            super.requestGET(absolutePluginRequestPath, relativePluginRequestPath, relativePath, signaturesSet,
                    signatureIndex, request, response, locale);
        }

    }

    @Override
    public void requestPOST(String absolutePluginRequestPath, String relativePluginRequestPath, String relativePath,
            SignaturesSetWeb signaturesSet, int signatureIndex, HttpServletRequest request,
            HttpServletResponse response, Locale locale) {
        if (relativePath.startsWith(FIRMAR_PRE_PAGE)) {
            firmarPre(absolutePluginRequestPath, relativePluginRequestPath, relativePath, request, response,
                    signaturesSet, locale);

        } else if (relativePath.startsWith(SIGN_ERROR_PAGE)) {
            signErrorPage(absolutePluginRequestPath, relativePluginRequestPath, request, response, signaturesSet,
                    signatureIndex, relativePath, locale);
        } else {
            super.requestPOST(absolutePluginRequestPath, relativePluginRequestPath, relativePath, signaturesSet,
                    signatureIndex, request, response, locale);
        }

    }

    /**
     * Inici del proces de firma
     */
    @Override
    public String signDocuments(HttpServletRequest request, String absolutePluginRequestPath,
            String relativePluginRequestPath, SignaturesSetWeb signaturesSet, Map<String, Object> parameters)
            throws Exception {

        addSignaturesSet(signaturesSet);
        return relativePluginRequestPath + "/" + FIRMAR_PRE_PAGE;

    }

    @Override
    protected void getJavascriptCSS(HttpServletRequest request, String absolutePluginRequestPath,
            String relativePluginRequestPath, PrintWriter out, AbstractSignatureWebPlugin.SignIDAndIndex key,
            SignaturesSetWeb value) {

        super.getJavascriptCSS(request, absolutePluginRequestPath, relativePluginRequestPath, out, key, value);

        String newJS = getProperty(FIRE_BASE_PROPERTIES + "newjavascripturl");

        if (newJS != null && newJS.trim().length() != 0) {
            out.println("<script type=\"text/javascript\" src=\"" + newJS + "\"></script>");
        }

        String newCSS = getProperty(FIRE_BASE_PROPERTIES + "newcssurl");

        if (newCSS != null && newCSS.trim().length() != 0) {
            out.println("<link href=\"" + newCSS + "\" rel=\"stylesheet\" type=\"text/css\">");
        }

    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // ------------- TANCAR FINESTRA DE LA WEB DE CL@VEFIRMA -------------------
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static final String CLOSE_FIRE_PAGE = "closefirepage";

    private void closeFirePage(HttpServletResponse response, Locale locale) {
        PrintWriter out;
        try {

            response.setCharacterEncoding("utf-8");
            response.setContentType("text/html");

            out = response.getWriter();

            out.println("<html><head>" + "<script type=\"text/javascript\">" + "    window.close();" + "</script>"
                    + "</head><body>" + "</body></html>");

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ------------------ FIRMAR PRE ---------------------------------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    private static final String FIRMAR_PRE_PAGE = "firmarpre";

    protected void firmarPre(String absolutePluginRequestPath, String relativePluginRequestPath, String query,
            HttpServletRequest request, HttpServletResponse response, SignaturesSetWeb signaturesSet, Locale locale) {

        final boolean debug = isDebug();

        if (debug) {
            log.info("firmarPre::absolutePluginRequestPath=" + absolutePluginRequestPath);
            log.info("firmarPre::relativePluginRequestPath=" + relativePluginRequestPath);
            log.info("firmarPre:: QUERY = |" + query + "|");

            Map<String, String[]> params = request.getParameterMap();
            for (Object key : params.keySet()) {
                log.info("firmarPre():: param[" + key + "] = " + ((String[]) params.get(key))[0]);
            }
        }

        try {
            X509Certificate certificate = null;

            firmar(absolutePluginRequestPath, relativePluginRequestPath, request, response, signaturesSet, locale,
                    debug, certificate);
        } catch (Exception e) {
            // TODO XYZ FILTRAR ERRORS . Veure documentacio

            // TODO Traduir
            String msg = " Error desconegut preparant l'enviament dels documents al servidor de FIRe: "
                    + e.getMessage();

            finishWithError(response, signaturesSet, msg, e);

        }
    }

    protected void firmar(String absolutePluginRequestPath, String relativePluginRequestPath,
            HttpServletRequest request, HttpServletResponse response, SignaturesSetWeb signaturesSet, Locale locale,
            final boolean debug, X509Certificate certificate)
            throws Exception, UnsupportedEncodingException, IOException {

        // --------------------
        // INITIALIZE

        final String signaturesSetID = signaturesSet.getSignaturesSetID();
        final CommonInfoSignature commonInfoSignature = signaturesSet.getCommonInfoSignature();

        int pos = relativePluginRequestPath.lastIndexOf("-1");

        String baseSignaturesSet = relativePluginRequestPath.substring(0, pos - 1);

        final String appId = getAppID();
        final String subjectId = getClaveFirmaUser(commonInfoSignature.getUsername(),
                commonInfoSignature.getAdministrationID());
        if (debug) {
            log.info("firmarPre:: Usuari Clave Firma = |" + subjectId + "|");
        }

        // TODO Check que tots els fitxers firmen amb el mateix tipus d'algorisme
        FileInfoSignature[] fileInfoArray = signaturesSet.getFileInfoSignatureArray();

        FIReFileInfoSignature[] fireFirmaSignInfoArray = new FIReFileInfoSignature[fileInfoArray.length];

        final Properties commonRemoteConfProperties = new Properties();

        // FILTRE DE CERTIFICATS
        // Afegir Filtre de Certificats si mode mix (local o cl@avefirma) o mode local
        final String certOrigin = getCertOrigin();
        if ("local".equals(certOrigin) || certOrigin == null) {

            String filtre = signaturesSet.getCommonInfoSignature().getFiltreCertificats();
            if (debug) {
                log.info("firmar::FILTRE[" + filtre + "]");
            }
            if (filtre != null && filtre.trim().length() != 0) {

                try {
                    Properties propFiltre = new Properties();
                    propFiltre.load(new StringReader(filtre));

                    if (debug) {
                        Set<Object> keys = propFiltre.keySet();
                        for (Object key : keys) {
                            log.info("firma::PropertiesFILTRE[" + key + "] => " + propFiltre.get(key));
                        }
                    }

                    commonRemoteConfProperties.putAll(propFiltre);
                } catch (Exception e) {
                    // TODO tradudir XYZ ZZZ
                    final String msg = " Error processant filtre de certificats: " + e.getMessage();
                    finishWithError(response, signaturesSet, msg, e);
                    return;
                }
            }
        }

        // ============= DADES COMUNS

        String callBackURLOK;
        String callBackURLError;
        String tancarFinestraURL;

        String callbackhost = getProperty(PROPERTY_CALLBACK_HOST);

        if (callbackhost == null) {
            callBackURLOK = absolutePluginRequestPath + "/" + FIRMAR_POST_PAGE;
            callBackURLError = absolutePluginRequestPath + "/" + SIGN_ERROR_PAGE + "/PROCES_DE_FIRMA";
            tancarFinestraURL = absolutePluginRequestPath + "/" + CLOSE_FIRE_PAGE;
        } else {
            callBackURLOK = callbackhost + request.getServletPath() + "/" + FIRMAR_POST_PAGE;
            callBackURLError = callbackhost + request.getServletPath() + "/" + SIGN_ERROR_PAGE + "/PROCES_DE_FIRMA";
            tancarFinestraURL = callbackhost + request.getServletPath() + "/" + CLOSE_FIRE_PAGE;
        }

        if (debug) {
            log.info("firmarPre::callBackURLOK = " + callBackURLOK);
            log.info("firmarPre::callBackURLError = " + callBackURLError);
            log.info("firmarPre::tancarFinestraURL = " + tancarFinestraURL);
        }

        // redirectOkUrl: URL a la que redirigir al usuario en caso de terminar
        // correcta-mente la operación.
        commonRemoteConfProperties.setProperty("redirectOkUrl", callBackURLOK);
        // redirectErrorUrl: URL a la que redirigir al usuario en caso de error.
        commonRemoteConfProperties.setProperty("redirectErrorUrl", callBackURLError);
        // procedureName: Nombre del procedimiento que se ejecuta (previamente
        // dado de alta en la GISS).
        commonRemoteConfProperties.setProperty("procedureName",
                getPropertyRequired(FIRE_BASE_PROPERTIES + "procedure"));

        // Configuramos si el certificado es local o de Cl@ve Firma (Opcional)
        if (certOrigin != null) {
            commonRemoteConfProperties.setProperty("certOrigin", certOrigin); //$NON-NLS-1$
        }

        // Configuramos el nombre de la aplicacion (opcional)
        final String appName = getAppName();
        if (appName != null) {
            commonRemoteConfProperties.setProperty("appName", appName); //$NON-NLS-1$
        }

        if (debug) {

            StringWriter writer = new StringWriter();
            commonRemoteConfProperties.list(new PrintWriter(writer));

            log.info(" -------------- COMMON REMOTE PROPERTIES -------------------\n" + writer.getBuffer().toString());

        }

        // --------------------
        // Crear BATCH
        FireClient fireClient = instantiateFireClient();

        // Possibles valors = "ES-A", ES-T i ES-LTV
        final String upgrade = null;

        CreateBatchResult createBatchResult;
        final String transactionId;
        try {

            // Com que cada firma defieix l'operacio i tipus..., és igual el valor per defecte
            final String signTypeClaveFirma = "PAdES"; // valor per defecte
            final String operation = OPERATION_SIGN; // valor per defecte
            final String extraparams = ""; // valor per defecte

            // però en el cas de l'algoritme només en podem agafar un. així que agafam el primer
            final String signAlgorithm = convertSignAlgorithm(fileInfoArray[0].getSignAlgorithm()).toString();

            createBatchResult = fireClient.createBatchProcess(subjectId, operation, signTypeClaveFirma, signAlgorithm,
                    extraparams, upgrade, commonRemoteConfProperties);

            // Del resultado obtenemos:
            // - El identificador de transaccion necesario para operar con el lote.
            transactionId = createBatchResult.getTransactionId();

            if (debug) {
                log.info("firmar:: fireClient.createBatchProcess() => Recibido el ID de transaccion: " + transactionId);
            }

        } catch (final Exception e) {
            // TODO XYZ ZZZ TRADUIR
            // String msg = getTraduccio("warn.nosuportamultiplesfirmes", locale,
            // this.getName(locale));
            final String msg = "S'ha produit un error al crear el lot de firmes: " + e.getMessage();
            finishWithError(response, signaturesSet, msg, e);
            return;
        }

        // ============= AFEGIR FIRMA A BATCH

        for (int i = 0; i < fileInfoArray.length; i++) {
            try {

                FileInfoSignature fileInfo = fileInfoArray[i];

                final String signTypeOrig = fileInfo.getSignType();

                String timeStampUrl = null;
                if (fileInfo.getTimeStampGenerator() != null) {
                    String callbackhostTS = getHostAndContextPath(absolutePluginRequestPath, relativePluginRequestPath);

                    timeStampUrl = callbackhostTS + baseSignaturesSet + "/" + i + "/" + TIMESTAMP_PAGE;
                }

                MiniAppletSignInfo info;
                info = MiniAppletUtils.convertLocalSignature(commonInfoSignature, fileInfo, timeStampUrl, certificate);

                final Properties signProperties = info.getProperties();

                byte[] dataToSign = info.getDataToSign();

                if (FileInfoSignature.SIGN_TYPE_SMIME.equals(signTypeOrig)) {
                    // SMIME
                    String mimeType = fileInfo.getMimeType();
                    if (mimeType == null || mimeType.trim().length() == 0) {
                        mimeType = "application/octet-stream";
                    }

                    MIMEInputStream mis = new MIMEInputStream(new ByteArrayInputStream(dataToSign), mimeType);
                    dataToSign = AOUtil.getDataFromInputStream(mis);
                    mis.close();
                }

                if (debug) {
                    log.info("firmarPre[" + i + "]:: SEGELL DE TEMPS = " + (fileInfo.getTimeStampGenerator() != null));
                }
                if (fileInfo.getTimeStampGenerator() != null) {

                    if (debug) {
                        log.info("firmar[" + i + "]:: SEGELL DE TEMPS[SIGNTYPE] PRE= " + signTypeOrig);
                    }
                    // Guardar dins Cache de Propietats si es denama timeStamp i el tipus
                    // és CADES o SMIME
                    if (FileInfoSignature.SIGN_TYPE_CADES.equals(signTypeOrig)
                            || FileInfoSignature.SIGN_TYPE_SMIME.equals(signTypeOrig)) {

                        if (debug) {
                            log.info("firmar[" + i + "]:: SEGELL DE TEMPS[SIGNTYPE] POST= " + signTypeOrig);
                        }

                        Properties[] allProp = timeStampCache.get(signaturesSetID);
                        if (allProp == null) {
                            allProp = new Properties[fileInfoArray.length];
                            timeStampCache.put(signaturesSetID, allProp);
                        }
                        allProp[i] = signProperties;
                    }

                }

                // Com que no se quines propietats van a cada costat, les copiam totes
                Properties remoteConfProperties2 = new Properties();

                remoteConfProperties2.putAll(commonRemoteConfProperties);
                remoteConfProperties2.putAll(signProperties);

                // La rúbrica de la signatura, com que es un element molt pesat, llavors
                // l'eliminam de les Properties de Firma (ja que no són d'on les llegeix
                // @firma)
                signProperties.remove("signatureRubricImage");

                if (debug) {

                    StringWriter writer = new StringWriter();
                    signProperties.list(new PrintWriter(writer));

                    log.info("--- firmar[" + i + "]:: SIGN PROPERTIES -------------------\n"
                            + writer.getBuffer().toString());

                }

                final HttpSignProcessConstants.SignatureFormat signTypeClaveFirma;
                signTypeClaveFirma = convertSignType(signTypeOrig);

                final HttpSignProcessConstants.SignatureAlgorithm signAlgorithm;
                signAlgorithm = convertSignAlgorithm(fileInfo.getSignAlgorithm());

                // Muntar Objecte
                FIReFileInfoSignature cfsi = new FIReFileInfoSignature(signTypeClaveFirma, signAlgorithm,
                        signProperties, certificate, dataToSign, remoteConfProperties2);

                final String documentId = String.valueOf(i);

                String remoteConfPropertiesB64;
                {
                    StringWriter writer = new StringWriter();

                    cfsi.getRemoteConfProperties().store(writer, "");

                    String remoteConfPropertiesStr = writer.getBuffer().toString();

                    if (debug) {
                        log.info("---  firmar[" + i + "]:: REMOTE PROPERTIES  ----- \n" + remoteConfPropertiesStr
                                + "\n\n");
                    }

                    final boolean urlSafe = true;
                    remoteConfPropertiesB64 = Base64.encode(remoteConfPropertiesStr.getBytes(), urlSafe);

                }

                fireClient.addDocumentToBatch(transactionId, subjectId, documentId, cfsi.getDataToSign(),
                        cfsi.getSignProperties(), OPERATION_SIGN, cfsi.getSignType().name(), remoteConfPropertiesB64,
                        upgrade);

                // Indicam que estam en progres
                StatusSignature ss = fileInfo.getStatusSignature();
                ss.setStatus(StatusSignature.STATUS_IN_PROGRESS);

                fireFirmaSignInfoArray[i] = cfsi;

            } catch (Throwable th) {
                String msg;
                if (th instanceof HttpForbiddenException) {
                    // XYZ ZZZ traduir
                    msg = "Error de permisos en la carga de datos: " + th.getMessage();
                } else if (th instanceof HttpNetworkException) {
                    // XYZ ZZZ traduir
                    msg = "Error de red en la carga de datos: " + th.getMessage();
                } else {
                    // XYZ ZZZ traduir
                    msg = "Error en la carga de datos a firmar: " + th.getMessage();
                }
                // Només cancelar la firma actual i no tota la transacció !!!
                FileInfoSignature fileInfo = fileInfoArray[i];
                StatusSignature ss = fileInfo.getStatusSignature();
                ss.setErrorMsg(msg);
                ss.setErrorException(th);
                ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);
            }

        }

        // ======== INICIAR EL PROCES DE FIRMA
        if (debug) {
            log.info("firmar ::  Iniciar proces de firma (fireClient.signBatch))");
        }

        final boolean stopOnError = false;
        SignOperationResult signOperationResult;
        try {
            signOperationResult = fireClient.signBatch(transactionId, subjectId, stopOnError);
        } catch (final Exception e) {

            // TODO XYZ ZZZ TRADUIR
            // String msg = getTraduccio("warn.nosuportamultiplesfirmes", locale,
            // this.getName(locale));
            final String msg = "S'ha produit un error al iniciar la firma del lot de firmes: " + e.getMessage();
            finishWithError(response, signaturesSet, msg, e);
            return;
        }

        final String redirectUrl = signOperationResult.getRedirectUrl();

        if (debug) {
            log.info("firmar()::Resultat de la cridada a fireClient.signBatch() OK.");
            log.info("            * URL de redireccio: " + redirectUrl);
        }

        this.transactions.put(signaturesSetID, new FIReSignaturesSet(appId, subjectId, transactionId,
                fireFirmaSignInfoArray, commonInfoSignature.getLanguageUI()));

        signaturesSet.getStatusSignaturesSet().setStatus(StatusSignaturesSet.STATUS_IN_PROGRESS);

        showFullScreenPage(relativePluginRequestPath, response, locale, debug, callBackURLOK, callBackURLError,
                tancarFinestraURL, redirectUrl);
    }

    protected void showFullScreenPage(String relativePluginRequestPath, HttpServletResponse response, Locale locale,
            final boolean debug, String callBackURLOK, String callBackURLError, String tancarFinestraURL,
            String redirectUrl) throws IOException {
        if (debug) {
            log.info("showFullScreenPage::callBackURLOK = " + callBackURLOK);
            log.info("showFullScreenPage::callBackURLError = " + callBackURLError);
            log.info("showFullScreenPage::tancarFinestraURL = " + tancarFinestraURL);
            log.info("showFullScreenPage::redireccionURL = " + redirectUrl);
        }

        final boolean showInNewWindow = false;

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html>" + "\n" + "<head>" + "\n" + "<script type=\"text/javascript\">" + "\n");

        if (showInNewWindow) {
            out.println(" var win;" + "\n" + "    win = window.open('" + redirectUrl + "', '_blank', '');" + "\n"
                    + "    var timer = setInterval(function() {" + "\n" + "        if (win.closed) {" + "\n"
                    + "          clearInterval(timer);" + "\n" + "          document.location.href = '" + callBackURLOK
                    + "';" + "\n" + "        }" + "\n" + "      }, 500);" + "\n" + " };" + "\n");
        } else {
            out.println("    var insideIframe = window.top !== window.self;" + "\n" + "    if(insideIframe){" + "\n"
                    + "       window.top.location.href='" + redirectUrl + "';\n" + "    } else {" + "\n"
                    + "       document.location.href = '" + redirectUrl + "';" + "\n" + "    };" + "\n");
        }

        out.println("</script>" + "\n" + "</head><body>" + "\n" + "<br/><center>" + "\n" + "<h1>"
                + getTraduccio("introduircontrasenya", locale) + "</h1><br/>" + "\n" + "<img src=\""
                + relativePluginRequestPath + "/" + WEBRESOURCE + "/img/ajax-loader2.gif\" />" + "\n"
                + "<br/><input id=\"clickMe\" type=\"button\" value=\"clickme\" onclick=\"xyz();\" />" + "\n"
                + "</center>" + "\n" + "</body>" + "\n" + "</html>");

        out.flush();
    }

    // ----------------------------------------------------------------------------
    // ---------------------------------------------------------------------------
    // ------------------ FIRMAR POST ERROR (SIGNERRORPAGE) ---------------------
    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------

    protected static final String SIGN_ERROR_PAGE = "signErrorPage";

    protected void signErrorPage(String absolutePluginRequestPath, String relativePluginRequestPath,
            HttpServletRequest request, HttpServletResponse response, SignaturesSetWeb signaturesSet,
            int signatureIndex, String query, Locale locale) {

        //final Map<String, String[]> params = request.getParameterMap();
        log.error("\n\n\n\n");

        // -----------------------------------------------------------------------

        String signaturesSetID = signaturesSet.getSignaturesSetID();

        FIReSignaturesSet fireSignaturesSet = transactions.get(signaturesSetID);
        final String transactionId = fireSignaturesSet.getFireTransaccionId();
        final String subjectId = fireSignaturesSet.getSubjectId();

        String msg = null;
        try {
            FireClient fireClient = instantiateFireClient();

            log.info("signErrorPage():: cridant a fireClient.recoverErrorResult(" + transactionId + "  ,  " + subjectId
                    + ") ...");

            TransactionResult transactionResult = fireClient.recoverErrorResult(transactionId, subjectId);

            String msgError = transactionResult.getErrorMessage();

            log.error("signErrorPage():: transactionResult.getErrorCode() => " + transactionResult.getErrorCode());
            log.error("signErrorPage():: transactionResult.getErrorMessage() => " + msgError);
            log.error(
                    "signErrorPage():: transactionResult.getProviderName() => " + transactionResult.getProviderName());
            log.error("signErrorPage():: transactionResult.getResultType() => " + transactionResult.getResultType());
            log.error("signErrorPage():: transactionResult.getState() => " + transactionResult.getState());

            if (msgError != null && msgError.trim().length() != 0) {

                if ("ca".equalsIgnoreCase(fireSignaturesSet.getLanguageUI())) {
                    msg = getTraduccio("codierror." + transactionResult.getErrorCode(), locale);
                }

                if (msg == null) {
                    msg = msgError;
                }
                //error.detail=(Codi Error: {0} - Proveïdor: {1} - Tipus Resultat: {2})
                msg = msg + getTraduccio("error.detail", locale, transactionResult.getErrorCode(),
                        transactionResult.getProviderName(), transactionResult.getResultType());
            }

            log.error("\n\n\n\n");

        } catch (Throwable th) {
            log.error("S'ha produit un error intentant atacar al component de FIRe per saber l'error que s'ha produit: "
                    + th.getMessage(), th);
        }

        // ------------------------------------------------------------------------

        if (msg == null) {
            msg = getTraduccio("error.desconegut", locale);
        }

        finishWithError(response, signaturesSet, msg, null);
    }

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ------------------ FIRMAR POST OK ------------------------------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    private static final String FIRMAR_POST_PAGE = "firmarpost";

    protected void firmarPostOk(HttpServletRequest request, HttpServletResponse response,
            SignaturesSetWeb signaturesSet, int signatureIndex2, Locale locale) {

        String signaturesSetID = signaturesSet.getSignaturesSetID();

        FIReSignaturesSet fireSignaturesSet = transactions.get(signaturesSetID);
        final String transactionId = fireSignaturesSet.getFireTransaccionId();
        final String subjectId = fireSignaturesSet.getSubjectId();

        final boolean debug = isDebug();

        if (debug) {
            log.info("");
            log.info("firmarPostOk:: signatureIndex = " + signatureIndex2);
            log.info("");
        }

        FileInfoSignature[] fileInfoArray = signaturesSet.getFileInfoSignatureArray();

        try {

            FireClient fireClient = instantiateFireClient();

            if (debug) {
                log.info("firmarPostOk:: cridant a fireClient.recoverBatchResult(" + transactionId + "  ,  " + subjectId
                        + ") ...");
            }
            BatchResult batchResult = fireClient.recoverBatchResult(transactionId, subjectId);

            // boolean isEmpty = batchResult.isEmpty();
            boolean isError = batchResult.isError(); // true si alguna de les firmes
                                                     // ha fallat
            if (isError) {
                log.error("firmarPostOk:: La transacció " + transactionId + " indica que algunes firmes"
                        + " han fallat (signaturesSetID = " + signaturesSetID + ")");
            }

            for (int i = 0; i < fileInfoArray.length; i++) {

                FileInfoSignature fileInfo = fileInfoArray[i];

                final String docId = String.valueOf(i);

                SignBatchResult sbr = batchResult.get(docId);

                if (!sbr.isSigned()) {
                    // FIRMA ERROR
                    String msgFire = sbr.getErrotType();
                    log.error("firmarPostOk[" + i + "]::(  sbr.isSigned() == false) "
                            + "S'ha produit un error durant la firma: " + msgFire);

                    StatusSignature ss = fileInfo.getStatusSignature();
                    ss.setErrorMsg(getTraduccio("error.firmantdocument", locale, fileInfo.getName(), msgFire));
                    ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);

                    continue;
                }

                // FIRMA OK

                // Recuperam fitxer firmat
                byte[] signedData;
                try {
                    TransactionResult result;
                    result = fireClient.recoverBatchSign(transactionId, subjectId, docId);

                    if (result.getState() == TransactionResult.STATE_ERROR) {
                        log.error("La cridada fireClient.recoverBatchSign(" + transactionId + ", " + subjectId + ", "
                                + docId + ") ha retornat un estat"
                                + " d'error en la transacció realitzada(result.getState() == TransactionResult.STATE_ERROR)");
                        String error = result.getErrorMessage() + "(" + result.getErrorCode() + ")";
                        throw new Exception(error);
                    }

                    signedData = result.getResult();

                } catch (Exception e) {

                    log.error("S'ha produit un error durant la descàrrega de la firma: " + e.getMessage(), e);
                    StatusSignature ss = fileInfo.getStatusSignature();
                    ss.setErrorMsg(
                            getTraduccio("error.downloadsignedfile", locale, fileInfo.getName(), e.getMessage()));
                    ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);
                    ss.setErrorException(e);
                    continue;
                }

                // ========= CAS OK

                // ***************** SELLO DE TIEMPO PER CADES&SMIME ****************

                if (fileInfo.getTimeStampGenerator() != null) {

                    // Aplicar Segellat de Temps si és SMIME o CADES
                    if (FileInfoSignature.SIGN_TYPE_CADES.equals(fileInfo.getSignType())
                            || FileInfoSignature.SIGN_TYPE_SMIME.equals(fileInfo.getSignType())) {

                        Properties[] allProp = timeStampCache.get(signaturesSetID);

                        try {

                            if (debug) {
                                log.info("firmarPostOk:: fisig.getSignType() => " + fileInfo.getSignType());
                                log.info("firmarPostOk:: allProp => " + Arrays.toString(allProp));
                            }

                            if (allProp == null) {
                                throw new Exception("No es troba informació per realitzar el segellat de Temps"
                                        + "(allProp no hauria de ser null !!!!)");
                            }

                            TsaParams tsaParams = new TsaParams(allProp[i]);

                            CMSTimestamper cmsTS = new CMSTimestamper(tsaParams);
                            signedData = cmsTS.addTimestamp(signedData, tsaParams.getTsaHashAlgorithm(),
                                    new GregorianCalendar());
                        } catch (final Exception e) {
                            // XYZ ZZZ Traduccio
                            String msg = "Error Aplicant Segellat de Temps a una firma SMIME o CADES: "
                                    + e.getMessage();
                            log.error(msg, e);

                            StatusSignature ss = fileInfo.getStatusSignature();
                            ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);
                            ss.setErrorMsg(msg);
                            ss.setErrorException(e);

                            finishWithError(response, signaturesSet, msg, e);

                            continue;

                        }
                    }
                }

                // ************** FIN SELLO DE TIEMPO ****************

                // ********** INICI SMIME

                File firmat = File.createTempFile("FireWebPlugin", "signedfile");

                if (FileInfoSignature.SIGN_TYPE_SMIME.equals(fileInfo.getSignType())) {
                    try {
                        // SMIME

                        String mimeType = fileInfo.getMimeType();
                        if (mimeType == null || mimeType.trim().length() == 0) {
                            mimeType = "application/octet-stream";
                        }

                        FileInputStream fis = new FileInputStream(fileInfo.getFileToSign());

                        SMIMEInputStream smis = new SMIMEInputStream(signedData, fis, mimeType);

                        FileOutputStream baos = new FileOutputStream(firmat);
                        FileUtils.copy(smis, baos);

                        smis.close();
                        fis.close();
                        baos.close();
                    } catch (final Exception e) {

                        // XYZ ZZZ traduir
                        String msg = "Error durant el postProces d'una Firma SMIME: " + e.getMessage();
                        log.error("firmarPostOk::" + msg, e);

                        StatusSignature ss = fileInfo.getStatusSignature();
                        ss.setStatus(StatusSignature.STATUS_FINAL_ERROR);
                        ss.setErrorMsg(msg);
                        ss.setErrorException(e);
                        continue;

                    }

                } else {

                    FileOutputStream fos = new FileOutputStream(firmat);
                    fos.write(signedData);
                    fos.flush();
                    fos.close();
                }

                // ********** FINAL SMIME

                StatusSignature ss = fileInfo.getStatusSignature();
                ss.setSignedData(firmat);
                ss.setStatus(StatusSignature.STATUS_FINAL_OK);

            } // FINAL FOR

            signaturesSet.getStatusSignaturesSet().setStatus(StatusSignaturesSet.STATUS_FINAL_OK);

            final String url = signaturesSet.getUrlFinal();

            sendRedirect(response, url);

        } catch (Exception e) {

            // TODO XYZ ZZZ Traduir
            log.error(" Error desconegut recuperant firmes de la transacció " + transactionId + " : " + e.getMessage(),
                    e);

            StatusSignaturesSet sss = signaturesSet.getStatusSignaturesSet();
            sss.setStatus(StatusSignature.STATUS_FINAL_ERROR);
            sss.setErrorException(e);
            String msg = getTraduccio("error.procesdefirma", locale, e.getMessage());
            sss.setErrorMsg(msg);

            finishWithError(response, signaturesSet, msg, e);

        }

    }

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // -------------------------- OPERACIONS API CLAVEFIRMA -------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------

    protected FireClient instantiateFireClient() throws Exception {
        final String appId = getAppID();
        final Properties prop = getFireProperties();

        return new FireClient(appId, prop);
    }

    protected Properties getFireProperties() throws Exception {
        Properties prop = new Properties();

        final String fireUrl = getPropertyRequired(FIRE_BASE_PROPERTIES + "fireUrl");

        // prop.setProperty("appId", appId);

        prop.setProperty("fireUrl", fireUrl); // http://10.215.216.175:6060/fire-signature/fireService

        String[] keys = new String[] { "javax.net.ssl.keyStore", "javax.net.ssl.keyStorePassword",
                "javax.net.ssl.keyStoreType", "javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword",
                "javax.net.ssl.trustStoreType" };

        for (String key : keys) {
            String value = getProperty(FIRE_BASE_PROPERTIES + key);

            if (value != null && value.trim().length() != 0) {
                prop.setProperty(key, value);
            }
        }
        return prop;
    }

    // ------------------------------------------------------------------
    // -------------------------------------------------------------------
    // -------------------------- CONFIGURACIO PLUGIN --------------------
    // -------------------------------------------------------------------
    // -------------------------------------------------------------------

    @Override
    public String getResourceBundleName() {
        return "fire";
    }

    @Override
    protected String getSimpleName() {
        return "FirePlugin";
    }

    @Override
    public List<String> getSupportedBarCodeTypes() {
        return null;
    }

    @Override
    public boolean acceptExternalTimeStampGenerator(String signType) {
        // A traves de propietats de miniapplet
        if (FileInfoSignature.SIGN_TYPE_PADES.equals(signType) || FileInfoSignature.SIGN_TYPE_CADES.equals(signType)
                || FileInfoSignature.SIGN_TYPE_SMIME.equals(signType)) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean providesTimeStampGenerator(String signType) {
        return false;
    }

    @Override
    public boolean acceptExternalRubricGenerator() {
        // No acceptam firmes amb taula de Firmes
        // Si l'usuari no està registrat (no està donat d'alta a Cl@veFirma),
        // llavors no podem obtenir el certificat, necessari per dibuixar
        // la taula de firmes. 
        // 
        return false;
        //return true;
    }

    @Override
    public boolean providesRubricGenerator() {
        return false;
    }

    @Override
    public boolean acceptExternalSecureVerificationCodeStamper() {
        return false;
    }

    @Override
    public boolean providesSecureVerificationCodeStamper() {
        return false;
    }

    @Override
    public String[] getSupportedSignatureTypes() {

        return new String[] { FileInfoSignature.SIGN_TYPE_PADES, FileInfoSignature.SIGN_TYPE_XADES,
                FileInfoSignature.SIGN_TYPE_CADES, FileInfoSignature.SIGN_TYPE_SMIME,
                // TODO XYZ ZZZ
                // • FacturaE: Formato de firma para facturas electrónicas.
                //   FileInfoSignature.SIGN_TYPE_FACTURAE
                // • CAdES-ASiC-S: Formato de firma avanzada ASiC de tipo CAdES.
                // FileInfoSignature.SIGN_TYPE_CADES_ASIC_S
                // • XAdES-ASiC-S: Formato de firma avanzada ASiC de tipo XAdES.
                // FileInfoSignature.SIGN_TYPE_CADES_ASIC_S
                // • NONE: Firma PKCS#1.
                // FileInfoSignature.SIGN_TYPE_PKCS1

        };
    }
    
    
    @Override
    public int[] getSupportedSignatureModes(String signType) {

        if (signType == null || signType.trim().length() == 0) {
            log.error("S'ha cridat a getSupportedSignatureModes amb un tipus de firma null o buit");
            return new int[0];
        }

        switch (signType) {
            case FileInfoSignature.SIGN_TYPE_PADES:
                return new int[] { FileInfoSignature.SIGN_MODE_ATTACHED_ENVELOPED };

            case FileInfoSignature.SIGN_TYPE_CADES:
                return new int[] { FileInfoSignature.SIGN_MODE_ATTACHED_ENVELOPING,
                        FileInfoSignature.SIGN_MODE_DETACHED };

            case FileInfoSignature.SIGN_TYPE_XADES:
                return new int[] { FileInfoSignature.SIGN_MODE_ATTACHED_ENVELOPING,
                        FileInfoSignature.SIGN_MODE_INTERNALLY_DETACHED };

            default:
                log.error("S'ha cridat a getSupportedSignatureModes amb un amb un tipus de firma desconegut: ]"
                        + signType + "[");
                return new int[0];
        }
    }
    
    

    @Override
    public String[] getSupportedSignatureAlgorithms(String signType) {

        if (FileInfoSignature.SIGN_TYPE_PADES.equals(signType) || FileInfoSignature.SIGN_TYPE_XADES.equals(signType)
                || FileInfoSignature.SIGN_TYPE_CADES.equals(signType)) {
            return new String[] { FileInfoSignature.SIGN_ALGORITHM_SHA1, FileInfoSignature.SIGN_ALGORITHM_SHA256,
                    FileInfoSignature.SIGN_ALGORITHM_SHA384, FileInfoSignature.SIGN_ALGORITHM_SHA512 };
        }

        if (FileInfoSignature.SIGN_TYPE_SMIME.equals(signType)) {
            return new String[] { FileInfoSignature.SIGN_ALGORITHM_SHA1 };
        }

        return null;
    }

    // ------------------------------------------------------------------
    // -------------------------------------------------------------------
    // ---------- U T I L I T A T S C L A V E - F I R M A ----------------
    // -------------------------------------------------------------------
    // -------------------------------------------------------------------

    protected HttpSignProcessConstants.SignatureFormat convertSignType(String signType) {
        if (FileInfoSignature.SIGN_TYPE_PADES.equals(signType)) {
            return HttpSignProcessConstants.SignatureFormat.PADES;
        } else if (FileInfoSignature.SIGN_TYPE_XADES.equals(signType)) {
            return HttpSignProcessConstants.SignatureFormat.XADES;
        } else if (FileInfoSignature.SIGN_TYPE_CADES.equals(signType)
                || FileInfoSignature.SIGN_TYPE_SMIME.equals(signType)) {
            return HttpSignProcessConstants.SignatureFormat.CADES;
        } else {
            log.error("Tipus de Firma no suportada fent conversió a FIRe: " + signType, new Exception());
            return null;
        }
    }

    protected HttpSignProcessConstants.SignatureAlgorithm convertSignAlgorithm(String signAlgorithm) {

        if (FileInfoSignature.SIGN_ALGORITHM_SHA1.equals(signAlgorithm)) {
            return HttpSignProcessConstants.SignatureAlgorithm.SHA1WITHRSA;
        } else if (FileInfoSignature.SIGN_ALGORITHM_SHA256.equals(signAlgorithm)) {
            return HttpSignProcessConstants.SignatureAlgorithm.SHA256WITHRSA;
        } else if (FileInfoSignature.SIGN_ALGORITHM_SHA384.equals(signAlgorithm)) {
            return HttpSignProcessConstants.SignatureAlgorithm.SHA384WITHRSA;
        } else if (FileInfoSignature.SIGN_ALGORITHM_SHA512.equals(signAlgorithm)) {
            return HttpSignProcessConstants.SignatureAlgorithm.SHA512WITHRSA;
        } else {
            // XYZ ZZZ Llançar error o un null ?????
            log.error("Algorisme de Firma no suportat fent conversió a FIRe: " + signAlgorithm, new Exception());
            return null;
        }
    }

    /**
     * Obté l'usuari ClaveFirma a partir de l'username o NIF (administrationID)
     * 
     * @param username
     *          (opcional)
     * @param administrationID
     *          És el NIF (obligatori)
     */
    public String getClaveFirmaUser(String username, String administrationID) throws Exception {

        boolean debug = isDebug();

        if (debug) {
            log.info("getClaveFirmaUser => U: " + username + " | NIF: " + administrationID);
        }

        // Primer provam el mapping
        String mappingPath = getProperty(PROPERTY_MAPPING_USERS_PATH);

        if (mappingPath != null) {
            Properties props = readPropertiesFromFile(new File(mappingPath));
            if (props != null && username != null) {
                String newUser = props.getProperty(username);
                if (newUser != null) {
                    return newUser;
                }
            }
        }

        // Si el mapping no funciona llavors provam el PATTERN
        // {0} == username || {1} == administrationID (NIF)

        String usersPattern = getProperty(PROPERTY_USERS_PATTERN);

        String newUser = null;

        if (usersPattern != null) {
            newUser = MessageFormat.format(usersPattern, username, administrationID);
        }

        if (newUser == null) {
            if (username == null) {
                newUser = administrationID;
            } else {
                newUser = username;
            }
        }

        if (debug) {
            log.info("getClaveFirmaUser:: RETURN " + newUser);
        }

        return newUser;

    }

    @Override
    public int getActiveTransactions() throws Exception {
        return internalGetActiveTransactions();
    }

    @Override
    public void resetAndClean(HttpServletRequest request) throws Exception {
        internalResetAndClean(request);

        transactions.clear();
        generateCertificateTransactions.clear();
        timeStampCache.clear();
    }

}
