@echo off
rem -------------------------------------------------------------------------
rem XDS Init Tool for Windows
rem -------------------------------------------------------------------------

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

pushd %DIRNAME%..
set "RESOLVED_JBOSS_HOME=%CD%"
popd

if "x%JBOSS_HOME%" == "x" (
  set "JBOSS_HOME=%RESOLVED_JBOSS_HOME%" 
)

pushd "%JBOSS_HOME%"
set "SANITIZED_JBOSS_HOME=%CD%"
popd

if /i "%RESOLVED_JBOSS_HOME%" NEQ "%SANITIZED_JBOSS_HOME%" (
    echo WARNING JBOSS_HOME may be pointing to a different installation - unpredictable results may occur.
    echo resolved:#%RESOLVED_JBOSS_HOME%# vs. #%SANITIZED_JBOSS_HOME%#
)

set DIRNAME=

if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=xdsinit.bat"
)

rem Setup JBoss specific properties
set JAVA_OPTS=-Dprogram.name=%PROGNAME% %JAVA_OPTS%

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set "JAVA=%JAVA_HOME%\bin\java"
)

rem Find run.jar, or we can't continue
if exist "%JBOSS_HOME%\jboss-modules.jar" (
    set "RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%JBOSS_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

"%JAVA%" %JAVA_OPTS% -Dlog4j.debug ^
    -jar "%JBOSS_HOME%\jboss-modules.jar" ^
    -mp "%JBOSS_HOME%\modules" ^
     org.dcm4chee.xds2.tool.init ^
     %*

:END
if "x%NOPAUSE%" == "x" pause
