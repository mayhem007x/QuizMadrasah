@echo off
REM Simple Gradle wrapper launcher for Windows - expects gradle-wrapper.jar in gradle\wrapper
set PRG_DIR=%~dp0
set WRAPPER_JAR=%PRG_DIR%gradle\wrapper\gradle-wrapper.jar
if exist "%WRAPPER_JAR%" (
  java -jar "%WRAPPER_JAR%" %*
) else (
  echo.
  echo The Gradle wrapper jar is missing.
  echo Run "gradle wrapper --gradle-version 8.4" in the project root or open this project in Android Studio to generate it.
  echo.
)
