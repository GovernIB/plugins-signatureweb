package org.fundaciobit.plugins.signatureweb.tester;

import org.fundaciobit.plugins.signatureweb.api.ISignatureWebPlugin;
import org.fundaciobit.plugins.signatureweb.api.SignaturesSetWeb;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.Serializable;
import java.util.Collections;
import java.util.logging.Logger;

@ConversationScoped
public class SignTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(SignTransaction.class.getName());

    @Inject
    Conversation conversation;

    private ISignatureWebPlugin plugin;
    private String urlBaseRelativa;
    private String urlBaseAbsoluta;

    public void start(SignaturesSetWeb signaturesSetWeb, ISignatureWebPlugin plugin, HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.plugin = plugin;
        conversation.begin(signaturesSetWeb.getSignaturesSetID());
        LOG.info("begin " + conversation.getId());

        urlBaseRelativa = request.getContextPath() + "/plugin/" + conversation.getId() + "/-1";
        urlBaseAbsoluta = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + urlBaseRelativa;

        LOG.info("URL Relativa: " + urlBaseRelativa);
        LOG.info("URL Absoluta: " + urlBaseAbsoluta);

        String filterError = plugin.filter(request, signaturesSetWeb, Collections.emptyMap());
        if (filterError != null) {
            response.sendError(500, filterError);
        }

        String returnUrl = plugin.signDocuments(request, urlBaseAbsoluta, urlBaseRelativa, signaturesSetWeb, Collections.emptyMap());

        response.sendRedirect(returnUrl);
    }

    public void request(HttpServletRequest request, HttpServletResponse response) {
        boolean isPost = request.getMethod().equals("POST");
        LOG.info(conversation.getId() + ":" + request.getRequestURI() + ", post? " + isPost);

        String query = request.getRequestURI().substring(urlBaseRelativa.length() + 1);

        if (isPost) {
            plugin.requestPOST(urlBaseAbsoluta, urlBaseRelativa, query, conversation.getId(), -1, request, response);
        } else {
            plugin.requestGET(urlBaseAbsoluta, urlBaseRelativa, query, conversation.getId(), -1, request, response);
        }
    }

    public SignaturesSetWeb end(HttpServletRequest request) throws Exception {
        LOG.info("end " + conversation.getId());
        SignaturesSetWeb signaturesSet;
        try {
            signaturesSet = plugin.getSignaturesSet(conversation.getId());
            plugin.closeSignaturesSet(request, conversation.getId());
        } finally {
            conversation.end();
        }

        return signaturesSet;
    }

}
