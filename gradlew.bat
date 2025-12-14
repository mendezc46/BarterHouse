@echo off
REM Gradle Wrapper para BarterHouse - Windows
REM Este script descarga y ejecuta Gradle 7.6

setlocal enabledelayedexpansion

set GRADLE_VERSION=7.6
set GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\gradle-%GRADLE_VERSION%

if not exist "%GRADLE_HOME%" (
    echo Descargando Gradle %GRADLE_VERSION%...
    if not exist "%USERPROFILE%\.gradle\wrapper" mkdir "%USERPROFILE%\.gradle\wrapper"
    
    REM Descargar usando PowerShell
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip', '%USERPROFILE%\.gradle\wrapper\gradle-%GRADLE_VERSION%-bin.zip')"
    
    REM Extraer
    powershell -Command "Expand-Archive -Path '%USERPROFILE%\.gradle\wrapper\gradle-%GRADLE_VERSION%-bin.zip' -DestinationPath '%USERPROFILE%\.gradle\wrapper' -Force"
    
    REM Limpiar ZIP
    del "%USERPROFILE%\.gradle\wrapper\gradle-%GRADLE_VERSION%-bin.zip"
)

REM Ejecutar Gradle
"%GRADLE_HOME%\bin\gradle.bat" %*

endlocal
