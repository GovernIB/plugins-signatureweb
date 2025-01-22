package org.fundaciobit.pluginsib.signatureweb.firmanocriptografica;

/**
 * 
 * @author anadal
 *
 */
public class StatusFirmaNoCriptografica {

    // XYZ ZZZ TODO FALTA ALGUNA DADA ???
    protected final Long evidenciaID;

    protected final String urlEvidencies;

    public StatusFirmaNoCriptografica(Long evidenciaID, String urlEvidencies) {
        super();
        this.evidenciaID = evidenciaID;
        this.urlEvidencies = urlEvidencies;

    }

    public Long getEvidenciaID() {
        return evidenciaID;
    }

    public String getUrlEvidencies() {
        return urlEvidencies;
    }

}
