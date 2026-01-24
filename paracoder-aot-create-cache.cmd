@echo off
chcp 65001 > nul

rem https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html
rem To use the AOT cache feature, you should first perform a training run on your application in extracted form
echo Removing previous 'extracted' folder ...
RMDIR /S /Q d:\java\prj\paracoder\build\libs\extracted

echo Extracting application from the fat JAR ...
java -Djarmode=tools -jar d:\java\prj\paracoder\build\libs\ParaCoder.jar extract --destination d:\java\prj\paracoder\build\libs\extracted

pause

echo Building new AOT cache ...
java -Dspring.aot.enabled=true -Dspring.context.exit=onRefresh -XX:AOTCacheOutput=paracoder.aot -XX:+UseCompactObjectHeaders -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar d:\java\prj\paracoder\build\libs\extracted\ParaCoder.jar

echo.
echo You have to use the cache file with the extracted form of the application, otherwise it has no effect!
echo.

echo ERRORLEVEL:%ERRORLEVEL%

pause
