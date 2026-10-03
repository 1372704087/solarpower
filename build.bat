@echo off
rem 1.12.2 build script: portable Temurin JDK 8 (ASCII-only comment, codepage safe)
set "JAVA_HOME=C:\Users\Administrator\.jdks\temurin8\jdk8u504-b01"
cd /d "%~dp0"
"C:\Users\Administrator\.gradle-dist\gradle-4.9\bin\gradle.bat" build %*
