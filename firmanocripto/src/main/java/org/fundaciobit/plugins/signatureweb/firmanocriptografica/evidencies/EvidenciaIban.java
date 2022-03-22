package org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies;

/**
 * 
 * @author anadal
 *
 */
public class EvidenciaIban implements IEvidencia {
    
    public static final String CODI = "IBAN";

    @Override
    public int getPunts() {
        return 15;
    }

    @Override
    public String getTitol() {        
        return "IBAN Bancari";
    }

    @Override
    public String getSubtitol() {        
        return "Informació sobre la persona a partir d'una compte bancaria (IBAN)";
    }

    @Override
    public String getCodi() {
        return CODI;
    }

}
