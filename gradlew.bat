@echo off
setlocal
set "APP_HOME=%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%APP_HOME%scripts\prepare-gradle.ps1"
if errorlevel 1 exit /b 1
java -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
