package org.fundaciobit.pluginsib.signatureweb.fortress.converter;

import com.viafirma.fortress.sdk.model.signature.SignatureConfiguration;

import java.io.IOException;

import org.fundaciobit.pluginsib.signature.api.FileInfoSignature;

public interface SignatureConverter {

    SignatureConfiguration convert(FileInfoSignature fileInfoSignature) throws IOException;

}
