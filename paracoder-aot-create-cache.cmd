@echo off
chcp 65001 > nul

rem https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html
rem To use the AOT cache feature, you should first perform a training run on your application in extracted form
echo Removing previous 'extracted' folder ...
RMDIR /S /Q .\build\libs\extracted

echo Extracting application from the fat JAR ...
java -Djarmode=tools -jar .\build\libs\ParaCoder.jar extract --destination .\build\libs\extracted

pause

echo Building new AOT cache ...
java -Dspring.aot.enabled=true -Dspring.context.exit=onRefresh -XX:AOTCacheOutput=.\build\libs\paracoder.aot -XX:+UseCompactObjectHeaders -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar .\build\libs\extracted\ParaCoder.jar

echo.
echo You have to use the cache file with the extracted form of the application, otherwise it has no effect!
echo.

echo ERRORLEVEL:%ERRORLEVEL%

pause
