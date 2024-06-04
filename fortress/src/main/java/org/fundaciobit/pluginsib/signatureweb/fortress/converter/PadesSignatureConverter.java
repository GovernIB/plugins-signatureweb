package org.fundaciobit.pluginsib.signatureweb.fortress.converter;

import org.fundaciobit.pluginsib.signature.api.FileInfoSignature;

import com.viafirma.fortress.sdk.model.signature.PadesConfiguration;
import com.viafirma.fortress.sdk.model.signature.SignatureConfiguration;

public class PadesSignatureConverter extends AbstractSignatureConverter {

    @Override
    protected void applyConfiguration(SignatureConfiguration config, FileInfoSignature fis) {
        config.setPadesConfiguration(new PadesConfiguration());
        SignatureConfiguration.SignatureType type = fis.getTimeStampGenerator() == null
                ? SignatureConfiguration.SignatureType.PADES_B
                : SignatureConfiguration.SignatureType.PADES_T;
        config.setSignatureType(type);
        config.setPackaging(SignatureConfiguration.Packaging.ENVELOPED);
    }
}
