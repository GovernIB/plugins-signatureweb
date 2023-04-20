package org.fundaciobit.pluginsib.signatureweb.fortress.converter;

import com.viafirma.fortress.sdk.model.signature.SignatureConfiguration;
import com.viafirma.fortress.sdk.model.signature.XadesConfiguration;
import com.viafirma.fortress.sdk.model.signature.SignatureConfiguration.Packaging;

import org.fundaciobit.plugins.signature.api.FileInfoSignature;

/**
 * 
 * @author anadal
 *
 */
public class XadesSignatureConverter extends AbstractSignatureConverter {

    @Override
    protected void applyConfiguration(SignatureConfiguration config, FileInfoSignature fis) {

        XadesConfiguration xadesConfiguration = new XadesConfiguration();
        config.setXadesConfiguration(xadesConfiguration);

        SignatureConfiguration.SignatureType type = fis.getTimeStampGenerator() == null
                ? SignatureConfiguration.SignatureType.XADES_B
                : SignatureConfiguration.SignatureType.XADES_T;
        config.setSignatureType(type);
        /*
        config.setPackaging(fis.getSignMode() == FileInfoSignature.SIGN_MODE_IMPLICIT
                ? SignatureConfiguration.Packaging.ENVELOPING
                : SignatureConfiguration.Packaging.DETACHED);
                */

        Packaging packaging;
        switch (fis.getSignMode()) {
            case FileInfoSignature.SIGN_MODE_ATTACHED_ENVELOPING:
                packaging = SignatureConfiguration.Packaging.ENVELOPING;
            break;

            case FileInfoSignature.SIGN_MODE_ATTACHED_ENVELOPED:
                packaging = SignatureConfiguration.Packaging.ENVELOPED;
            break;

            case FileInfoSignature.SIGN_MODE_DETACHED:
                packaging = SignatureConfiguration.Packaging.DETACHED;
            break;

            default:
                log.error("Mode de Firma desconegut o no suportat ]" + fis.getSignMode() + "[ dins de XadesSignatureConverter",
                        new Exception());
                packaging = null;
            break;
        }
        config.setPackaging(packaging);
    }
}
