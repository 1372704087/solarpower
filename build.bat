@echo off
rem 1.12.2 版编译脚本：使用免安装的 Temurin JDK 8（C:\Users\Administrator\.jdks\temurin8）
set "JAVA_HOME=C:\Users\Administrator\.jdks\temurin8\jdk8u504-b01"
cd /d "%~dp0"
"C:\Users\Administrator\.gradle-dist\gradle-4.9\bin\gradle.bat" build %*
