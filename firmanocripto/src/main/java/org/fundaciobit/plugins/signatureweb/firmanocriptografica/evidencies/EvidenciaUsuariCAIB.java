package org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies;

/**
 * 
 * @author anadal
 *
 */
public class EvidenciaUsuariCAIB implements IEvidencia {
    
    public static final String CODI = "USUARI_CAIB";

    @Override
    public int getPunts() {
        return 25;
    }

    @Override
    public String getTitol() {        
        return "Validacio de les Dades en possessió de la CAIB ";
    }

    @Override
    public String getSubtitol() {        
        return "A partir l'autenticació a l'entorn de la CAIB emprant usuari i contrasenya";
    }

    @Override
    public String getCodi() {
        return CODI;
    }

}
