package org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies;

/**
 * 
 * @author anadal
 *
 */
public class EvidenciaIP implements IEvidencia {
    
    public static final String CODI = "IP_ORDINADOR";

    @Override
    public int getPunts() {
        return 20;
    }

    @Override
    public String getTitol() {        
        return "IP Ordinador";
    }

    @Override
    public String getSubtitol() {        
        return "Informació sobre la IP amb la que s'està connectant";
    }

    @Override
    public String getCodi() {
        return CODI;
    }

}
