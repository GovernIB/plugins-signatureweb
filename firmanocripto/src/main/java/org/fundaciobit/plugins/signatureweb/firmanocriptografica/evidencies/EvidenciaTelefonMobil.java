package org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies;

/**
 * 
 * @author anadal
 *
 */
public class EvidenciaTelefonMobil implements IEvidencia {
    
    public static final String CODI = "SMS_CODI_MOBIL";

    @Override
    public String getCodi() {        
        return CODI;
    }

    @Override
    public int getPunts() {
        return 50;
    }

    @Override
    public String getTitol() {
        return "Enviament de SMS al Mòbil";
    }

    @Override
    public String getSubtitol() {
        return "Validació del seu numero de telefon mòbil enviant un SMS al seu dispositiu mòbil";
    }

}
