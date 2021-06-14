
 - Crear fitxer "test.properties" amb el hostname del JBoss i les dades dels signants.
 - Crear fitxer "plugin.properties" amb les propietats des plugins.
 - Fixar la propietat de sistema "org.fundaciobit.plugins.signatureweb.path" dins el JBoss que apunti al directori
on s'ubica el fitxer plugin.properties
 - Executar mvn verify per compilar
 - Desplegar signaturewebtester.war dins el JBoss
 - Executar tests amb mvn -PtestsIntegracio verify