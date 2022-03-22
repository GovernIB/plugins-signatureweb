package org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies;

/**
 * 
 * @author anadal
 *
 */
public class EvidenciaPadro implements IEvidencia {
    
    public static final String CODI = "PADRO";

    @Override
    public int getPunts() {
        return 50;
    }

    @Override
    public String getTitol() {        
        return "Dades del Padro";
    }

    @Override
    public String getSubtitol() {        
        return "A partir del Codi postal, Nom carrer, Població, Municipi, Provincia i Comunitat Autònoma.";
    }

    @Override
    public String getCodi() {
        return CODI;
    }

}
