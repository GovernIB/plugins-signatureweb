package org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies;

/**
 * 
 * @author anadal
 *
 */
public class EvidenciaClaveAutenticacion implements IEvidencia {
    
    public static final String CODI = "CLAVE_AUTENTICACION";

    @Override
    public int getPunts() {
        return 50;
    }

    @Override
    public String getTitol() {        
        return "Validacio emprant Cl@ve Autenticacion";
    }

    @Override
    public String getSubtitol() {        
        return "A partir l'autenticació a través de Cl@ve Autenticacion";
    }

    @Override
    public String getCodi() {
        return CODI;
    }

}
