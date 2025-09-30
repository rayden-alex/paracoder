@echo off
chcp 65001 > nul

java -XX:AOTMode=create -XX:AOTConfiguration=paracoder.aotconf -XX:AOTCache=paracoder.aot --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar d:\java\prj\paracoder\build\libs\ParaCoder.jar

echo ERRORLEVEL:%ERRORLEVEL%

rem popd

pause
