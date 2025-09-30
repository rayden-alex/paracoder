@echo off
chcp 65001 > nul

java -XX:AOTMode=record -XX:AOTConfiguration=paracoder.aotconf --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar d:\java\prj\paracoder\build\libs\ParaCoder.jar --recurse -d --thread-count=5 %*

echo ERRORLEVEL:%ERRORLEVEL%

rem popd

pause
