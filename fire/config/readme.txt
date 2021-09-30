

CREAR UN CERTIFICAT PUBLIC/PRIVAT
=================================

keytool -genkey -alias fire -keyalg RSA -validity 5475 -dname "CN=FireTest" -keystore fire.p12 -storepass firepass -storetype pkcs12


EXTREURE PEM
============

keytool -exportcert -alias fire -keystore fire.p12 -storepass firepass -storetype pkcs12 -rfc -file fire.pem