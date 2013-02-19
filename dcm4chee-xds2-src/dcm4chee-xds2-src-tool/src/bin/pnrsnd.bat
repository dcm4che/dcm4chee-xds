@echo off
rem -------------------------------------------------------------------------
rem pnrsnd  Launcher
rem -------------------------------------------------------------------------

if not "%ECHO%" == ""  echo %ECHO%
if "%OS%" == "Windows_NT"  setlocal

set MAIN_CLASS=org.dcm4chee.xds2.src.tool.pnrsnd.PnRSnd
set MAIN_JAR=dcm4chee-xds2-src-tool-2.0.0.jar

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

rem Read all command line arguments

set ARGS=
:loop
if [%1] == [] goto end
        set ARGS=%ARGS% %1
        shift
        goto loop
:end

if not "%DCM4CHE_HOME%" == "" goto HAVE_DCM4CHE_HOME

set DCM4CHE_HOME=%DIRNAME%..

:HAVE_DCM4CHE_HOME

if not "%JAVA_HOME%" == "" goto HAVE_JAVA_HOME

set JAVA=java

goto SKIP_SET_JAVA_HOME

:HAVE_JAVA_HOME

set JAVA=%JAVA_HOME%\bin\java

:SKIP_SET_JAVA_HOME

set CP=%DCM4CHE_HOME%\conf\
set CP=%CP%;%DCM4CHE_HOME%\lib\%MAIN_JAR%
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4chee-xds2-src-base-2.0.0.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4chee-xds2-common-2.0.0.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4chee-xds2-infoset-2.0.0.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4che-net-audit-3.0.1.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4che-core-3.0.1.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4che-net-3.0.1.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4che-audit-3.0.1.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\slf4j-api-1.6.1.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\slf4j-log4j12-1.6.1.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\log4j-1.2.16.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\commons-cli-1.2.jar

set JAVA_OPTS=%JAVA_OPTS% -Dhttps.protocols=TLSv1
set JAVA_OPTS=%JAVA_OPTS% "-Dhttps.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA"
set JAVA_OPTS=%JAVA_OPTS% -Djavax.net.ssl.trustStore=%DCM4CHE_HOME%\conf\EURO2012\keystore
set JAVA_OPTS=%JAVA_OPTS% -Djavax.net.ssl.trustStorePassword=changeit
set JAVA_OPTS=%JAVA_OPTS% -Djavax.net.ssl.keyStore=%DCM4CHE_HOME%\conf\EURO2012\keystore
set JAVA_OPTS=%JAVA_OPTS% -Djavax.net.ssl.keyStorePassword=changeit

"%JAVA%" -version
"%JAVA%" %JAVA_OPTS% -cp "%CP%" %MAIN_CLASS% %ARGS%
