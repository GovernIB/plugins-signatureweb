
 - Crear fitxer "test.properties" amb el hostname del JBoss i les dades dels signants.
 - Crear fitxer "plugin.properties" amb les propietats des plugins.
 - Crear fitxer "timestamp.properties" amb les propietats del plugin de timestamp.
 - Fixar la propietat de sistema "org.fundaciobit.plugins.signatureweb.path" dins el JBoss que apunti al directori
on s'ubica els fitxers plugin.properties i timestamp.properties
 - Executar mvn verify per compilar
 - Desplegar signaturewebtester.war dins el JBoss
 - Executar tests amb mvn -PtestsIntegracio verify