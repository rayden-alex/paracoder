@echo off
chcp 65001 > nul

java -Dspring.aot.enabled=true -Dspring.context.exit=onRefresh -XX:AOTCacheOutput=paracoder.aot -XX:+UseCompactObjectHeaders -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar d:\java\prj\paracoder\build\libs\ParaCoder.jar

echo ERRORLEVEL:%ERRORLEVEL%

pause
