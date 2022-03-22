package org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies;

/**
 * 
 * @author anadal
 *
 */
public class EvidenciaDniNomLlinatges implements IEvidencia {
    
    public static final String CODI = "NIF_NOM_LLINATGES";

    @Override
    public int getPunts() {
        return 50;
    }

    @Override
    public String getTitol() {        
        return "Validacio NIF, Nom i Llinatges";
    }

    @Override
    public String getSubtitol() {        
        return "A partir del numero de DNI, Data Caducitat, el nom i llinatges que apareixen al document d'identitat";
    }

    @Override
    public String getCodi() {
        return CODI;
    }

}
