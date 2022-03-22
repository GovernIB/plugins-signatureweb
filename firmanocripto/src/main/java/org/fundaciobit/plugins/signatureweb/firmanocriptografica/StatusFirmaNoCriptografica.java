package org.fundaciobit.plugins.signatureweb.firmanocriptografica;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fundaciobit.plugins.signatureweb.firmanocriptografica.evidencies.IEvidencia;

/**
 * 
 * @author anadal
 *
 */
public class StatusFirmaNoCriptografica {

    protected final Map<String, Integer> evidenciesCodePuntsMap = new HashMap<String, Integer>();

    public StatusFirmaNoCriptografica(List<IEvidencia> evidencies) {
        super();
        for (IEvidencia iEvidencia : evidencies) {
            this.evidenciesCodePuntsMap.put(iEvidencia.getCodi(), null);
        }
    }

    public Map<String, Integer> getEvidenciesPuntsMap() {
        return evidenciesCodePuntsMap;
    }

}
