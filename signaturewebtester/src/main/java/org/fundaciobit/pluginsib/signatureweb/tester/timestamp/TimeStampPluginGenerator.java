package org.fundaciobit.pluginsib.signatureweb.tester.timestamp;


import org.fundaciobit.plugins.timestamp.api.ITimeStampPlugin;
import org.fundaciobit.pluginsib.signature.api.ITimeStampGenerator;
import org.jboss.logging.Logger;

import java.util.Calendar;

public class TimeStampPluginGenerator implements ITimeStampGenerator {

  private final Logger log = Logger.getLogger(this.getClass());

  private final ITimeStampPlugin timeStampPlugin;

  public TimeStampPluginGenerator(ITimeStampPlugin timeStampPlugin) {
    this.timeStampPlugin = timeStampPlugin;
  }

  @Override
  public byte[] getTimeStamp(byte[] data, Calendar cal) throws Exception {
    log.info("getTimeStamp");
    return timeStampPlugin.getTimeStampDirect(data, cal);
  }

  @Override
  public String getTimeStampPolicyOID() {
    return timeStampPlugin.getTimeStampPolicyOID();
  }

  @Override
  public String getTimeStampHashAlgorithm() {
    return timeStampPlugin.getTimeStampHashAlgorithm();
  }

}