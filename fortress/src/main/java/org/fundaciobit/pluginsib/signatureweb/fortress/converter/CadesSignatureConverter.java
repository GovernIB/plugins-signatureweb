package org.fundaciobit.pluginsib.signatureweb.fortress.converter;

import org.fundaciobit.pluginsib.signature.api.FileInfoSignature;

import com.viafirma.fortress.sdk.model.signature.SignatureConfiguration;
import com.viafirma.fortress.sdk.model.signature.SignatureConfiguration.Packaging;

/**
 * 
 * @author anadal
 *
 */
public class CadesSignatureConverter extends AbstractSignatureConverter {

    @Override
    protected void applyConfiguration(SignatureConfiguration config, FileInfoSignature fis) {
        SignatureConfiguration.SignatureType type = fis.getTimeStampGenerator() == null
                ? SignatureConfiguration.SignatureType.CADES_B
                : SignatureConfiguration.SignatureType.CADES_T;
        config.setSignatureType(type);

        Packaging packaging;
        switch (fis.getSignMode()) {
            case FileInfoSignature.SIGN_MODE_ATTACHED_ENVELOPING:
                packaging = SignatureConfiguration.Packaging.ENVELOPING;
            break;

            case FileInfoSignature.SIGN_MODE_DETACHED:
                packaging = SignatureConfiguration.Packaging.DETACHED;
            break;

            default:
                log.error("Mode de Firma desconegut ]" + fis.getSignMode() + "[ dins de CadesSignatureConverter",
                        new Exception());
                packaging = null;
            break;
        }
        config.setPackaging(packaging);
    }
}
