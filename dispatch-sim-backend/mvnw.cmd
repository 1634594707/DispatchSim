@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "MAVEN_REPO=%SCRIPT_DIR%.m2\repository"
set "BUNDLED_MAVEN=%SCRIPT_DIR%..\.tools\apache-maven-3.9.11\bin\mvn.cmd"

if exist "%BUNDLED_MAVEN%" (
  call "%BUNDLED_MAVEN%" -Dmaven.repo.local="%MAVEN_REPO%" %*
  exit /b %ERRORLEVEL%
)

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  call mvn -Dmaven.repo.local="%MAVEN_REPO%" %*
  exit /b %ERRORLEVEL%
)

echo Maven was not found. Install Maven or restore .tools\apache-maven-3.9.11.
exit /b 1
