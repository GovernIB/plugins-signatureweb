@echo off

echo optional parameters -Dcaib -Psqlgen

cmd /C mvn clean install -DskipTests %* 

if %errorlevel% EQU 0 (

	@echo off
	IF DEFINED SIGNATUREWEBTESTER_DEPLOY_DIR (
      
	  echo --------- COPIANT EAR  ---------

	  xcopy /Y .\target\signaturewebtester.war %SIGNATUREWEBTESTER_DEPLOY_DIR%

	) ELSE (
	  echo  =================================================================
	  echo    Definex la variable d'entorn SIGNATUREWEBTESTER_DEPLOY_DIR apuntant al
	  echo    directori de deploy del JBOSS  i automaticament s'hi copiara
	  echo    l'ear generat.
	  echo  =================================================================
	) 

)

