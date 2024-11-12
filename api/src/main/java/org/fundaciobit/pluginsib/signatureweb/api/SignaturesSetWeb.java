package org.fundaciobit.pluginsib.signatureweb.api;

import java.util.Date;

import org.fundaciobit.pluginsib.signature.api.CommonInfoSignature;
import org.fundaciobit.pluginsib.signature.api.FileInfoSignature;
import org.fundaciobit.pluginsib.signature.api.SignaturesSet;

/**
 * 
 * @author anadal
 *
 */
public class SignaturesSetWeb extends SignaturesSet {

    public String urlFinal;

    /** Data en que les sol·lictuds de firma caduquen */
    protected Date expiryDate;

    /**
     * 
     */
    public SignaturesSetWeb() {
    }

    /**
     * @param signaturesSetID
     * @param commonInfoSignature
     * @param fileInfoSignatureArray
     */
    public SignaturesSetWeb(String signaturesSetID, Date expiryDate, CommonInfoSignature commonInfoSignature,
            FileInfoSignature[] fileInfoSignatureArray, String urlFinal) {
        super(signaturesSetID, commonInfoSignature, fileInfoSignatureArray);
        this.urlFinal = urlFinal;
        this.expiryDate = expiryDate;
    }

    public String getUrlFinal() {
        return urlFinal;
    }

    public void setUrlFinal(String urlFinal) {
        this.urlFinal = urlFinal;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

}
