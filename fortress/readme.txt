
Autor/s: Antoni Reus Darder
Creat:07/09/2020 09:36


1.- Requisits
	Els requisits són els mateixos que els descrits als manual corresponent per la instal·lació de Portafib 2, amb les següents particularitats:
    • El plugin està disponible a partir de la versió 2.0.12 de Portafib.
    • El plugin requreix la versió de Java 1.7 com a mínim per executar-se.
          Tot i que Portafib 2.0 requereix Java 1.7 per compilar, l'entorn d'execució pot estar sobre Java 1.6. Però en el cas que s'empri el plugin de Viafirma Fortress, aquest requereix Java 1.7 a l'entorn d'execució
    • Disposar d'una URL d'accés a Viafirma-Fortress, així com un identificador de client i el seu corresponent "secret".
2.- Compilació
	Per tal de compilar i incloure el plugin de firma web dins l'EAR de l'aplicació cal indicar a la comanda de compilació el profile "fortress", per exemple:
	mvn -DskipTests -Pws-portafib-v1 -Pws-portafirmas -Pfortress clean install
	
	Es pot verificar que s'ha compilat el plugin comprovant l'existència del fitxer:
	ear/target/portafib/lib/pluginsib-signatureweb-fortress-3.0.0.jar
	
3.- Afegir el certificat de firmaprofesional al magatzem de certificats Java
Atès que la connexió amb el sistema de Viafirma-Fortress es realitza amb una connexió SSL pot ser necessari afegir el certificat de servidor al magatzem de certificats de confiança de Java.
Si s'empra el magatzem per defecte "cacerts", es pot fer seguint les següents passes:
     3.1. Obtenir el certifcat "pem" del servidor. Es pot fer de diverses maneres:
         a) Accedint amb un navegador Firefox a la URL (p.e. https://pre.firmacloud.com/fortress/ ), i anat a "Informació del lloc web" (pitjant damunt el pany), i "Connexió segura" (pitjar la fletxa cap a la dreta), i "Més informació", i a la nova finestra pitjar el botó "Mostra Certificat". A la pàgina que es carrega anar a l'enllaç "Baixa: PEM (cert)" i guardar l'arxiu "firmacloud-com.pem" (Altres navegadors ofereixen possibilitats similars)
         b) Des d'una consola Linux, executant la comanda:
            ▪ openssl s_client -showcerts -connect pre.firmacloud.com:443 </dev/null 2>/dev/null|openssl x509 -outform PEM > firmacloud-com.pem
     3.2. Importar el certificat dins el magatzem. Per això cal definir la variable d'entorn JAVA_HOME, i executar el següent:
         a) Windows: 
keytool -importcert -noprompt -file firmacloud-com.pem -alias firmaprofesional -keystore %JAVA_HOME%\jre\lib\security\cacerts -storepass changeit
         b) Linux: 
keytool -importcert -noprompt -file firmacloud-com.pem -alias firmaprofesional -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit

4.- Configuració Plugin

Veure Manual d'Usuari de PortaFIB, aparatat "Plugin de Firma Web de Via Firma (Fortress)"