package org.fundaciobit.pluginsib.signatureweb.miniappletinserversia;

import java.util.Properties;

import org.fundaciobit.pluginsib.signatureserver.miniappletutils.AbstractTriFaseSigner;

/**
 * 
 * @author anadal
 *
 */
public class MiniAppletInServerSIASigner extends AbstractTriFaseSigner {

  final String algorithm;

  final Properties params;

  public MiniAppletInServerSIASigner(final String algorithm, final Properties params) {
    super();
    this.algorithm = algorithm;
    this.params = params;
  }

  @Override
  public byte[] step2_signHash(final String algorithm, final byte[] hashDocumentoParam)
      throws Exception {
    // NO FER RES
    throw new Exception("La firma es genera cridant al servidor de SIA");

  }

  public String getAlgorithm() {
    return algorithm;
  }

  public Properties getParams() {
    return params;
  }

  /**
   * Evita que la carrega es faci amb classloaders pensats per funcionar en applets.
   * Veure #541.
   */
  @Override
  public Class<?> loadClass(String name) throws Exception {
    return Class.forName(name);
  }
}