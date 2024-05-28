
COM EXECUTAR EL TEST
====================


(1) Dins de $JBOSS\standalone crear un directori deploy_signaturewebtester.

(2) Dins del directori $JBOSS\standalone\deploy_signaturewebtester copiar els
fitxers plugin.properties.sample i timestamp.properties.sample
que es troben dins del directori pluginsib-signatureweb-4.0\signaturewebtester, 
i renombrar-los a plugin.properties, timestamp.properties i test.properties
respectivament.

(3) Configurar els fitxers del punt anterior amb les dades necessàries.

 - Fitxer "plugin.properties" amb les propietats des plugins.
 - Fitxer "timestamp.properties" amb les propietats del plugin de timestamp.

(4) Dins de $JBOSS\bin crear un fitxer signaturewebtester.bat amb el següent contingut:

standalone.bat -b 0.0.0.0 -c standalone_signaturewebtester.xml

(4) Dins de $JBOSS\standalone\configuration crear un fitxer standalone_signaturewebtester.xml,
    còpia de d'un standalone.xml net.
    
    (4.1) Afegir el següent contingut a l'apartat de properties:
    {{ NOTA: Substituir el valor $JBOSS7 pel directori on es troba el JBoss }}
    
    <system-properties>
        <property name="org.fundaciobit.pluginsib.signatureweb.path" value="$JBOSS7/standalone/deploy_signaturewebtester/"/>
    </system-properties>

   (4.2) Cercar l'entrada "<subsystem xmlns="urn:jboss:domain:deployment-scanner:2.0">" i en el
        valor del fill substituir path="deployments" per path="deploy_signaturewebtester"

(5) Copiar fitxer "test.properties.sample" a "test.properties" i configurar el hostname del JBoss i les dades dels signants.


(6) Desplegar signaturewebtester.war dins el JBoss. Obrir navegador a localhost:8080/signaturewebtester


===== JA NO FUNCIONA SELENIUM !!!!!  ======
(6) Executar mvn verify per compilar
 - Desplegar signaturewebtester.war dins el JBoss
 - Executar tests amb mvn -PtestsIntegracio verify
 
 
 
 
 