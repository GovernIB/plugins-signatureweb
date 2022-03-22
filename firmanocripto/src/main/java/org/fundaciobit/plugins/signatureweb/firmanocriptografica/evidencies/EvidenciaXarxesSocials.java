package org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies;

/**
 * 
 * @author anadal
 *
 */
public class EvidenciaXarxesSocials implements IEvidencia {
    
    public static final String CODI = "XARXES_SOCIALS";

    @Override
    public int getPunts() {
        return 50;
    }

    @Override
    public String getTitol() {        
        return "Validacio emprant comptes a Xarxes Socials";
    }

    @Override
    public String getSubtitol() {        
        return "Autenticació a Xarxes Socials (google, facebook, twiter, ...) per obtenir informació de la persona";
    }

    @Override
    public String getCodi() {
        return CODI;
    }

}
